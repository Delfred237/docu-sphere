package com.docusphere.document.domain;

import com.docusphere.auth.domain.User;
import com.docusphere.common.domain.BaseEntity;
import com.docusphere.folder.domain.Folder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document extends BaseEntity {

    @Column(nullable = false)
    private String name; // Nom d'affichage (peut être modifié par l'utilisateur)

    @Column(name = "original_filename", nullable = false)
    private String originalFilename; // Nom lors de l'upload (pour l'audit)

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename; // UUID sur le disque

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(nullable = false)
    private Long size; // en bytes

    @Column(nullable = false, length = 64)
    private String checksum; // SHA-256

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id") // Nullable : un document peut être à la racine
    private Folder folder;
}
