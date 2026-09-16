package com.docusphere.auth.event;

import org.springframework.context.ApplicationEvent;

public class UserRegisteredEvent extends ApplicationEvent {

    private final String email;
    private final String token;

    public UserRegisteredEvent(Object source, String email, String token) {
        super(source);
        this.email = email;
        this.token = token;
    }

    public String getEmail() { return email; }
    public String getToken() { return token; }
}
