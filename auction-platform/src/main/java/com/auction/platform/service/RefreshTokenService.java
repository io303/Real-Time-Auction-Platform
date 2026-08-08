package com.auction.platform.service;

import com.auction.platform.entity.User;

public interface RefreshTokenService {
    String issueRefreshToken(User user);
    RotatedToken rotate(String rawRefreshToken);
    void revoke(String rawRefreshToken);
    void revokeAllForUser(User user);

    /** Result of a rotation: the user the token belonged to, and the new raw token for the client. */
    record RotatedToken(User user, String newRawRefreshToken) {}
}
