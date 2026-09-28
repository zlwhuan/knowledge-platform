package com.company.knowledge.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record SkillChatSessionResponse(
        Long id,
        String skillId,
        String title,
        int messageCount,
        LocalDateTime updatedAt
) {
}
