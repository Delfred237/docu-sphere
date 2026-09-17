package com.docusphere.auth.repository;

import com.docusphere.auth.domain.EmailVerificationToken;
import com.docusphere.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByCode(String code);

    Optional<EmailVerificationToken> findByUser(User savedUser);
}
