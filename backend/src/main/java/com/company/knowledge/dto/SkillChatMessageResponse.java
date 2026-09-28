package com.company.knowledge.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SkillChatMessageResponse(
        Long id,
        String role,
        String content,
        List<Map<String, Object>> sources,
        List<Map<String, Object>> traces,
        LocalDateTime createdAt
) {
}
