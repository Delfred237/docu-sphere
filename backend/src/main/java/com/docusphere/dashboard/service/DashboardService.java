package com.docusphere.dashboard.service;

import com.docusphere.auth.domain.User;
import com.docusphere.dashboard.dto.DashboardResponse;
import com.docusphere.document.domain.Document;
import com.docusphere.document.domain.DocumentStatus;
import com.docusphere.document.repository.DocumentRepository;
import com.docusphere.folder.repository.FolderRepository;
import com.docusphere.sharing.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final ShareLinkRepository shareLinkRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(User user) {
        List<Document> userDocuments = documentRepository.findAllByOwnerIdAndDeletedFalse(user.getId());

        // Overview
        long totalDocuments = userDocuments.size();
        long totalFolders = folderRepository.countByOwnerIdAndDeletedFalse(user.getId());
        long pendingReviews = userDocuments.stream()
                .filter(d -> d.getStatus() == DocumentStatus.PENDING_REVIEW)
                .count();
        long sharedLinks = shareLinkRepository.countByCreatedByUserIdAndDeletedFalse(user.getId());
        long storageUsed = userDocuments.stream()
                .mapToLong(Document::getSize)
                .sum();

        DashboardResponse.Overview overview = new DashboardResponse.Overview(
                totalDocuments, totalFolders, pendingReviews, sharedLinks, storageUsed
        );

        // Activity sur les 14 derniers jours
        List<DashboardResponse.DailyActivity> activity = buildDailyActivity(userDocuments);

        // Répartition par statut
        Map<String, Long> byStatus = userDocuments.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getStatus().name(),
                        Collectors.counting()
                ));

        // Répartition par type MIME
        Map<String, Long> byType = userDocuments.stream()
                .collect(Collectors.groupingBy(
                        d -> extractFileType(d.getMimeType()),
                        Collectors.counting()
                ));

        // Documents récents (5 derniers)
        List<DashboardResponse.RecentDocument> recentDocuments = userDocuments.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(d -> new DashboardResponse.RecentDocument(
                        d.getPublicId(),
                        d.getName(),
                        d.getMimeType(),
                        d.getSize(),
                        d.getStatus().name(),
                        d.getCreatedAt().toString(),
                        d.getOwner().getFirstName() + " " + d.getOwner().getLastName()
                ))
                .toList();

        return new DashboardResponse(overview, activity, byStatus, byType, recentDocuments);
    }

    private List<DashboardResponse.DailyActivity> buildDailyActivity(List<Document> documents) {
        LocalDate today = LocalDate.now();
        List<DashboardResponse.DailyActivity> activity = new ArrayList<>();

        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long uploads = documents.stream()
                    .filter(d -> {
                        LocalDate docDate = Instant.ofEpochMilli(d.getCreatedAt().toEpochMilli())
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        return docDate.equals(date);
                    })
                    .count();

            activity.add(new DashboardResponse.DailyActivity(date, uploads, 0));
        }

        return activity;
    }

    private String extractFileType(String mimeType) {
        if (mimeType == null) return "Other";
        if (mimeType.contains("pdf")) return "PDF";
        if (mimeType.contains("image")) return "Image";
        if (mimeType.contains("word")) return "Word";
        if (mimeType.contains("excel") || mimeType.contains("sheet")) return "Excel";
        if (mimeType.contains("text")) return "Text";
        return "Other";
    }
}