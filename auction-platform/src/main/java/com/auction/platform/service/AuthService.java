package com.auction.platform.service;

import com.auction.platform.dto.request.LoginRequest;
import com.auction.platform.dto.request.RefreshTokenRequest;
import com.auction.platform.dto.request.RegisterRequest;
import com.auction.platform.dto.response.AuthResponse;
import com.auction.platform.dto.response.MessageResponse;

public interface AuthService {
    MessageResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
}
