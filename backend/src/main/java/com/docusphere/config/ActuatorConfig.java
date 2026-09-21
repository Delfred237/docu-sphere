package com.docusphere.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health check customisé pour valider les composants critiques.
 * Spring Boot détecte automatiquement tous les HealthIndicator.
 */
@Component
public class ActuatorConfig implements HealthIndicator {

    private final com.docusphere.storage.StorageProperties storageProperties;

    public ActuatorConfig(com.docusphere.storage.StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public Health health() {
        java.nio.file.Path storagePath = java.nio.file.Paths.get(storageProperties.getLocation());

        if (!java.nio.file.Files.exists(storagePath)) {
            return Health.down()
                    .withDetail("reason", "Storage directory does not exist")
                    .withDetail("path", storagePath.toString())
                    .build();
        }

        if (!java.nio.file.Files.isWritable(storagePath)) {
            return Health.down()
                    .withDetail("reason", "Storage directory is not writable")
                    .build();
        }

        return Health.up()
                .withDetail("path", storagePath.toString())
                .withDetail("writable", true)
                .build();
    }
}