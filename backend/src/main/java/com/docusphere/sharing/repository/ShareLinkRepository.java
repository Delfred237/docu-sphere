package com.docusphere.sharing.repository;

import com.docusphere.sharing.domain.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByTokenAndDeletedFalse(String token);

    Optional<ShareLink> findByPublicIdAndDeletedFalse(String linkPublicId);
}