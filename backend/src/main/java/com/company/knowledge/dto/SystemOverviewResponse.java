package com.company.knowledge.dto;

public record SystemOverviewResponse(
        String appName,
        String officeCommand,
        boolean onlyOfficeEnabled,
        String serverPort,
        long itemCount,
        long categoryCount,
        long userCount
) {
}
