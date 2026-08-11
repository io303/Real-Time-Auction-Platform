package com.auction.platform.service.impl;

import com.auction.platform.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void sendVerificationEmail(String toEmail, String rawToken) {
        String link = frontendBaseUrl + "/verify-email?token=" + rawToken;
        send(toEmail, "Verify your email",
                "Welcome! Please verify your email by visiting: " + link);
        log.info("Verification link for {} (dev visibility): {}", toEmail, link);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String link = frontendBaseUrl + "/reset-password?token=" + rawToken;
        send(toEmail, "Reset your password",
                "We received a request to reset your password. Visit: " + link
                        + " — if you didn't request this, ignore this email.");
        log.info("Password reset link for {} (dev visibility): {}", toEmail, link);
    }

    @Override
    public void sendNotificationEmail(String toEmail, String subject, String body) {
        send(toEmail, subject, body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Deliberately fail soft: no SMTP configured in dev shouldn't 500 the request.
            // In production with real SMTP creds, this catch block is what you'd wire an alert to.
            log.warn("Failed to send email to {} — SMTP likely not configured. Reason: {}", to, e.getMessage());
        }
    }
}
