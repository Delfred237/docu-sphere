package com.docusphere.document.dto;

import com.docusphere.document.domain.Document;
import com.docusphere.document.domain.DocumentStatus;

public record DocumentResponse(
        String publicId,
        String name,
        String mimeType,
        Long size,
        DocumentStatus status,
        String folderPublicId
) {
    public static DocumentResponse fromEntity(Document doc) {
        return new DocumentResponse(
                doc.getPublicId(),
                doc.getName(),
                doc.getMimeType(),
                doc.getSize(),
                doc.getStatus(),
                doc.getFolder() != null ? doc.getFolder().getPublicId() : null
        );
    }
}
