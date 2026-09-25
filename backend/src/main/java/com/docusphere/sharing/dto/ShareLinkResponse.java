package com.docusphere.sharing.dto;

import com.docusphere.sharing.domain.ShareLink;

import java.time.Instant;

public record ShareLinkResponse(
        String publicId,
        String token,
        String documentPublicId,
        String documentName,
        String documentMimeType,
        Long documentSize,
        boolean allowDownload,
        int downloadCount,
        Instant expiresAt,
        Instant createdAt
) {
    public static ShareLinkResponse fromEntity(ShareLink link) {
        return new ShareLinkResponse(
                link.getPublicId(),
                link.getToken(),
                link.getDocument().getPublicId(),
                link.getDocument().getName(),
                link.getDocument().getMimeType(),
                link.getDocument().getSize(),
                link.isAllowDownload(),
                link.getDownloadCount(),
                link.getExpiresAt(),
                link.getCreatedAt()
        );
    }
}