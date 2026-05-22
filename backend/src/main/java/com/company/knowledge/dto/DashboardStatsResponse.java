package com.company.knowledge.dto;

public record DashboardStatsResponse(
        long categoryCount,
        long itemCount,
        long attachmentCount,
        long markdownItemCount,
        long missingSummaryCount,
        long missingAttachmentCount,
        long recentUpdatedCount
) {
}
