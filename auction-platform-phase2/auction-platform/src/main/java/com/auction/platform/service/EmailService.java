package com.auction.platform.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String rawToken);
    void sendPasswordResetEmail(String toEmail, String rawToken);
}
