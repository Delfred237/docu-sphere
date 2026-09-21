package com.docusphere.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Centralise toutes les métriques métier custom.
 *
 * Exemples de métriques exposées :
 * - docusphere.documents.uploaded_total
 * - docusphere.documents.validated_total
 * - docusphere.authentication.failures_total
 * - docusphere.emails.sent_total
 */
@Component
public class BusinessMetrics {

    private final Counter documentsUploaded;
    private final Counter documentsValidated;
    private final Counter documentsRejected;
    private final Counter authenticationFailures;
    private final Counter emailsSent;
    private final Counter emailsFailed;
    private final Timer documentUploadDuration;

    public BusinessMetrics(MeterRegistry registry) {
        this.documentsUploaded = Counter.builder("docusphere.documents.uploaded")
                .description("Nombre total de documents uploadés")
                .tag("type", "document")
                .register(registry);

        this.documentsValidated = Counter.builder("docusphere.documents.validated")
                .description("Nombre total de documents validés")
                .tag("status", "approved")
                .register(registry);

        this.documentsRejected = Counter.builder("docusphere.documents.rejected")
                .description("Nombre total de documents rejetés")
                .tag("status", "rejected")
                .register(registry);

        this.authenticationFailures = Counter.builder("docusphere.authentication.failures")
                .description("Nombre de tentatives de connexion échouées")
                .register(registry);

        this.emailsSent = Counter.builder("docusphere.emails.sent")
                .description("Nombre d'emails envoyés avec succès")
                .register(registry);

        this.emailsFailed = Counter.builder("docusphere.emails.failed")
                .description("Nombre d'emails qui ont échoué à l'envoi")
                .register(registry);

        this.documentUploadDuration = Timer.builder("docusphere.documents.upload.duration")
                .description("Temps de traitement d'un upload de document")
                .register(registry);
    }

    public void incrementDocumentsUploaded() {
        documentsUploaded.increment();
    }

    public void incrementDocumentsValidated() {
        documentsValidated.increment();
    }

    public void incrementDocumentsRejected() {
        documentsRejected.increment();
    }

    public void incrementAuthenticationFailures() {
        authenticationFailures.increment();
    }

    public void incrementEmailsSent() {
        emailsSent.increment();
    }

    public void incrementEmailsFailed() {
        emailsFailed.increment();
    }

    public Timer getDocumentUploadDuration() {
        return documentUploadDuration;
    }
}