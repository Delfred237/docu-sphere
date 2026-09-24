package com.docusphere.document.service;

import com.docusphere.audit.event.AuditEvent;
import com.docusphere.auth.domain.User;
import com.docusphere.common.exception.BusinessException;
import com.docusphere.common.exception.ForbiddenException;
import com.docusphere.common.exception.InvalidFileException;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.common.metrics.BusinessMetrics;
import com.docusphere.document.domain.Document;
import com.docusphere.document.domain.DocumentStatus;
import com.docusphere.document.dto.DocumentResponse;
import com.docusphere.document.dto.DocumentValidationRequest;
import com.docusphere.document.repository.DocumentRepository;
import com.docusphere.document.specification.DocumentSpecifications;
import com.docusphere.folder.domain.Folder;
import com.docusphere.folder.repository.FolderRepository;
import com.docusphere.notification.domain.NotificationType;
import com.docusphere.notification.event.NotificationEvent;
import com.docusphere.notification.listener.NotificationEventListener;
import com.docusphere.storage.StorageProperties;
import com.docusphere.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessMetrics businessMetrics;
    private final HttpServletRequest httpServletRequest;


    @Transactional
    public DocumentResponse submitForReview(String publicId, User currentUser) {
        Document document = getDocumentEntityForAction(publicId, currentUser);

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new BusinessException(
                    "Only documents in DRAFT status can be submitted for review.",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS_TRANSITION"
            );
        }

        document.setStatus(DocumentStatus.PENDING_REVIEW);
        Document saved = documentRepository.save(document);

        // Publication de l'audit
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DOCUMENT_SUBMITTED",
                currentUser.getId(),
                currentUser.getEmail(),
                "DOCUMENT",
                saved.getPublicId(),
                getClientIp(),
                Map.of("status", "PENDING_REVIEW")
        ));

        eventPublisher.publishEvent(new NotificationEvent(
                this,
                NotificationType.DOCUMENT_SUBMITTED,
                currentUser.getId(),
                currentUser.getEmail(),
                "Document Submitted",
                "Your document \"" + document.getName() + "\" has been submitted for review.",
                "/documents/" + document.getPublicId(),
                false
        ));

        return DocumentResponse.fromEntity(saved);
    }

    @Transactional
    public DocumentResponse approveDocument(String publicId, User reviewer, DocumentValidationRequest request) {
        Document document = getDocumentEntityForAction(publicId, reviewer);

        if (document.getStatus() != DocumentStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    "Only documents in PENDING_REVIEW status can be approved.",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS_TRANSITION"
            );
        }

        // Règle métier optionnelle mais recommandée : le créateur ne devrait pas être le validateur
        if (document.getOwner().getId().equals(reviewer.getId())) {
            throw new BusinessException(
                    "You cannot approve a document you own.",
                    HttpStatus.FORBIDDEN,
                    "SELF_APPROVAL_FORBIDDEN"
            );
        }

        document.setStatus(DocumentStatus.APPROVED);
        // Ici on pourrait sauvegarder le commentaire dans une table d'historique (voir étape Audit)
        Document saved = documentRepository.save(document);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DOCUMENT_APPROVED",
                reviewer.getId(),
                reviewer.getEmail(),
                "DOCUMENT",
                saved.getPublicId(),
                getClientIp(),
                Map.of("comment", request.comment() != null ? request.comment() : "")
        ));

        eventPublisher.publishEvent(new NotificationEvent(
                this,
                NotificationType.DOCUMENT_APPROVED,
                document.getOwner().getId(),
                document.getOwner().getEmail(),
                "Document Approved",
                "Your document \"" + document.getName() + "\" has been approved.",
                "/documents/" + document.getPublicId(),
                true // Envoyer par email
        ));

        businessMetrics.incrementDocumentsValidated();

        return DocumentResponse.fromEntity(saved);
    }

    @Transactional
    public DocumentResponse rejectDocument(String publicId, User reviewer, DocumentValidationRequest request) {
        Document document = getDocumentEntityForAction(publicId, reviewer);

        if (document.getStatus() != DocumentStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    "Only documents in PENDING_REVIEW status can be rejected.",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS_TRANSITION"
            );
        }

        document.setStatus(DocumentStatus.REJECTED);
        Document saved = documentRepository.save(document);

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DOCUMENT_REJECTED",
                reviewer.getId(),
                reviewer.getEmail(),
                "DOCUMENT",
                saved.getPublicId(),
                getClientIp(),
                Map.of("comment", request.comment() != null ? request.comment() : "")
        ));

        // À la fin de rejectDocument() :
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                NotificationType.DOCUMENT_REJECTED,
                document.getOwner().getId(),
                document.getOwner().getEmail(),
                "Document Rejected",
                "Your document \"" + document.getName() + "\" has been rejected. Comment: " + request.comment(),
                "/documents/" + document.getPublicId(),
                true
        ));

        businessMetrics.incrementDocumentsRejected();

        return DocumentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> searchDocuments(
            User owner,
            String name,
            DocumentStatus status,
            String mimeType,
            String folderPublicId,
            boolean rootOnly,
            Pageable pageable) {

        // 1. Construction dynamique de la requête
        Specification<Document> spec = Specification.where(
                DocumentSpecifications.isNotDeleted())
                .and(DocumentSpecifications.hasOwner(owner));

        if (name != null && !name.isBlank()) {
            spec = spec.and(DocumentSpecifications.nameContains(name));
        }
        if (status != null) {
            spec = spec.and(DocumentSpecifications.hasStatus(status));
        }
        if (mimeType != null && !mimeType.isBlank()) {
            spec = spec.and(DocumentSpecifications.hasMimeType(mimeType));
        }

        // Logique spécifique au dossier : soit un dossier précis, soit la racine
        if (folderPublicId != null && !folderPublicId.isBlank()) {
            spec = spec.and(DocumentSpecifications.inFolder(folderPublicId));
        } else if (rootOnly) {
            spec = spec.and(DocumentSpecifications.isRootLevel());
        }

        // 2. Exécution paginée
        // Note : Spring Data gère automatiquement le COUNT et le SELECT avec LIMIT/OFFSET
        Page<Document> documentPage = documentRepository.findAll(spec, pageable);

        // 3. Mapping vers DTO
        return documentPage.map(DocumentResponse::fromEntity);
    }

    @Transactional
    public DocumentResponse uploadDocument(User owner, MultipartFile file, String folderPublicId) {
        // 1. Validations basiques
        if (file.isEmpty()) {
            throw new InvalidFileException("Cannot upload empty file.");
        }
        if (file.getSize() > storageProperties.getMaxFileSize()) {
            throw new InvalidFileException("File exceeds maximum allowed size of " + storageProperties.getMaxFileSize() + " bytes.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !storageProperties.getAllowedMimeTypes().contains(mimeType)) {
            throw new InvalidFileException("File type not allowed: " + mimeType);
        }

        // 2. Vérification du dossier (Sécurité IDOR)
        Folder folder = null;
        if (folderPublicId != null && !folderPublicId.isBlank()) {
            folder = folderRepository.findByPublicId(folderPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "publicId", folderPublicId));
            if (!folder.getOwner().getId().equals(owner.getId())) {
                throw new InvalidFileException("Access denied to target folder.");
            }
        }

        // 3. Gestion du Fichier Temporaire (Le cœur de la robustesse)
        Path tempPath = null;
        try {
            // Créer un fichier temporaire sécurisé
            tempPath = Files.createTempFile("docusphere-upload-", ".tmp");

            // Copier le contenu du MultipartFile vers le disque (évite le OutOfMemoryError)
            Files.copy(file.getInputStream(), tempPath, StandardCopyOption.REPLACE_EXISTING);

            // Calculer le Hash SHA-256 du fichier temporaire
            String checksum = calculateSha256(tempPath);

            // Stocker le fichier via le StorageService en ouvrant un nouveau flux
            String storedFilename;
            try (InputStream is = new FileInputStream(tempPath.toFile())) {
                storedFilename = storageService.store(is, file.getOriginalFilename());
            }

            // 4. Création de l'entité Document
            Document document = new Document();
            document.setName(file.getOriginalFilename()); // Nom d'affichage
            document.setOriginalFilename(file.getOriginalFilename());
            document.setStoredFilename(storedFilename);
            document.setMimeType(mimeType);
            document.setSize(file.getSize());
            document.setChecksum(checksum);
            document.setStatus(DocumentStatus.DRAFT);
            document.setOwner(owner);
            document.setFolder(folder);

            Document savedDocument = documentRepository.save(document);

            eventPublisher.publishEvent(new AuditEvent(
                    this,
                    "DOCUMENT_UPLOADED",
                    owner.getId(),
                    owner.getEmail(),
                    "DOCUMENT",
                    savedDocument.getPublicId(),
                    getClientIp(),
                    Map.of("upload", "Upload complete successfully!")
            ));

            businessMetrics.incrementDocumentsUploaded();

            return DocumentResponse.fromEntity(savedDocument);

        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("Failed to process uploaded file", e);
            throw new RuntimeException("Failed to process uploaded file", e);
        } finally {
            // 5. Nettoyage CRITIQUE du fichier temporaire
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tempPath, e);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Document getDocumentEntity(String publicId, User user) {
        Document document = documentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "publicId", publicId));

        // Sécurité IDOR
        if (!document.getOwner().getId().equals(user.getId())) {
            // Ici on pourrait lancer une ForbiddenException, mais pour le download, on veut juste bloquer.
            throw new RuntimeException("Access denied");
        }
        return document;
    }

    public InputStream downloadDocument(String publicId, User user) {
        Document document = getDocumentEntity(publicId, user);
        return storageService.load(document.getStoredFilename());
    }

    @Transactional
    public void deleteDocument(String publicId, User currentUser) {
        Document document = documentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "publicId", publicId));

        // Vérifier que l'utilisateur est le propriétaire
        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You cannot delete a document you don't own");
        }

        // Soft delete
        document.setDeleted(true);
        document.setDeletedAt(Instant.now());
        documentRepository.save(document);

        // Publier un événement d'audit
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "DOCUMENT_DELETED",
                currentUser.getId(),
                currentUser.getEmail(),
                "DOCUMENT",
                document.getPublicId(),
                getClientIp(),
                Map.of("name", document.getName())
        ));
    }

    // Utilitaire

    /**
     * Méthode utilitaire pour récupérer l'IP
     * @return String IP Adress
     */
    private String getClientIp() {
        String xForwardedFor = httpServletRequest.getHeader("X-FORWARDED-FOR");
        return (xForwardedFor != null) ? xForwardedFor.split(",")[0] : httpServletRequest.getRemoteAddr();
    }

    /**
     * Méthode utilitaire interne pour récupérer un document et vérifier les droits de base.
     * Différent de getDocumentEntity qui est utilisé pour le téléchargement.
     */
    private Document getDocumentEntityForAction(String publicId, User user) {
        Document document = documentRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "publicId", publicId));

        // Pour soumettre, il faut être le propriétaire.
        // Pour approuver/rejeter, la sécurité est gérée par @PreAuthorize sur le controller,
        // mais on s'assure ici que l'utilisateur a au moins accès à la lecture du document
        // (soit il est owner, soit il a le droit de validation global).
        // Pour simplifier le MVP : on considère que si tu as la permission DOCUMENT_VALIDATE,
        // tu as le droit de lire n'importe quel document en attente.

        boolean isOwner = document.getOwner().getId().equals(user.getId());
        boolean isReviewer = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .anyMatch(p -> p.getName() == com.docusphere.auth.domain.PermissionName.DOCUMENT_VALIDATE ||
                        p.getName() == com.docusphere.auth.domain.PermissionName.DOCUMENT_REJECT);

        if (!isOwner && !isReviewer) {
            throw new com.docusphere.common.exception.ForbiddenException("Access denied to this document.");
        }

        return document;
    }

    /**
     * Calcule le hash SHA-256 d'un fichier de manière efficace (par blocs).
     */
    private String calculateSha256(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new FileInputStream(filePath.toFile())) {
            byte[] buffer = new byte[8192]; // Lecture par blocs de 8 Ko
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hashBytes = digest.digest();

        // Conversion en chaîne hexadécimale
        StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
