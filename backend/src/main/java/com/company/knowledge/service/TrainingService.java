package com.company.knowledge.service;

import com.company.knowledge.dto.TrainingRecordRequest;
import com.company.knowledge.dto.TrainingRecordResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TrainingService {

    TrainingRecordResponse create(TrainingRecordRequest request, String operatorName);

    TrainingRecordResponse update(Long id, TrainingRecordRequest request, String operatorName);

    TrainingRecordResponse get(Long id);

    void delete(Long id);

    Page<TrainingRecordResponse> list(String keyword, String trainingType, int page, int size);

    List<TrainingRecordResponse> listAll();
}
