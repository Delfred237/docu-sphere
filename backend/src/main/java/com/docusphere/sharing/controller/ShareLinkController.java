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

import java.util.List;

@RestController
@RequestMapping("/v1/share-links")
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;


    @GetMapping
    public ResponseEntity<List<ShareLinkResponse>> getUserShareLinks(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(shareLinkService.getUserShareLinks(user));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<Void> revokeShareLink(
            @AuthenticationPrincipal User user,
            @PathVariable String publicId) {
        shareLinkService.revokeShareLinkByPublicId(publicId, user);
        return ResponseEntity.noContent().build();
    }
}