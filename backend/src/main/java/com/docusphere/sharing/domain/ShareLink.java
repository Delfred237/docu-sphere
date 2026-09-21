package com.docusphere.sharing.domain;

import com.docusphere.auth.domain.User;
import com.docusphere.common.domain.BaseEntity;
import com.docusphere.document.domain.Document;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "share_links")
@Getter
@Setter
@NoArgsConstructor
public class ShareLink extends BaseEntity {

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdByUser;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "allow_download", nullable = false)
    private boolean allowDownload = true;

    @Column(name = "download_count", nullable = false)
    private int downloadCount = 0;

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }
}