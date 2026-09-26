package com.company.knowledge.dto;

import java.util.List;
import java.util.Map;

/**
 * 技能对话请求
 */
public record SkillChatRequest(
        String skillId,
        String message,
        List<Map<String, String>> history
) {
}
