package com.docusphere.document.specification;

import com.docusphere.auth.domain.User;
import com.docusphere.document.domain.Document;
import com.docusphere.document.domain.DocumentStatus;
import org.springframework.data.jpa.domain.Specification;

public final class DocumentSpecifications {

    private DocumentSpecifications() {
        throw new UnsupportedOperationException("Utility class");
    }

    // CRITIQUE : Sécurité. Toujours filtrer par propriétaire pour éviter les fuites de données.
    public static Specification<Document> hasOwner(User owner) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("owner"), owner);
    }

    public static Specification<Document> hasStatus(DocumentStatus status) {
        return (root, query, criteriaBuilder) ->
                status == null ? null : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Document> hasMimeType(String mimeType) {
        return (root, query, criteriaBuilder) ->
                mimeType == null ? null : criteriaBuilder.equal(root.get("mimeType"), mimeType);
    }

    public static Specification<Document> nameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.isBlank()) return null;
            // ILIKE (insensible à la casse) en PostgreSQL
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Document> inFolder(String folderPublicId) {
        return (root, query, criteriaBuilder) -> {
            if (folderPublicId == null || folderPublicId.isBlank()) return null;
            // Jointure implicite pour accéder à publicId du dossier
            return criteriaBuilder.equal(root.get("folder").get("publicId"), folderPublicId);
        };
    }

    public static Specification<Document> isRootLevel() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("folder"));
    }
}