package com.auction.platform.dto.response;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String fullName;
    private String email;
    private Set<String> roles;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
}
