package com.docusphere.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DocuSphere API")
                        .version("1.0.0")
                        .description("""
                    API REST complète pour la plateforme de gestion documentaire DocuSphere.
                    
                    Cette API permet de gérer :
                    - L'authentification (JWT avec Access/Refresh tokens)
                    - Les utilisateurs et les rôles (RBAC)
                    - Les dossiers et les documents
                    - Le workflow de validation
                    - Les notifications
                    - Le partage de documents et les QR codes
                    
                    **Authentification** : La plupart des endpoints nécessitent un token JWT.
                    Cliquez sur le bouton "Authorize" en haut à droite et entrez votre token
                    au format `Bearer VOTRE_TOKEN`.
                """)
                        .contact(new Contact()
                                .name("DocuSphere Team")
                                .email("delfredtene17@gmail.com")
                                .url("https://docusphere.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8080" + contextPath)
                                .description("Local Development"),
                        new Server().url("https://api.docusphere.com").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Entrez votre token JWT (sans le préfixe 'Bearer')")));
    }
}