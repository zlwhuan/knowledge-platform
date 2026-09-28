package com.company.knowledge.dto;

import java.util.List;
import java.util.Map;

/**
 * 技能对话请求
 */
public record SkillChatRequest(
        String skillId,
        List<String> skillIds,
        String message,
        List<Map<String, String>> history,
        Long sessionId,
        String model
) {
}
