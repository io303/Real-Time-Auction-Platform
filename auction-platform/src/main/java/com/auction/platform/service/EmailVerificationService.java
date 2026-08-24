
        package com.auction.platform.service;

import com.auction.platform.entity.User;

public interface EmailVerificationService {

    void sendVerification(User user);

    void verify(String rawToken);

    void resendVerification(String email);
}

