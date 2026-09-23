package com.foodrescue.reservationservice.security;

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
                        // Utilisation de hasAnyAuthority / hasAuthority pour correspondre exactement aux rôles du JWT (avec le préfixe ROLE_)
                        .requestMatchers(HttpMethod.POST, "/api/reservations/**").hasAnyAuthority("ROLE_USER", "ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/**").hasAnyAuthority("ROLE_USER", "ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/**").hasAnyAuthority("ROLE_USER", "ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**").hasAuthority("ROLE_ADMIN")

                        // Actuator (monitoring)
                        .requestMatchers("/actuator/**").permitAll()

                        // Toutes les autres routes nécessitent d'être authentifié
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}