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
            message.setFrom("srahul12768@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            System.out.println("========== EMAIL SENDING ==========");
            System.out.println("FROM: " + "srahul12768@gmail.com");
            System.out.println("TO: " + to);
            System.out.println("SUBJECT: " + subject);

            mailSender.send(message);

            System.out.println("========== EMAIL SENT SUCCESSFULLY ==========");

        } catch (Exception e) {
            System.out.println("========== EMAIL FAILED ==========");
            e.printStackTrace();
        }
    }
}
