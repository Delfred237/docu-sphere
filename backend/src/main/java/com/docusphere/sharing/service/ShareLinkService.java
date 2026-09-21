package com.docusphere.sharing.service;

import com.docusphere.auth.domain.User;
import com.docusphere.common.exception.BusinessException;
import com.docusphere.common.exception.ForbiddenException;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.document.domain.Document;
import com.docusphere.document.repository.DocumentRepository;
import com.docusphere.sharing.domain.ShareLink;
import com.docusphere.sharing.dto.ShareLinkResponse;
import com.docusphere.sharing.repository.ShareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ShareLinkService {

    private final ShareLinkRepository shareLinkRepository;
    private final DocumentRepository documentRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    @Transactional
    public ShareLinkResponse createShareLink(String documentPublicId, User creator, boolean allowDownload, long expirationDays) {
        Document document = documentRepository.findByPublicId(documentPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "publicId", documentPublicId));

        // Sécurité : Seul le propriétaire peut partager son document
        if (!document.getOwner().getId().equals(creator.getId())) {
            throw new ForbiddenException("You cannot share a document you do not own.");
        }

        ShareLink shareLink = new ShareLink();
        shareLink.setToken(generateSecureToken());
        shareLink.setDocument(document);
        shareLink.setCreatedByUser(creator);
        shareLink.setExpiresAt(Instant.now().plusSeconds(expirationDays * 24 * 3600));
        shareLink.setAllowDownload(allowDownload);

        ShareLink savedLink = shareLinkRepository.save(shareLink);
        return ShareLinkResponse.fromEntity(savedLink);
    }

    @Transactional(readOnly = true)
    public ShareLink getValidShareLink(String token) {
        ShareLink shareLink = shareLinkRepository.findByTokenAndDeletedFalse(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found or invalid."));

        if (shareLink.isExpired()) {
            throw new BusinessException("This share link has expired.", HttpStatus.GONE, "LINK_EXPIRED");
        }

        return shareLink;
    }

    @Transactional
    public void recordDownload(String token) {
        ShareLink shareLink = shareLinkRepository.findByTokenAndDeletedFalse(token)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found."));

        shareLink.incrementDownloadCount();
        shareLinkRepository.save(shareLink);
    }

    @Transactional
    public void revokeShareLink(String linkPublicId, User user) {
        ShareLink shareLink = shareLinkRepository.findByPublicIdAndDeletedFalse(linkPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found."));

        // Sécurité : Seul le créateur du lien peut le révoquer
        if (!shareLink.getCreatedByUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You cannot revoke a share link you did not create.");
        }

        shareLink.setDeleted(true);
        shareLink.setDeletedAt(Instant.now());
        shareLinkRepository.save(shareLink);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[48]; // 48 bytes = 64 caractères en Base64
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }
}