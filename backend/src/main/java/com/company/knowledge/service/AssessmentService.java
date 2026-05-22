package com.company.knowledge.service;

import com.company.knowledge.dto.AssessmentRecordRequest;
import com.company.knowledge.dto.AssessmentRecordResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AssessmentService {

    AssessmentRecordResponse create(AssessmentRecordRequest request, String operatorName);

    AssessmentRecordResponse update(Long id, AssessmentRecordRequest request, String operatorName);

    AssessmentRecordResponse get(Long id);

    void delete(Long id);

    Page<AssessmentRecordResponse> list(String keyword, String assessmentType, String grade, int page, int size);

    List<AssessmentRecordResponse> listAll();

    List<AssessmentRecordResponse> listByTrainingRecordId(Long trainingRecordId);
}
