package com.docusphere.audit.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class AuditEvent extends ApplicationEvent {

    private final String action;
    private final Long userId;
    private final String userEmail;
    private final String resourceType;
    private final String resourceId;
    private final String ipAddress;
    private final Map<String, Object> details;

    public AuditEvent(Object source, String action, Long userId, String userEmail,
                      String resourceType, String resourceId, String ipAddress, Map<String, Object> details) {
        super(source);
        this.action = action;
        this.userId = userId;
        this.userEmail = userEmail;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.details = details;
    }
}