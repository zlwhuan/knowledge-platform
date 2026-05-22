package com.company.knowledge.service.impl;

import com.company.knowledge.dto.AssessmentRecordRequest;
import com.company.knowledge.dto.AssessmentRecordResponse;
import com.company.knowledge.entity.AssessmentRecord;
import com.company.knowledge.entity.TrainingRecord;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.AssessmentRecordRepository;
import com.company.knowledge.repository.TrainingRecordRepository;
import com.company.knowledge.service.AssessmentService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRecordRepository assessmentRecordRepository;
    private final TrainingRecordRepository trainingRecordRepository;

    public AssessmentServiceImpl(AssessmentRecordRepository assessmentRecordRepository,
                                 TrainingRecordRepository trainingRecordRepository) {
        this.assessmentRecordRepository = assessmentRecordRepository;
        this.trainingRecordRepository = trainingRecordRepository;
    }

    @Override
    @Transactional
    public AssessmentRecordResponse create(AssessmentRecordRequest request, String operatorName) {
        AssessmentRecord record = new AssessmentRecord();
        record.setTitle(request.getTitle());
        record.setAssessmentType(request.getAssessmentType());
        record.setAssessmentDate(request.getAssessmentDate());
        record.setAssessorIds(request.getAssessorIds());
        record.setGrade(request.getGrade());
        record.setEvaluation(request.getEvaluation());
        record.setTrainingRecordId(request.getTrainingRecordId());
        record.setCreatedBy(operatorName);
        record.setUpdatedBy(operatorName);

        record = assessmentRecordRepository.save(record);
        return toResponse(record);
    }

    @Override
    @Transactional
    public AssessmentRecordResponse update(Long id, AssessmentRecordRequest request, String operatorName) {
        AssessmentRecord record = assessmentRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考核记录不存在"));

        record.setTitle(request.getTitle());
        record.setAssessmentType(request.getAssessmentType());
        record.setAssessmentDate(request.getAssessmentDate());
        record.setAssessorIds(request.getAssessorIds());
        record.setGrade(request.getGrade());
        record.setEvaluation(request.getEvaluation());
        record.setTrainingRecordId(request.getTrainingRecordId());
        record.setUpdatedBy(operatorName);

        record = assessmentRecordRepository.save(record);
        return toResponse(record);
    }

    @Override
    public AssessmentRecordResponse get(Long id) {
        AssessmentRecord record = assessmentRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("考核记录不存在"));
        return toResponse(record);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!assessmentRecordRepository.existsById(id)) {
            throw new ResourceNotFoundException("考核记录不存在");
        }
        assessmentRecordRepository.deleteById(id);
    }

    @Override
    public Page<AssessmentRecordResponse> list(String keyword, String assessmentType, String grade, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "assessmentDate"));

        Specification<AssessmentRecord> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + keyword.toLowerCase() + "%"
                ));
            }

            if (assessmentType != null && !assessmentType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("assessmentType"), assessmentType));
            }

            if (grade != null && !grade.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("grade"), grade));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return assessmentRecordRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    public List<AssessmentRecordResponse> listAll() {
        return assessmentRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "assessmentDate"))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AssessmentRecordResponse> listByTrainingRecordId(Long trainingRecordId) {
        return assessmentRecordRepository.findByTrainingRecordId(trainingRecordId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AssessmentRecordResponse toResponse(AssessmentRecord record) {
        String trainingRecordTitle = null;
        if (record.getTrainingRecordId() != null) {
            trainingRecordTitle = trainingRecordRepository.findById(record.getTrainingRecordId())
                    .map(TrainingRecord::getTitle)
                    .orElse(null);
        }

        return new AssessmentRecordResponse(
                record.getId(),
                record.getTitle(),
                record.getAssessmentType(),
                record.getAssessmentDate(),
                record.getAssessorIds(),
                record.getGrade(),
                record.getEvaluation(),
                record.getTrainingRecordId(),
                trainingRecordTitle,
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getUpdatedBy(),
                record.getUpdatedAt()
        );
    }
}
