package com.company.knowledge.entity;

public enum RoleType {
    ADMIN,
    SALES,
    PRESALES,
    DELIVERY_OPS,
    FINANCE,
    // 兼容历史数据（旧角色），建议后续迁移后删除
    USER,
    REVIEWER
}
