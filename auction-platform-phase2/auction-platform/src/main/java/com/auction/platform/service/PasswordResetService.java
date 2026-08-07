package com.auction.platform.service;

public interface PasswordResetService {
    void initiateReset(String email);
    void resetPassword(String rawToken, String newPassword);
}
