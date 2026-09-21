package com.docusphere.sharing.controller;

import com.docusphere.auth.domain.User;
import com.docusphere.sharing.dto.ShareLinkResponse;
import com.docusphere.sharing.service.ShareLinkService;
import com.docusphere.qr.service.QrCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/documents")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;
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

        String baseUrl = "http://localhost:5173/share/"; // URL du frontend
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
}