package com.docusphere.audit.listener;

import com.docusphere.audit.domain.AuditLog;
import com.docusphere.audit.event.AuditEvent;
import com.docusphere.audit.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuditEvent(AuditEvent event) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(event.getAction());
            auditLog.setUserId(event.getUserId());
            auditLog.setUserEmail(event.getUserEmail());
            auditLog.setResourceType(event.getResourceType());
            auditLog.setResourceId(event.getResourceId());
            auditLog.setIpAddress(event.getIpAddress());

            // Conversion du Map de détails en JSON
            if (event.getDetails() != null && !event.getDetails().isEmpty()) {
                auditLog.setDetails(objectMapper.writeValueAsString(event.getDetails()));
            }

            auditLogRepository.save(auditLog);
            log.debug("Audit log recorded: {} for resource {}", event.getAction(), event.getResourceId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit details for action {}", event.getAction(), e);
        }
    }
}