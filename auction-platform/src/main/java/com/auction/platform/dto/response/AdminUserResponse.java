package com.auction.platform.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Set<String> roles;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
