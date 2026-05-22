package com.company.knowledge.dto;

import java.time.LocalDateTime;

public record CustomerContactResponse(
        Long id,
        String name,
        String position,
        String department,
        String gender,
        String mobile,
        String officePhone,
        String email,
        String wechat,
        String qq,
        String decisionLevel,
        Boolean primaryContact,
        String notes,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
