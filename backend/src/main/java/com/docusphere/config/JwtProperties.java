package com.docusphere.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class JwtProperties {

    private String jwtSecret;
    private long jwtExpirationMs;
    private long refreshTokenExpirationMs;
}
