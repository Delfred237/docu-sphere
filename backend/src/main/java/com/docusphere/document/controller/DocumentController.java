package com.docusphere.document.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.document.domain.Document;
import com.docusphere.document.domain.DocumentStatus;
import com.docusphere.document.dto.DocumentResponse;
import com.docusphere.document.dto.DocumentValidationRequest;
import com.docusphere.document.dto.UpdateDocumentRequest;
import com.docusphere.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Gestion des documents (upload, recherche, workflow)")
public class DocumentController {

    private final DocumentService documentService;


    @Operation(summary = "Soumettre pour validation", description = "Passe le document de DRAFT à PENDING_REVIEW.")
    @PostMapping("/{publicId}/submit")
    public ResponseEntity<DocumentResponse> submitForReview(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {
        return ResponseEntity.ok(documentService.submitForReview(publicId, user));
    }

    @Operation(summary = "Approuver un document", description = "Approuve un document en attente de validation. Nécessite la permission DOCUMENT_VALIDATE.")
    @PostMapping("/{publicId}/approve")
    @PreAuthorize("hasAuthority('DOCUMENT_VALIDATE')")
    public ResponseEntity<DocumentResponse> approveDocument(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId,
            @Valid @RequestBody(required = false) DocumentValidationRequest request) {
        // Si le body est vide, on crée un request vide
        if (request == null) request = new DocumentValidationRequest(null);
        return ResponseEntity.ok(documentService.approveDocument(publicId, user, request));
    }

    @Operation(summary = "Rejéter un document", description = "Réjète un document en attente de validation. Nécessite la permission DOCUMENT_REJECT.")
    @PostMapping("/{publicId}/reject")
    @PreAuthorize("hasAuthority('DOCUMENT_REJECT')")
    public ResponseEntity<DocumentResponse> rejectDocument(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId,
            @Valid @RequestBody(required = false) DocumentValidationRequest request) {
        if (request == null) request = new DocumentValidationRequest(null);
        return ResponseEntity.ok(documentService.rejectDocument(publicId, user, request));
    }

    @Operation(summary = "Uploader un document", description = "Upload un fichier dans un dossier spécifique ou à la racine.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Le fichier à uploader")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Dossier de destination (publicId)")
            @RequestParam(value = "folderId", required = false) String folderId) {

        DocumentResponse response = documentService.uploadDocument(user, file, folderId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Rechercher des documents", description = "Recherche paginée avec filtres par nom, statut, type MIME et dossier.")
    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> searchDocuments(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Recherche par nom (insensible à la casse)")
            @RequestParam(required = false) String name,
            @Parameter(description = "Filtrer par statut")
            @RequestParam(required = false) DocumentStatus status,
            @Parameter(description = "Filtrer par type MIME")
            @RequestParam(required = false) String mimeType,
            @Parameter(description = "Filtrer par dossier (publicId)")
            @RequestParam(required = false) String folderId,
            @Parameter(description = "Retourner uniquement les documents à la racine")
            @RequestParam(required = false, defaultValue = "false") boolean rootOnly,
            @Parameter(description = "Pagination et tri")
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<DocumentResponse> results = documentService.searchDocuments(
                user, name, status, mimeType, folderId, rootOnly, pageable
        );

        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Télécharger un document", description = "Télécharge le fichier original du document.")
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

    @PatchMapping("/{publicId}/rename")
    public ResponseEntity<DocumentResponse> renameDocument(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId,
            @Valid @RequestBody UpdateDocumentRequest request) {
        return ResponseEntity.ok(documentService.renameDocument(publicId, request.name(), user));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> deleteDocument(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {
        documentService.deleteDocument(publicId, user);
        return ResponseEntity.noContent().build();
    }
}