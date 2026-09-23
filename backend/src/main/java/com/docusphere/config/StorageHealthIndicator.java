package com.docusphere.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Health check customisé pour valider les composants critiques.
 *
 * Désactivé en profil test car le dossier de stockage n'existe pas.
 */
@Component
@Profile("!test")  // ← Désactivé en profil test
public class StorageHealthIndicator implements HealthIndicator {

    private final com.docusphere.storage.StorageProperties storageProperties;

    public StorageHealthIndicator(com.docusphere.storage.StorageProperties storageProperties) {
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