package com.company.knowledge.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ProjectDashboardResponse(
        long totalProjects,
        long activeProjects,
        long highRiskProjects,
        long acceptancePendingProjects,
        long paymentPendingProjects,
        BigDecimal totalContractAmount,
        BigDecimal totalReceivedAmount,
        Map<String, Long> stageCounts,
        Map<String, Long> statusCounts,
        Map<String, Long> roleTodoCounts
) {
}
