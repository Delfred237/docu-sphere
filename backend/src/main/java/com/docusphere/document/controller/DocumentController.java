package com.docusphere.document.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.document.domain.Document;
import com.docusphere.document.dto.DocumentResponse;
import com.docusphere.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
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
