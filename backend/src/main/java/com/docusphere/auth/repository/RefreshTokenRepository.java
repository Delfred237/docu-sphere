package com.docusphere.auth.repository;

import com.docusphere.auth.domain.RefreshToken;
import com.docusphere.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user); // Pour déconnecter de tous les appareils si besoin
}
