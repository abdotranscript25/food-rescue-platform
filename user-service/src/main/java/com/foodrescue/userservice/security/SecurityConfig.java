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
                        // 1. Autoriser la création initiale de profil (lors du register dans auth-service ou appel interne)
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // 2. Consultation/lecture des profils par d'autres services ou par l'utilisateur authentifié
                        .requestMatchers(HttpMethod.GET, "/api/users/**").authenticated()

                        // 3. Modification de profil nécessitant d'être authentifié
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").authenticated()

                        // 4. Seul un ADMIN peut supprimer un profil utilisateur
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")

                        // 5. Actuator pour le monitoring
                        .requestMatchers("/actuator/**").permitAll()

                        // Toutes les autres requêtes nécessitent d'être authentifié
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}