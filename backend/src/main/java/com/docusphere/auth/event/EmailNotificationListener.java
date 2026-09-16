package com.docusphere.auth.event;

import com.docusphere.common.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailService emailService;

    // N'exécute cette méthode QUE si la transaction d'inscription a réussi (COMMIT)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.debug("Handling UserRegisteredEvent for {}", event.getEmail());
        emailService.sendVerificationEmail(event.getEmail(), event.getToken());
    }
}
