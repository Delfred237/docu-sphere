package com.docusphere.folder.dto;

import com.docusphere.folder.domain.Folder;

public record FolderResponse(
        String publicId,
        String name,
        boolean isRoot,
        String parentPublicId
) {
    public static FolderResponse fromEntity(Folder folder) {
        return new FolderResponse(
                folder.getPublicId(),
                folder.getName(),
                folder.isRoot(),
                folder.getParent() != null ? folder.getParent().getPublicId() : null
        );
    }
}
