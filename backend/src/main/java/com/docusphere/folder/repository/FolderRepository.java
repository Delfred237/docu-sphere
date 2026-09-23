package com.docusphere.folder.repository;

import com.docusphere.auth.domain.User;
import com.docusphere.folder.domain.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    // Dossiers à la racine pour un utilisateur
    List<Folder> findByOwnerAndParentIsNullOrderByCreatedAtDesc(User owner);

    // Dossiers enfants d'un dossier spécifique
    List<Folder> findByParentOrderByCreatedAtDesc(Folder parent);

    // Vérification de doublon (nom + propriétaire + parent)
    boolean existsByNameAndOwnerIdAndParentIdAndDeletedFalse(String name, Long ownerId, Long parentId);

    // Recherche par publicId
    Optional<Folder> findByPublicId(String publicId);

    long countByOwnerIdAndDeletedFalse(Long id);
}
