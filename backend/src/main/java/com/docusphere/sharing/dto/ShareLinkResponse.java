package com.docusphere.sharing.dto;

import com.docusphere.sharing.domain.ShareLink;

import java.time.Instant;

public record ShareLinkResponse(
        String publicId,
        String token,
        String documentPublicId,
        String documentName,
        String documentMimeType,
        Instant expiresAt,
        boolean allowDownload,
        int downloadCount,
        Instant createdAt
) {
    public static ShareLinkResponse fromEntity(ShareLink shareLink) {
        return new ShareLinkResponse(
                shareLink.getPublicId(),
                shareLink.getToken(),
                shareLink.getDocument().getPublicId(),
                shareLink.getDocument().getName(),
                shareLink.getDocument().getMimeType(),
                shareLink.getExpiresAt(),
                shareLink.isAllowDownload(),
                shareLink.getDownloadCount(),
                shareLink.getCreatedAt()
        );
    }
}