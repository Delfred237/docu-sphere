package com.docusphere.sharing.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.document.domain.Document;
import com.docusphere.document.dto.DocumentResponse;
import com.docusphere.document.service.DocumentService;
import com.docusphere.qr.service.QrCodeService;
import com.docusphere.sharing.domain.ShareLink;
import com.docusphere.sharing.dto.ShareLinkResponse;
import com.docusphere.sharing.service.ShareLinkService;
import com.docusphere.storage.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@Slf4j
@RestController
@RequestMapping("/v1/shared")
@RequiredArgsConstructor
public class SharedDocumentController {

    private final ShareLinkService shareLinkService;
    private final StorageService storageService;
    private final QrCodeService qrCodeService;

    public record CreateShareLinkRequest(
            boolean allowDownload,
            long expirationDays // Ex: 7 jours
    ) {}

    @PostMapping("/{documentPublicId}/share")
    public ResponseEntity<ShareLinkResponse> createShareLink(
            @AuthenticationPrincipal User user,
            @PathVariable String documentPublicId,
            @Valid @RequestBody CreateShareLinkRequest request) {

        ShareLinkResponse response = shareLinkService.createShareLink(
                documentPublicId, user, request.allowDownload(), request.expirationDays()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/share/{linkPublicId}/qrcode")
    public ResponseEntity<byte[]> getShareLinkQrCode(
            @AuthenticationPrincipal User user,
            @PathVariable String linkPublicId) {

        // Récupérer le lien pour construire l'URL
        // Pour simplifier, on suppose que l'utilisateur a le droit de voir le QR code
        // Dans une vraie app, on vérifierait les droits

        String baseUrl = "http://localhost:5173/api/v1/shared/"; // URL du frontend
        String content = baseUrl + linkPublicId;

        try {
            byte[] qrCodeImage = qrCodeService.generateQrCode(content, 300, 300);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{token}/info")
    public ResponseEntity<DocumentResponse> getSharedDocumentInfo(@PathVariable String token) {
        return ResponseEntity.ok(shareLinkService.getSharedDocumentResponse(token));
    }

    @GetMapping("/{token}/download")
    public ResponseEntity<InputStreamResource> downloadSharedDocument(@PathVariable String token) {
        Document document = shareLinkService.prepareSharedDocumentDownload(token);

        InputStream fileStream = storageService.load(document.getStoredFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .body(new InputStreamResource(fileStream));
    }
}