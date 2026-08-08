package com.auction.platform.dto.response;

import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private boolean emailVerified;
    private Set<String> roles;
    private List<AddressResponse> addresses;
}
