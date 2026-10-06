package com.docusphere.common.email;

import com.docusphere.common.metrics.BusinessMetrics;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final BusinessMetrics businessMetrics;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationCode(String toEmail, String firstName, String code) {

            // Préparation du contexte Thymeleaf
            Context context = new Context();
            context.setVariable("fullName", firstName);
            context.setVariable("code", code);
            context.setVariable("expirationMinutes", 15);

            // Génération du HTML
            String htmlContent = templateEngine.process("emails/verification-code", context);

            sendHtmlEmail(toEmail, "Verify your DocuSphere account", htmlContent);
            businessMetrics.incrementEmailsSent();
            log.info("Verification code sent to {}", toEmail);


    }

    public void sendPasswordResetEmail(String to, String firstName, String code) {
        Context context = new Context();
        context.setVariables(Map.of(
                "firstName", firstName,
                "code", code
        ));

        String html = templateEngine.process("emails/password-reset", context);
        sendHtmlEmail(to, "Reset your DocuSphere password", html);
    }

    @Async
    public void sendNotificationEmail(String toEmail, String title, String message, String actionUrl) {
    // Déterminer le template selon le titre ou le contenu
        String templateName = determineTemplate(title, message);

        Context context = new Context();
        context.setVariables(Map.of(
                "title", title,
                "message", message,
                "actionUrl", actionUrl != null ? actionUrl : ""
        ));

        String html = templateEngine.process(templateName, context);
        sendHtmlEmail(toEmail, title, html);
    }

    /**
     * Détermine quel template utiliser selon le contenu de la notification
     */
    private String determineTemplate(String title, String message) {
        if (title != null) {
            if (title.contains("Approved")) return "emails/document-approved";
            if (title.contains("Rejected")) return "emails/document-rejected";
            if (title.contains("Submitted")) return "emails/document-submitted";
        }

        if (message != null) {
            if (message.contains("approved")) return "emails/document-approved";
            if (message.contains("rejected")) return "emails/document-rejected";
            if (message.contains("submitted")) return "emails/document-submitted";
        }

        return "emails/notification-generic"; // Template par défaut
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom("noreply@docusphere.com", "DocuSphere");
            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
