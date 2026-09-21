package com.docusphere.document.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.document.domain.Document;
import com.docusphere.document.domain.DocumentStatus;
import com.docusphere.document.dto.DocumentResponse;
import com.docusphere.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) String folderId) throws IOException {

        DocumentResponse response = documentService.uploadDocument(user, file, folderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> searchDocuments(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) String folderId,
            @RequestParam(required = false, defaultValue = "false") boolean rootOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<DocumentResponse> results = documentService.searchDocuments(
                user, name, status, mimeType, folderId, rootOnly, pageable
        );

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{publicId}/download")
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {

        Document document = documentService.getDocumentEntity(publicId, user);
        var inputStream = documentService.downloadDocument(publicId, user);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .body(new InputStreamResource(inputStream));
    }
}