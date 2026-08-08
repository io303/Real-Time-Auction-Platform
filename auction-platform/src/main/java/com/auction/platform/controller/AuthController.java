package com.auction.platform.controller;

import com.auction.platform.dto.request.ForgotPasswordRequest;
import com.auction.platform.dto.request.LoginRequest;
import com.auction.platform.dto.request.RefreshTokenRequest;
import com.auction.platform.dto.request.RegisterRequest;
import com.auction.platform.dto.request.ResetPasswordRequest;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.AuthResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.service.AuthService;
import com.auction.platform.service.EmailVerificationService;
import com.auction.platform.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, tokens, verification, password reset")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user (defaults to Buyer role, requires email verification)")
    public ResponseEntity<ApiResponse<MessageResponse>> register(@Valid @RequestBody RegisterRequest request) {
        MessageResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Registered", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive access + refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Exchange a valid refresh token for a new access + refresh token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh token, ending that session")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out", MessageResponse.of("Session ended")));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email using the token sent by email")
    public ResponseEntity<ApiResponse<MessageResponse>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified",
                MessageResponse.of("Your email has been verified. You can now log in.")));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset email")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiateReset(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Request received",
                MessageResponse.of("If that email is registered, a reset link has been sent.")));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the token sent by email")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset",
                MessageResponse.of("Your password has been reset. Please log in with your new password.")));
    }
}
