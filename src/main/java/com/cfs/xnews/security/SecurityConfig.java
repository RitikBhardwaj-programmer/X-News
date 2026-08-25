package com.cfs.xnews.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter =
                jwtAuthenticationFilter;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                .csrf(csrf ->
                        csrf.disable()
                )

                .cors(cors -> {})

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )


                .authorizeHttpRequests(auth -> auth


                        // =========================================
                        // PUBLIC
                        // =========================================

                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()


                        // =========================================
                        // USER + ADMIN
                        // =========================================

                        // Current logged-in user
                        .requestMatchers(
                                "/api/v1/users/me"
                        ).authenticated()


                        // View articles
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/articles",
                                "/api/v1/articles/**"
                        ).authenticated()


                        // View sources
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/sources",
                                "/api/v1/sources/**"
                        ).authenticated()


                        // View events
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/events",
                                "/api/v1/events/**"
                        ).authenticated()


                        // Event analysis
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/events/*/analyze"
                        ).authenticated()


                        // Fact checking
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/events/*/fact-checks"
                        ).authenticated()


                        // =========================================
                        // ADMIN ONLY
                        // =========================================

                        // Create article
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/articles"
                        ).hasRole("ADMIN")


                        // Delete article
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/articles",
                                "/api/v1/articles/**"
                        ).hasRole("ADMIN")


                        // Create source
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/sources"
                        ).hasRole("ADMIN")


                        // Enable / disable source
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/sources/**"
                        ).hasRole("ADMIN")


                        // Collect / fetch source
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/sources/*/collect"
                        ).hasRole("ADMIN")


                        // Delete events
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/events",
                                "/api/v1/events/**"
                        ).hasRole("ADMIN")


                        // =========================================
                        // INTERNAL / TEST
                        // =========================================

                        // Do not expose AI test endpoint
                        .requestMatchers(
                                "/test-ai/**",
                                "/embed",
                                "/predict"
                        ).denyAll()


                        // =========================================
                        // EVERYTHING ELSE
                        // =========================================

                        .anyRequest().authenticated()
                )


                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();


        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:5173",
                        "http://localhost:4173",
                        "https://lemon-pond-082981600.7.azurestaticapps.net"
                )
        );


        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );


        configuration.setAllowedHeaders(
                List.of("*")
        );


        configuration.setAllowCredentials(true);


        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();


        source.registerCorsConfiguration(
                "/**",
                configuration
        );


        return source;
    }
}