package com.docusphere.folder.service;

import com.docusphere.audit.event.AuditEvent;
import com.docusphere.auth.domain.User;
import com.docusphere.common.exception.BusinessException;
import com.docusphere.common.exception.DuplicateResourceException;
import com.docusphere.common.exception.ForbiddenException;
import com.docusphere.common.exception.ResourceNotFoundException;
import com.docusphere.document.repository.DocumentRepository;
import com.docusphere.folder.domain.Folder;
import com.docusphere.folder.dto.CreateFolderRequest;
import com.docusphere.folder.dto.FolderResponse;
import com.docusphere.folder.repository.FolderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final HttpServletRequest httpServletRequest;

    @Transactional
    public FolderResponse createFolder(User owner, CreateFolderRequest request) {
        Folder parent = null;
        Long parentId = null;

        // Si un parentId est fourni, on vérifie qu'il existe
        if (request.parentId() != null && !request.parentId().isBlank()) {
            parent = folderRepository.findByPublicId(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder", "publicId", request.parentId()));

            // Sécurité métier : On ne peut créer un dossier que dans SON PROPRE dossier parent
            if (!parent.getOwner().getId().equals(owner.getId())) {
                throw new IllegalArgumentException("You cannot create a folder in another user's directory.");
            }
            parentId = parent.getId();
        }

        // Vérification des doublons
        if (folderRepository.existsByNameAndOwnerIdAndParentIdAndDeletedFalse(request.name(), owner.getId(), parentId)) {
            throw new DuplicateResourceException("A folder with this name already exists in this location.");
        }

        Folder folder = new Folder();
        folder.setName(request.name());
        folder.setOwner(owner);
        folder.setParent(parent);

        Folder savedFolder = folderRepository.save(folder);
        return FolderResponse.fromEntity(savedFolder);
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getRootFolders(User owner) {
        return folderRepository.findAllByParentIsNullAndOwnerIdAndDeletedFalse(owner.getId())
                .stream()
                .map(FolderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FolderResponse> getSubFolders(User owner, String parentPublicId) {
        Folder parent = folderRepository.findByPublicId(parentPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "publicId", parentPublicId));

        // Sécurité : Vérifier que l'utilisateur est propriétaire ou a accès
        if (!parent.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Access denied to this folder.");
        }

        return folderRepository.findAllByParentIdAndDeletedFalse(parent.getId())
                .stream()
                .map(FolderResponse::fromEntity)
                .toList();
    }

    @Transactional
    public FolderResponse renameFolder(String publicId, String newName, User currentUser) {
        Folder folder = folderRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "publicId", publicId));

        // Vérifier que l'utilisateur est le propriétaire
        if (!folder.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You cannot rename a folder you don't own");
        }

        folder.setName(newName.trim());
        folder.setUpdatedAt(Instant.now());
        Folder saved = folderRepository.save(folder);

        // Publier un événement d'audit
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "FOLDER_RENAMED",
                currentUser.getId(),
                currentUser.getEmail(),
                "FOLDER",
                saved.getPublicId(),
                getClientIp(),
                Map.of("name", saved.getName())
        ));

        return FolderResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteFolder(String publicId, User currentUser) {
        Folder folder = folderRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Folder", "publicId", publicId));

        // Vérifier que l'utilisateur est le propriétaire
        if (!folder.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You cannot delete a folder you don't own");
        }

        // Vérifier que le dossier est vide (pas d'enfants ni de documents)
        if (folderRepository.existsByParentIdAndDeletedFalse(folder.getId())) {
            throw new BusinessException(
                    "Cannot delete folder with subfolders. Delete subfolders first.",
                    HttpStatus.BAD_REQUEST,
                    "FOLDER_NOT_EMPTY"
            );
        }

        if (documentRepository.existsByFolderIdAndDeletedFalse(folder.getId())) {
            throw new BusinessException(
                    "Cannot delete folder with documents. Move or delete documents first.",
                    HttpStatus.BAD_REQUEST,
                    "FOLDER_NOT_EMPTY"
            );
        }

        // Soft delete
        folder.setDeleted(true);
        folder.setDeletedAt(Instant.now());
        folderRepository.save(folder);
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
}