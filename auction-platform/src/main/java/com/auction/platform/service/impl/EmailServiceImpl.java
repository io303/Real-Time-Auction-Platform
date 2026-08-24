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

        String link = frontendBaseUrl
                + "/verify-email?token="
                + rawToken;

        send(
                toEmail,
                "Verify your email",
                "Welcome to Bidly!\n\n"
                        + "Please verify your email by visiting:\n"
                        + link
        );

        log.info(
                "Verification link for {}: {}",
                toEmail,
                link
        );
    }

    @Override
    public void sendPasswordResetEmail(
            String toEmail,
            String rawToken
    ) {

        String link = frontendBaseUrl
                + "/reset-password?token="
                + rawToken;

        send(
                toEmail,
                "Reset your password",
                "We received a request to reset your password.\n\n"
                        + "Visit the following link to reset your password:\n"
                        + link
                        + "\n\n"
                        + "If you didn't request this, please ignore this email."
        );

        log.info(
                "Password reset link for {}: {}",
                toEmail,
                link
        );
    }

    @Override
    public void sendNotificationEmail(
            String toEmail,
            String subject,
            String body
    ) {
        send(toEmail, subject, body);
    }

    private void send(
            String to,
            String subject,
            String body
    ) {

        try {

            SimpleMailMessage message =
                    new SimpleMailMessage();

            message.setFrom("srahul12768@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            log.info("========== EMAIL SENDING ==========");
            log.info("FROM: srahul12768@gmail.com");
            log.info("TO: {}", to);
            log.info("SUBJECT: {}", subject);

            mailSender.send(message);

            log.info(
                    "========== EMAIL SENT SUCCESSFULLY =========="
            );

        } catch (Exception e) {

            log.error(
                    "========== EMAIL FAILED ==========",
                    e
            );

            // IMPORTANT:
            // Do not throw the exception.
            // Registration/password reset should not fail
            // just because the email provider is unavailable.
        }
    }
}