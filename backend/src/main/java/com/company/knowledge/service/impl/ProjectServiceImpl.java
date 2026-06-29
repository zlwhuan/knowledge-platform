package com.company.knowledge.service.impl;

import com.company.knowledge.dto.ProjectActivityRequest;
import com.company.knowledge.dto.ProjectActivityResponse;
import com.company.knowledge.dto.ProjectDashboardResponse;
import com.company.knowledge.dto.ProjectProgressRecordRequest;
import com.company.knowledge.dto.ProjectProgressRecordResponse;
import com.company.knowledge.dto.ProjectRequest;
import com.company.knowledge.dto.ProjectResponse;
import com.company.knowledge.entity.Project;
import com.company.knowledge.entity.ProjectActivity;
import com.company.knowledge.entity.ProjectProgressRecord;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.ProjectActivityRepository;
import com.company.knowledge.repository.ProjectProgressRecordRepository;
import com.company.knowledge.repository.ProjectRepository;
import com.company.knowledge.service.ProjectService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectActivityRepository projectActivityRepository;
    private final ProjectProgressRecordRepository projectProgressRecordRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository, ProjectActivityRepository projectActivityRepository, ProjectProgressRecordRepository projectProgressRecordRepository) {
        this.projectRepository = projectRepository;
        this.projectActivityRepository = projectActivityRepository;
        this.projectProgressRecordRepository = projectProgressRecordRepository;
    }

    @Override
    public List<ProjectResponse> list(String keyword, String customerName, String stage, String status, String owner, String riskLevel) {
        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("name"), pattern),
                    cb.like(root.get("customerName"), pattern),
                    cb.like(root.get("description"), pattern)
                ));
            }
            if (StringUtils.hasText(customerName)) {
                predicates.add(cb.like(root.get("customerName"), "%" + customerName.trim() + "%"));
            }
            if (StringUtils.hasText(stage)) {
                predicates.add(cb.equal(root.get("stage"), stage));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(owner)) {
                String oPattern = "%" + owner.trim() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("salesOwner"), oPattern),
                    cb.like(root.get("projectManager"), oPattern),
                    cb.like(root.get("implementationOwner"), oPattern),
                    cb.like(root.get("documentOwner"), oPattern),
                    cb.like(root.get("serviceOwner"), oPattern)
                ));
            }
            if (StringUtils.hasText(riskLevel)) {
                predicates.add(cb.equal(root.get("riskLevel"), riskLevel));
            }
            query.orderBy(cb.desc(root.get("updatedAt")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return projectRepository.findAll(spec).stream().map(this::toResponse).toList();
    }

    @Override
    public ProjectDashboardResponse dashboard() {
        List<Project> projects = projectRepository.findAll();
        LocalDate today = LocalDate.now();
        long totalProjects = projects.size();
        long activeProjects = projects.stream().filter(project -> equalsValue(project.getStatus(), "进行中")).count();
        long highRiskProjects = projects.stream().filter(project -> equalsValue(project.getRiskLevel(), "高")).count();
        long acceptancePendingProjects = projects.stream().filter(project -> equalsValue(project.getStage(), "验收结算") && !equalsValue(project.getStatus(), "已完成")).count();
        long paymentPendingProjects = projects.stream().filter(project -> !equalsValue(project.getPaymentStatus(), "回款完成")).count();
        BigDecimal totalContractAmount = projects.stream()
                .map(project -> project.getContractAmount() == null ? BigDecimal.ZERO : project.getContractAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReceivedAmount = projects.stream()
                .map(project -> project.getReceivedAmount() == null ? BigDecimal.ZERO : project.getReceivedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> stageCounts = new LinkedHashMap<>();
        stageCounts.put("商机立项", countStage(projects, "商机立项"));
        stageCounts.put("合同执行", countStage(projects, "合同执行"));
        stageCounts.put("实施交付", countStage(projects, "实施交付"));
        stageCounts.put("验收结算", countStage(projects, "验收结算"));
        stageCounts.put("售后维保", countStage(projects, "售后维保"));

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("进行中", countStatus(projects, "进行中"));
        statusCounts.put("已完成", countStatus(projects, "已完成"));
        statusCounts.put("暂停", countStatus(projects, "暂停"));

        Map<String, Long> roleTodoCounts = new LinkedHashMap<>();
        roleTodoCounts.put("老板", highRiskProjects + paymentPendingProjects);
        roleTodoCounts.put("销售", projects.stream().filter(project -> equalsValue(project.getStage(), "合同执行") && !equalsValue(project.getStatus(), "已完成")).count());
        roleTodoCounts.put("实施", projects.stream().filter(project -> equalsValue(project.getStage(), "实施交付")).count());
        roleTodoCounts.put("运维", projects.stream().filter(project -> equalsValue(project.getStage(), "售后维保") && !equalsValue(project.getStatus(), "已完成")).count());
        roleTodoCounts.put("财务", paymentPendingProjects);

        return new ProjectDashboardResponse(
                totalProjects,
                activeProjects,
                highRiskProjects,
                acceptancePendingProjects,
                paymentPendingProjects,
                totalContractAmount,
                totalReceivedAmount,
                stageCounts,
                statusCounts,
                roleTodoCounts
        );
    }

    @Override
    public ProjectResponse get(Long id) {
        return toResponse(findProject(id));
    }

    @Override
    public ProjectResponse create(ProjectRequest request, String operatorName) {
        Project project = new Project();
        apply(project, request, operatorName);
        if (!StringUtils.hasText(project.getCreatedBy())) {
            project.setCreatedBy(StringUtils.hasText(operatorName) ? operatorName : "系统");
        }
        return toResponse(projectRepository.save(project));
    }

    @Override
    public ProjectResponse update(Long id, ProjectRequest request, String operatorName) {
        Project project = findProject(id);
        apply(project, request, operatorName);
        return toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project project = findProject(id);
        projectActivityRepository.deleteAllByProjectId(id);
        projectProgressRecordRepository.deleteAllByProjectId(id);
        projectRepository.delete(project);
    }

    @Override
    public ProjectProgressRecordResponse addProgressRecord(Long projectId, ProjectProgressRecordRequest request, String operatorName) {
        Project project = findProject(projectId);
        ProjectProgressRecord record = new ProjectProgressRecord();
        record.setProject(project);
        applyProgressRecord(record, request, operatorName, false);
        ProjectProgressRecord saved = projectProgressRecordRepository.save(record);
        refreshProjectSnapshot(project, operatorName);
        return toProgressRecordResponse(saved);
    }

    @Override
    public ProjectProgressRecordResponse updateProgressRecord(Long recordId, ProjectProgressRecordRequest request, String operatorName) {
        ProjectProgressRecord record = projectProgressRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("项目进度不存在"));
        applyProgressRecord(record, request, operatorName, true);
        ProjectProgressRecord saved = projectProgressRecordRepository.save(record);
        refreshProjectSnapshot(record.getProject(), operatorName);
        return toProgressRecordResponse(saved);
    }

    @Override
    public void deleteProgressRecord(Long recordId) {
        ProjectProgressRecord record = projectProgressRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("项目进度不存在"));
        Project project = record.getProject();
        projectProgressRecordRepository.delete(record);
        refreshProjectSnapshot(project, null);
    }

    @Override
    public ProjectActivityResponse addActivity(Long projectId, ProjectActivityRequest request, String operatorName) {
        Project project = findProject(projectId);
        ProjectActivity activity = new ProjectActivity();
        activity.setProject(project);
        applyActivity(activity, request, operatorName, false);
        ProjectActivity saved = projectActivityRepository.save(activity);
        touchProject(project, operatorName);
        return toActivityResponse(saved);
    }

    @Override
    public ProjectActivityResponse updateActivity(Long activityId, ProjectActivityRequest request, String operatorName) {
        ProjectActivity activity = projectActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("项目记录不存在"));
        applyActivity(activity, request, operatorName, true);
        ProjectActivity saved = projectActivityRepository.save(activity);
        touchProject(activity.getProject(), operatorName);
        return toActivityResponse(saved);
    }

    @Override
    public void deleteActivity(Long activityId) {
        ProjectActivity activity = projectActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("项目记录不存在"));
        Project project = activity.getProject();
        projectActivityRepository.delete(activity);
        touchProject(project, null);
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("项目不存在"));
    }

    private void applyProgressRecord(ProjectProgressRecord record, ProjectProgressRecordRequest request, String operatorName, boolean keepCreator) {
        record.setStage(request.getStage());
        record.setStatus(request.getStatus());
        record.setProgress(request.getProgress() == null ? 0 : request.getProgress());
        record.setRiskLevel(request.getRiskLevel());
        record.setSummary(request.getSummary());
        record.setNextAction(request.getNextAction());
        record.setNextActionDueDate(request.getNextActionDueDate());
        record.setOwnerName(request.getOwnerName());
        record.setRecordTime(request.getRecordTime() != null ? request.getRecordTime() : LocalDateTime.now());
        if (!keepCreator || !StringUtils.hasText(record.getCreatedBy())) {
            record.setCreatedBy(StringUtils.hasText(operatorName) ? operatorName : "系统");
        }
    }

    private void applyActivity(ProjectActivity activity, ProjectActivityRequest request, String operatorName, boolean keepCreator) {
        activity.setRecordType(request.getRecordType());
        activity.setContent(request.getContent());
        activity.setOwnerName(request.getOwnerName());
        activity.setRecordTime(request.getRecordTime() != null ? request.getRecordTime() : LocalDateTime.now());
        if (!keepCreator || !StringUtils.hasText(activity.getCreatedBy())) {
            activity.setCreatedBy(StringUtils.hasText(operatorName) ? operatorName : "系统");
        }
    }

    private void touchProject(Project project, String operatorName) {
        project.setUpdatedBy(StringUtils.hasText(operatorName) ? operatorName : project.getUpdatedBy());
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    private void refreshProjectSnapshot(Project project, String operatorName) {
        ProjectProgressRecord latestRecord = projectProgressRecordRepository.findFirstByProjectIdOrderByRecordTimeDescIdDesc(project.getId());
        if (latestRecord != null) {
            project.setStage(latestRecord.getStage());
            project.setStatus(latestRecord.getStatus());
            project.setProgress(latestRecord.getProgress());
            project.setRiskLevel(latestRecord.getRiskLevel());
        }
        touchProject(project, operatorName);
    }

    private void apply(Project project, ProjectRequest request, String operatorName) {
        if (request.getContractAmount() != null && request.getReceivedAmount() != null
                && request.getReceivedAmount().compareTo(request.getContractAmount()) > 0) {
            throw new IllegalArgumentException("回款金额不能大于合同金额");
        }
        if (request.getStartDate() != null && request.getPlannedEndDate() != null
                && request.getStartDate().isAfter(request.getPlannedEndDate())) {
            throw new IllegalArgumentException("计划结束日期不能早于开始日期");
        }
        project.setName(request.getName());
        project.setCustomerName(request.getCustomerName());
        project.setStage(request.getStage());
        project.setStatus(request.getStatus());
        project.setProgress(request.getProgress() == null ? 0 : request.getProgress());
        project.setContractAmount(request.getContractAmount() == null ? java.math.BigDecimal.ZERO : request.getContractAmount());
        project.setReceivedAmount(request.getReceivedAmount() == null ? java.math.BigDecimal.ZERO : request.getReceivedAmount());
        project.setStartDate(request.getStartDate());
        project.setPlannedEndDate(request.getPlannedEndDate());
        project.setAcceptanceDate(request.getAcceptanceDate());
        project.setWarrantyUntil(request.getWarrantyUntil());
        project.setSalesOwner(request.getSalesOwner());
        project.setProjectManager(request.getProjectManager());
        project.setImplementationOwner(request.getImplementationOwner());
        project.setDocumentOwner(request.getDocumentOwner());
        project.setServiceOwner(request.getServiceOwner());
        project.setContractStatus(request.getContractStatus());
        project.setPaymentStatus(request.getPaymentStatus());
        project.setAcceptanceStatus(request.getAcceptanceStatus());
        project.setServiceStatus(request.getServiceStatus());
        project.setRiskLevel(request.getRiskLevel());
        project.setProjectContactIds(request.getProjectContactIds());
        project.setProjectContactLinks(request.getProjectContactLinks());
        project.setRelatedContactNotes(request.getRelatedContactNotes());
        project.setDescription(request.getDescription());
        project.setUpdatedBy(StringUtils.hasText(operatorName) ? operatorName : project.getCreatedBy());
        project.setUpdatedAt(LocalDateTime.now());
    }

    private ProjectResponse toResponse(Project project) {
        List<ProjectActivityResponse> activities = projectActivityRepository.findAllByProjectIdOrderByRecordTimeDesc(project.getId())
                .stream()
                .map(this::toActivityResponse)
                .toList();
        List<ProjectProgressRecordResponse> progressRecords = projectProgressRecordRepository.findAllByProjectIdOrderByRecordTimeDesc(project.getId())
                .stream()
                .map(this::toProgressRecordResponse)
                .toList();
        String latestProgressSummary = progressRecords.isEmpty() ? null : progressRecords.get(0).summary();
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getCustomerName(),
                project.getStage(),
                project.getStatus(),
                project.getProgress(),
                project.getContractAmount(),
                project.getReceivedAmount(),
                project.getStartDate(),
                project.getPlannedEndDate(),
                project.getAcceptanceDate(),
                project.getWarrantyUntil(),
                project.getSalesOwner(),
                project.getProjectManager(),
                project.getImplementationOwner(),
                project.getDocumentOwner(),
                project.getServiceOwner(),
                project.getRiskLevel(),
                project.getContractStatus(),
                project.getPaymentStatus(),
                project.getAcceptanceStatus(),
                project.getServiceStatus(),
                project.getProjectContactIds(),
                project.getProjectContactLinks(),
                project.getRelatedContactNotes(),
                latestProgressSummary,
                project.getDescription(),
                project.getCreatedBy(),
                project.getUpdatedBy(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                activities,
                progressRecords
        );
    }

    private ProjectActivityResponse toActivityResponse(ProjectActivity activity) {
        return new ProjectActivityResponse(
                activity.getId(),
                activity.getRecordType(),
                activity.getContent(),
                activity.getOwnerName(),
                activity.getRecordTime(),
                activity.getCreatedBy(),
                activity.getCreatedAt()
        );
    }

    private ProjectProgressRecordResponse toProgressRecordResponse(ProjectProgressRecord record) {
        return new ProjectProgressRecordResponse(
                record.getId(),
                record.getStage(),
                record.getStatus(),
                record.getProgress(),
                record.getRiskLevel(),
                record.getSummary(),
                record.getNextAction(),
                record.getNextActionDueDate(),
                record.getOwnerName(),
                record.getRecordTime(),
                record.getCreatedBy(),
                record.getCreatedAt()
        );
    }

    private boolean matches(String value, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private boolean equalsValue(String value, String expected) {
        return !StringUtils.hasText(expected) || expected.trim().equals(value);
    }

    private long countStage(List<Project> projects, String stage) {
        return projects.stream().filter(project -> equalsValue(project.getStage(), stage)).count();
    }

    private long countStatus(List<Project> projects, String status) {
        return projects.stream().filter(project -> equalsValue(project.getStatus(), status)).count();
    }

    private boolean isPending(LocalDate date, LocalDate today) {
        return date != null && !date.isBefore(today);
    }

    private boolean matchesAnyOwner(Project project, String owner) {
        if (!StringUtils.hasText(owner)) {
            return true;
        }
        return matches(project.getSalesOwner(), owner)
                || matches(project.getProjectManager(), owner)
                || matches(project.getImplementationOwner(), owner)
                || matches(project.getDocumentOwner(), owner)
                || matches(project.getServiceOwner(), owner);
    }
}
