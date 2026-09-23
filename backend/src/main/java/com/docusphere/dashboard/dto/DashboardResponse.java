package com.docusphere.dashboard.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        Overview overview,
        List<DailyActivity> activity,
        Map<String, Long> byStatus,
        Map<String, Long> byType,
        List<RecentDocument> recentDocuments
) {
    public record Overview(
            long totalDocuments,
            long totalFolders,
            long pendingReviews,
            long sharedLinks,
            long storageUsedBytes
    ) {}

    public record DailyActivity(
            LocalDate date,
            long uploads,
            long approvals
    ) {}

    public record RecentDocument(
            String publicId,
            String name,
            String mimeType,
            Long size,
            String status,
            String uploadedAt,
            String ownerName
    ) {}
}