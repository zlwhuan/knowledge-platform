package com.company.knowledge.service.impl;

import com.company.knowledge.dto.TrainingRecordRequest;
import com.company.knowledge.dto.TrainingRecordResponse;
import com.company.knowledge.entity.TrainingRecord;
import com.company.knowledge.exception.ResourceNotFoundException;
import com.company.knowledge.repository.TrainingRecordRepository;
import com.company.knowledge.service.TrainingService;
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
public class TrainingServiceImpl implements TrainingService {

    private final TrainingRecordRepository trainingRecordRepository;

    public TrainingServiceImpl(TrainingRecordRepository trainingRecordRepository) {
        this.trainingRecordRepository = trainingRecordRepository;
    }

    @Override
    @Transactional
    public TrainingRecordResponse create(TrainingRecordRequest request, String operatorName) {
        TrainingRecord record = new TrainingRecord();
        record.setTitle(request.getTitle());
        record.setContent(request.getContent());
        record.setTrainingDate(request.getTrainingDate());
        record.setTrainingType(request.getTrainingType());
        record.setTrainer(request.getTrainer());
        record.setParticipantIds(request.getParticipantIds());
        record.setAttachmentIds(request.getAttachmentIds());
        record.setRemarks(request.getRemarks());
        record.setCreatedBy(operatorName);
        record.setUpdatedBy(operatorName);

        record = trainingRecordRepository.save(record);
        return toResponse(record);
    }

    @Override
    @Transactional
    public TrainingRecordResponse update(Long id, TrainingRecordRequest request, String operatorName) {
        TrainingRecord record = trainingRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("培训记录不存在"));

        record.setTitle(request.getTitle());
        record.setContent(request.getContent());
        record.setTrainingDate(request.getTrainingDate());
        record.setTrainingType(request.getTrainingType());
        record.setTrainer(request.getTrainer());
        record.setParticipantIds(request.getParticipantIds());
        record.setAttachmentIds(request.getAttachmentIds());
        record.setRemarks(request.getRemarks());
        record.setUpdatedBy(operatorName);

        record = trainingRecordRepository.save(record);
        return toResponse(record);
    }

    @Override
    public TrainingRecordResponse get(Long id) {
        TrainingRecord record = trainingRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("培训记录不存在"));
        return toResponse(record);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!trainingRecordRepository.existsById(id)) {
            throw new ResourceNotFoundException("培训记录不存在");
        }
        trainingRecordRepository.deleteById(id);
    }

    @Override
    public Page<TrainingRecordResponse> list(String keyword, String trainingType, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "trainingDate"));

        Specification<TrainingRecord> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + keyword.toLowerCase() + "%"
                ));
            }

            if (trainingType != null && !trainingType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("trainingType"), trainingType));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return trainingRecordRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    public List<TrainingRecordResponse> listAll() {
        return trainingRecordRepository.findAll(Sort.by(Sort.Direction.DESC, "trainingDate"))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TrainingRecordResponse toResponse(TrainingRecord record) {
        return new TrainingRecordResponse(
                record.getId(),
                record.getTitle(),
                record.getContent(),
                record.getTrainingDate(),
                record.getTrainingType(),
                record.getTrainer(),
                record.getParticipantIds(),
                record.getAttachmentIds(),
                record.getRemarks(),
                record.getCreatedBy(),
                record.getCreatedAt(),
                record.getUpdatedBy(),
                record.getUpdatedAt()
        );
    }
}
