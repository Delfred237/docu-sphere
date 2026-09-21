package com.docusphere.document.dto;

import jakarta.validation.constraints.Size;

public record DocumentValidationRequest(
        @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
        String comment
) {}