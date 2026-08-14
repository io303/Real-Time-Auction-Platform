package com.auction.platform.security.userdetails;

import com.auction.platform.entity.Role;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.RoleType;
import com.auction.platform.repository.UserRepository;
import com.auction.platform.security.userdetails.CachedUserAuth.CachedRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Caches the auth-relevant projection of a User in Redis (5-minute TTL), since
 * JwtAuthenticationFilter calls loadUserByUsername() on EVERY authenticated request.
 *
 * IMPORTANT: both the cache-hit and cache-miss paths return the SAME type (CustomUserDetails),
 * always wrapping a usable User object — never a bare Spring User. An earlier draft of this
 * caching layer returned different types on each path, which silently broke every
 * @AuthenticationPrincipal CustomUserDetails controller parameter once the cache was warm
 * (Spring injects null instead of throwing, since the actual object wasn't assignable — this
 * would have surfaced as random NullPointerExceptions well after code review). Keeping both
 * paths type-consistent is the single most important property of this class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private static final String KEY_PREFIX = "auth:user:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        CachedUserAuth cached = getFromCache(email);
        if (cached != null) {
            return new CustomUserDetails(reconstruct(cached));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        putInCache(toCachedUserAuth(user));
        return new CustomUserDetails(user);
    }

    /** Called whenever password, enabled status, or roles change for a user. */
    public void evictCache(String email) {
        try {
            redisTemplate.delete(KEY_PREFIX + email);
        } catch (Exception e) {
            log.warn("Failed to evict auth cache for {}: {}", email, e.getMessage());
        }
    }

    private CachedUserAuth getFromCache(String email) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + email);
            return json != null ? objectMapper.readValue(json, CachedUserAuth.class) : null;
        } catch (Exception e) {
            log.warn("Auth cache read failed for {}: {}", email, e.getMessage());
            return null;
        }
    }

    private void putInCache(CachedUserAuth auth) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + auth.email(), objectMapper.writeValueAsString(auth), TTL);
        } catch (Exception e) {
            log.warn("Auth cache write failed for {}: {}", auth.email(), e.getMessage());
        }
    }

    private CachedUserAuth toCachedUserAuth(User user) {
        return new CachedUserAuth(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPassword(),
                user.getPhoneNumber(),
                user.getProfileImageUrl(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.isEmailVerified(),
                user.getRoles().stream()
                        .map(r -> new CachedRole(r.getId(), r.getName().name()))
                        .collect(Collectors.toList())
        );
    }

    /** Rebuilds a detached-but-usable User from cached data, with real Role IDs preserved. */
    private User reconstruct(CachedUserAuth cached) {
        Set<Role> roles = new HashSet<>();
        for (CachedRole cr : cached.roles()) {
            roles.add(Role.builder().id(cr.id()).name(RoleType.valueOf(cr.name())).build());
        }

        return User.builder()
                .id(cached.id())
                .fullName(cached.fullName())
                .email(cached.email())
                .password(cached.password())
                .phoneNumber(cached.phoneNumber())
                .profileImageUrl(cached.profileImageUrl())
                .enabled(cached.enabled())
                .accountNonLocked(cached.accountNonLocked())
                .emailVerified(cached.emailVerified())
                .roles(roles)
                .build();
    }
}
