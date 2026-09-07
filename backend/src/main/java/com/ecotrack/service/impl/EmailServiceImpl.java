package com.ecotrack.service.impl;

import com.ecotrack.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Autowired
    public EmailServiceImpl(@Lazy @Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String token) {
        if (mailSender == null) {
            System.err.println("JavaMailSender is not configured. Email to " + to + " skipped.");
            return;
        }

        String resetUrl = frontendUrl + "/auth/reset-password?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("ecotrackcarbon.support@gmail.com");
            helper.setTo(to);
            helper.setSubject("EcoTrack - Password Reset Request");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; padding: 30px; }
                        .header { text-align: center; margin-bottom: 20px; }
                        .header h1 { color: #2e7d32; margin: 0; }
                        .content { color: #333; line-height: 1.6; }
                        .btn { display: inline-block; background-color: #2e7d32; color: #ffffff !important; text-decoration: none; padding: 12px 30px; border-radius: 5px; margin: 20px 0; font-weight: bold; }
                        .footer { text-align: center; margin-top: 20px; color: #888; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>EcoTrack</h1>
                        </div>
                        <div class="content">
                            <h2>Password Reset Request</h2>
                            <p>We received a request to reset your password. Click the button below to set a new password:</p>
                            <p style="text-align: center;">
                                <a href="%s" class="btn">Reset My Password</a>
                            </p>
                            <p>This link will expire in <strong>1 hour</strong>.</p>
                            <p>If you did not request a password reset, please ignore this email. Your password will remain unchanged.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 EcoTrack. Carbon Footprint & Sustainability Platform.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resetUrl);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}