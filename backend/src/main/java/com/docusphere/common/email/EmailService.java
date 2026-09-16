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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String subject = "Verify your DocuSphere account";
        // Dans une vraie app, on externaliserait ce template. Ici, on reste pragmatique.
        String verificationLink = "http://localhost:5173/verify-email?token=" + token;
        String body = buildVerificationEmailBody(toEmail, verificationLink);


        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true); // true = isHtml

            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (MessagingException e) {
            // En production, on enverrait ça dans une queue de retry (DLQ).
            // Ici, on log l'erreur pour ne pas faire crasher le thread asynchrone
            log.error("Failed to send verification email to {}", toEmail, e);
        }
    }

    private String buildVerificationEmailBody(String username, String link) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2>Welcome to DocuSphere, %s!</h2>
                <p>Please click the button below to verify your email address and activate your account.</p>
                <a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #007bff; color: #ffffff; text-decoration: none; border-radius: 5px;">Verify Email</a>
                <p>If you did not create an account, no further action is required.</p>
                <p>Regards,<br>The DocuSphere Team</p>
            </div>
            """.formatted(username, link);
    }
}
