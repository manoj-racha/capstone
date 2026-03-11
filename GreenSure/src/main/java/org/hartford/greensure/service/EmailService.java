package org.hartford.greensure.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Send a password reset email with a link containing the reset token.
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("GreenSure — Reset Your Password");
            message.setText(
                    "Hello,\n\n" +
                            "We received a request to reset your GreenSure account password.\n\n" +
                            "Click the link below to reset your password:\n" +
                            resetLink + "\n\n" +
                            "This link will expire in 30 minutes.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "The GreenSure Team");

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset email. Please try again.");
        }
    }

    /**
     * Send a welcome email to a newly created agent with their credentials.
     */
    public void sendAgentWelcomeEmail(String toEmail, String fullName, String tempPassword) {
        try {
            String loginLink = frontendUrl + "/login";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to GreenSure — Your Agent Account");
            message.setText(
                    "Hello " + fullName + ",\n\n" +
                            "Welcome to GreenSure! Your field agent account has been created.\n\n" +
                            "Your login credentials:\n" +
                            "Email: " + toEmail + "\n" +
                            "Temporary Password: " + tempPassword + "\n\n" +
                            "Login here: " + loginLink + "\n\n" +
                            "Please change your password after your first login.\n\n" +
                            "Best regards,\n" +
                            "The GreenSure Admin Team");

            mailSender.send(message);
            log.info("Welcome email sent to agent {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to agent {}: {}", toEmail, e.getMessage());
            // Don't throw — agent was created successfully, email is best-effort
        }
    }
}
