package com.docusphere.common.email;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationCode(String toEmail, String firstName, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Préparation du contexte Thymeleaf
            Context context = new Context();
            context.setVariable("fullName", firstName);
            context.setVariable("code", code);
            context.setVariable("expirationMinutes", 15);

            // Génération du HTML
            String htmlContent = templateEngine.process("emails/verification-code", context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your DocuSphere Verification Code");
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("Verification code sent to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", toEmail, e);
        }
    }

    @Async
    public void sendNotificationEmail(String toEmail, String title, String message, String actionUrl) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            Context context = new Context();
            context.setVariable("title", title);
            context.setVariable("message", message);
            context.setVariable("actionUrl", actionUrl);

            String htmlContent = templateEngine.process("emails/notification", context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[DocuSphere] " + title);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Notification email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send notification email to {}", toEmail, e);
        }
    }
}
