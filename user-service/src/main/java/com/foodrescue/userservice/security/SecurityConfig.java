package com.foodrescue.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // ⚠️ AJOUT : Autoriser /error pour ne pas masquer les erreurs 500 en 403
                        .requestMatchers("/error").permitAll()

                        // 1. Autoriser la création de profil (appel Feign de auth-service)
                        .requestMatchers(HttpMethod.POST, "/api/users/profiles", "/api/users/profiles/**").permitAll()

                        // 2. Lecture des profils
                        .requestMatchers(HttpMethod.GET, "/api/users/**", "/api/users/profiles/**").hasAnyAuthority(
                                "ROLE_CONSUMER", "ROLE_MERCHANT", "ROLE_ADMIN",
                                "CONSUMER", "MERCHANT", "ADMIN"
                        )

                        // 3. Modification
                        .requestMatchers(HttpMethod.PUT, "/api/users/**", "/api/users/profiles/**").authenticated()

                        // 4. Suppression
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**", "/api/users/profiles/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // 5. Actuator
                        .requestMatchers("/actuator/**").permitAll()

                        // 6. Tout le reste
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}