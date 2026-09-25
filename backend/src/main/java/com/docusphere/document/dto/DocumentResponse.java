package com.docusphere.document.dto;

import com.docusphere.document.domain.Document;
import com.docusphere.document.domain.DocumentStatus;

public record DocumentResponse(
        String publicId,
        String name,
        String originalFilename,
        String mimeType,
        Long size,
        DocumentStatus status,
        boolean hasFile,
        String folderPublicId,
        String folderName,
        String createdAt) {
    public static DocumentResponse fromEntity(Document doc) {
        return new DocumentResponse(
                doc.getPublicId(),
                doc.getName(),
                doc.getOriginalFilename(),
                doc.getMimeType(),
                doc.getSize(),
                doc.getStatus(),
                doc.getStoredFilename() != null && doc.getSize() > 0,
                doc.getFolder() != null ? doc.getFolder().getPublicId() : null,
                doc.getFolder() != null ? doc.getFolder().getName() : null,
                doc.getCreatedAt().toString()
        );
    }
}
