package com.company.knowledge.dto;

import java.util.List;
import java.util.Map;

/**
 * 技能对话响应：回答 + 检索出处
 */
public record SkillChatResponse(
        String answer,
        List<Map<String, Object>> sources,
        String skillId,
        int toolRounds,
        String error
) {
    public static SkillChatResponse ok(String answer, List<Map<String, Object>> sources, String skillId, int toolRounds) {
        return new SkillChatResponse(answer, sources, skillId, toolRounds, null);
    }

    public static SkillChatResponse fail(String skillId, String error) {
        return new SkillChatResponse("", List.of(), skillId, 0, error);
    }
}
