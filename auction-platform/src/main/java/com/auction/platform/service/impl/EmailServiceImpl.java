
        package com.auction.platform.service.impl;

import com.auction.platform.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.resend.com")
            .build();

    private static final String FROM_EMAIL = "Bidly <onboarding@resend.dev>";

    @Override
    public void sendVerificationEmail(String toEmail, String rawToken) {

        String link = frontendBaseUrl
                + "/verify-email?token="
                + rawToken;

        send(
                toEmail,
                "Verify your Bidly account",
                """
                Welcome to Bidly!

                Please verify your email address by clicking the link below:

                %s

                This verification link will expire in 24 hours.

                If you did not create this account, you can safely ignore this email.

                Regards,
                Bidly Team
                """.formatted(link)
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
                "Reset your Bidly password",
                """
                Hello,

                We received a request to reset your Bidly password.

                Click the link below to reset your password:

                %s

                This link will expire in 15 minutes.

                If you did not request a password reset, please ignore this email.

                Regards,
                Bidly Team
                """.formatted(link)
        );

        log.info(
                "Password reset link generated for {}",
                toEmail
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
            String toEmail,
            String subject,
            String body
    ) {

        log.info("========== EMAIL SENDING ==========");
        log.info("TO: {}", toEmail);
        log.info("SUBJECT: {}", subject);

        try {

            Map<String, Object> requestBody = Map.of(
                    "from", FROM_EMAIL,
                    "to", new String[]{toEmail},
                    "subject", subject,
                    "text", body
            );

            restClient.post()
                    .uri("/emails")
                    .header(
                            "Authorization",
                            "Bearer " + resendApiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info(
                    "========== EMAIL SENT SUCCESSFULLY =========="
            );

        } catch (Exception e) {

            log.error(
                    "========== EMAIL FAILED ==========",
                    e
            );

            throw new RuntimeException(
                    "Failed to send email",
                    e
            );
        }
    }
}

