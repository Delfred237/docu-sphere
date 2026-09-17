package com.docusphere.auth.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class UserRegisteredEvent extends ApplicationEvent {

    @Getter
    private final String email;
    private final String fullName;
    private final String code;

    public UserRegisteredEvent(Object source, String email, String fullName, String code) {
        super(source);
        this.email = email;
        this.fullName = fullName;
        this.code = code;
    }

    public String getFullName() { return fullName; }
    public String getCode() { return code; }
}
