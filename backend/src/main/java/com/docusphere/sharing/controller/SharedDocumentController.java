package com.docusphere.sharing.controller;

import com.docusphere.document.domain.Document;
import com.docusphere.document.dto.DocumentResponse;
import com.docusphere.document.service.DocumentService;
import com.docusphere.sharing.domain.ShareLink;
import com.docusphere.sharing.service.ShareLinkService;
import com.docusphere.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;

@RestController
@RequestMapping("/v1/share")
@RequiredArgsConstructor
public class SharedDocumentController {

    private final ShareLinkService shareLinkService;
    private final StorageService storageService;

    @GetMapping("/{token}/info")
    public ResponseEntity<DocumentResponse> getSharedDocumentInfo(@PathVariable String token) {
        ShareLink shareLink = shareLinkService.getValidShareLink(token);
        Document document = shareLink.getDocument();
        return ResponseEntity.ok(DocumentResponse.fromEntity(document));
    }

    @GetMapping("/{token}/download")
    public ResponseEntity<InputStreamResource> downloadSharedDocument(@PathVariable String token) {
        ShareLink shareLink = shareLinkService.getValidShareLink(token);

        if (!shareLink.isAllowDownload()) {
            return ResponseEntity.status(403).build();
        }

        // Incrémenter le compteur de téléchargements
        shareLinkService.recordDownload(token);

        Document document = shareLink.getDocument();
        InputStream fileStream = storageService.load(document.getStoredFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getOriginalFilename() + "\"")
                .body(new InputStreamResource(fileStream));
    }
}