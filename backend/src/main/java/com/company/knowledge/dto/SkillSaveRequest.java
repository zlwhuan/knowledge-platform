package com.company.knowledge.dto;

import java.util.List;

/**
 * 技能保存请求（新增/编辑）
 */
public record SkillSaveRequest(
        String skillKey,
        String name,
        String description,
        String icon,
        String systemPrompt,
        List<String> tools,
        List<String> scopeCategoryIds,
        List<String> scopeItemIds,
        Integer sortOrder,
        Boolean enabled,
        /** 个人技能归属用户名；空 = 公共技能 */
        String ownerUsername
) {
}
