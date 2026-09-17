package com.docusphere.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFolderRequest(
        @NotBlank(message = "Folder name is required")
        @Size(max = 255)
        String name,

        // Optionnel : null signifie dossier racine
        String parentId
) {}
