package com.auction.platform.config;

import com.auction.platform.security.jwt.JwtAuthenticationFilter;
import com.auction.platform.security.userdetails.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;

    // =========================================================
    // PASSWORD ENCODER
    // =========================================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // =========================================================
    // AUTHENTICATION PROVIDER
    // =========================================================

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    // =========================================================
    // AUTHENTICATION MANAGER
    // =========================================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();
    }

    // =========================================================
    // CORS CONFIGURATION
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        // -----------------------------------------------------
        // Allowed Frontend Origins
        // -----------------------------------------------------

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3002",
                "http://localhost:5173",
                "https://bidly-frontend-8ysq.onrender.com"
        ));

        // -----------------------------------------------------
        // Allowed HTTP Methods
        // -----------------------------------------------------

        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));

        // -----------------------------------------------------
        // Allowed Headers
        // -----------------------------------------------------

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // -----------------------------------------------------
        // Headers exposed to frontend
        // -----------------------------------------------------

        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        // -----------------------------------------------------
        // Allow cookies / credentials
        // -----------------------------------------------------

        configuration.setAllowCredentials(true);

        // -----------------------------------------------------
        // Apply CORS configuration to all endpoints
        // -----------------------------------------------------

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    // =========================================================
    // SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

                // -------------------------------------------------
                // CSRF
                // JWT based API -> CSRF disabled
                // -------------------------------------------------

                .csrf(csrf -> csrf.disable())

                // -------------------------------------------------
                // CORS
                // -------------------------------------------------

                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource()
                        )
                )

                // -------------------------------------------------
                // SESSION MANAGEMENT
                // -------------------------------------------------

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // -------------------------------------------------
                // AUTHORIZATION RULES
                // -------------------------------------------------

                .authorizeHttpRequests(auth -> auth

                        // =================================================
                        // CORS PREFLIGHT
                        // =================================================

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // =================================================
                        // PUBLIC AUTH APIs
                        // =================================================

                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh-token",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/verify-email",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password"
                        ).permitAll()

                        // =================================================
                        // PUBLIC STATIC FILES / WEBSOCKET / SWAGGER
                        // =================================================

                        .requestMatchers(
                                "/uploads/**",
                                "/ws/**",
                                "/websocket-test.html",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml"
                        ).permitAll()

                        // =================================================
                        // PAYMENT WEBHOOK
                        // =================================================

                        .requestMatchers(
                                "/api/v1/payments/webhook"
                        ).permitAll()

                        // =================================================
                        // PUBLIC CATEGORIES
                        // =================================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/categories",
                                "/api/v1/categories/**"
                        ).permitAll()

                        // =================================================
                        // PUBLIC AUCTIONS
                        // =================================================

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auctions",
                                "/api/v1/auctions/**"
                        ).permitAll()

                        // =================================================
                        // USER'S OWN AUCTIONS
                        // =================================================

                        .requestMatchers(
                                "/api/v1/auctions/mine"
                        ).authenticated()

                        // =================================================
                        // ADMIN APIs
                        // =================================================

                        .requestMatchers(
                                "/api/v1/admin/**"
                        ).hasRole("ADMIN")

                        // =================================================
                        // EVERYTHING ELSE
                        // =================================================

                        .anyRequest().authenticated()
                )

                // -------------------------------------------------
                // Authentication Provider
                // -------------------------------------------------

                .authenticationProvider(
                        authenticationProvider()
                )

                // -------------------------------------------------
                // JWT FILTER
                // -------------------------------------------------

                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}