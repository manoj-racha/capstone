package org.hartford.greensure.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private Gmail gmail;

    @Value("${gmail.sender.email:}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        try {
            String fromAddress = (fromEmail == null || fromEmail.isBlank()) ? "me" : fromEmail;
            Message gmailMessage = buildMessage(fromAddress, to, subject, wrapHtml(body));
            gmail.users().messages().send("me", gmailMessage).execute();
            log.info("Email sent to {} with subject {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private Message buildMessage(String from, String to, String subject, String htmlBody) throws Exception {
        Session session = Session.getInstance(new Properties(), null);
        MimeMessage mimeMessage = new MimeMessage(session);

        if (!"me".equalsIgnoreCase(from)) {
            mimeMessage.setFrom(new InternetAddress(from));
        }
        mimeMessage.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(to, false));
        mimeMessage.setSubject(subject, StandardCharsets.UTF_8.name());
        mimeMessage.setContent(htmlBody, "text/html; charset=UTF-8");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);

        Message message = new Message();
        message.setRaw(Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray()));
        return message;
    }

    private String wrapHtml(String body) {
        return """
                <div style=\"margin:0;padding:0;background:#f4f7f4;font-family:Arial,sans-serif;color:#1f2937;\">
                    <table role=\"presentation\" width=\"100%%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px 0;\">
                        <tr>
                            <td align=\"center\">
                                <table role=\"presentation\" width=\"620\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:620px;background:#ffffff;border:1px solid #dbe5dc;border-radius:10px;overflow:hidden;\">
                                    <tr>
                                        <td style=\"background:#1f5c3a;color:#ffffff;padding:16px 24px;font-size:20px;font-weight:700;\">GreenSure</td>
                                    </tr>
                                    <tr>
                                        <td style=\"padding:24px 24px 16px 24px;line-height:1.6;font-size:15px;\">%s</td>
                                    </tr>
                                    <tr>
                                        <td style=\"padding:14px 24px 20px 24px;border-top:1px solid #e5e7eb;color:#6b7280;font-size:12px;\">GreenSure Platform</td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </div>
                """
                .formatted(body);
    }
}
