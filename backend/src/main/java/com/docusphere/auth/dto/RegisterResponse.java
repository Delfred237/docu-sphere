package com.docusphere.auth.dto;

public record RegisterResponse(
        String publicId,
        String email,
        String firstName,
        String lastName
) {}
