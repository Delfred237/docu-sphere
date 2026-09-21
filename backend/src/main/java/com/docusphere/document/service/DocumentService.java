package com.docusphere.document.service;

import com.docusphere.auth.domain.User;
import com.docusphere.common.exception.InvalidFileException;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.document.domain.Document;
import com.docusphere.document.domain.DocumentStatus;
import com.docusphere.document.dto.DocumentResponse;
import com.docusphere.document.repository.DocumentRepository;
import com.docusphere.document.specification.DocumentSpecifications;
import com.docusphere.folder.domain.Folder;
import com.docusphere.folder.repository.FolderRepository;
import com.docusphere.storage.StorageProperties;
import com.docusphere.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final StorageService storageService;
    private final StorageProperties storageProperties;


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
        Specification<Document> spec = Specification.where(DocumentSpecifications.hasOwner(owner));

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

    // Utilitaire
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
