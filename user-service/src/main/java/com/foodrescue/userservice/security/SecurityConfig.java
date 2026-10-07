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
                        // 1. Création initiale de profil (appelée par auth-service)
                        .requestMatchers(HttpMethod.POST, "/api/users/profiles").permitAll()

                        // 2. Lecture des profils : réservée aux rôles authentifiés (avec ou sans préfixe ROLE_)
                        .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyAuthority(
                                "ROLE_CONSUMER", "ROLE_MERCHANT", "ROLE_ADMIN",
                                "CONSUMER", "MERCHANT", "ADMIN"
                        )

                        // 3. Modification de profil : sécurisée par authentification
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()

                        // 4. Suppression : ADMIN uniquement
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // 5. Actuator (monitoring)
                        .requestMatchers("/actuator/**").permitAll()

                        // 6. Tout le reste nécessite d'être authentifié
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}