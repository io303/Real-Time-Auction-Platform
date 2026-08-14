package com.auction.platform.security.userdetails;

import java.util.List;

/**
 * Lightweight, Redis-safe projection of a User — never a raw JPA entity.
 *
 * Roles are cached as (id, name) pairs, not just names — this lets us reconstruct real Role
 * references (with valid database IDs) on a cache hit, so the resulting User object behaves
 * correctly if it's later passed to userRepository.save() (e.g. from ProfileServiceImpl).
 * Caching only role names would produce id-less Role objects that could corrupt the
 * user_roles join table on save.
 */
public record CachedUserAuth(
        Long id,
        String fullName,
        String email,
        String password,
        String phoneNumber,
        String profileImageUrl,
        boolean enabled,
        boolean accountNonLocked,
        boolean emailVerified,
        List<CachedRole> roles
) {
    public record CachedRole(Long id, String name) {}
}
