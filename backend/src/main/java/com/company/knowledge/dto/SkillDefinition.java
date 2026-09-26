package com.company.knowledge.dto;

import java.util.List;

/**
 * 技能定义（含管理字段）
 */
public record SkillDefinition(
        Long dbId,
        String id,
        String name,
        String description,
        String icon,
        String systemPrompt,
        List<String> tools,
        Integer sortOrder,
        Boolean enabled
) {
    public SkillDefinition {
        if (tools == null) tools = List.of();
        if (sortOrder == null) sortOrder = 0;
        if (enabled == null) enabled = true;
    }
}

