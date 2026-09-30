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
                        // CORRECTION : ROLE_USER → ROLE_CONSUMER
                        .requestMatchers(HttpMethod.POST, "/api/reservations/**")
                        .hasAnyAuthority("ROLE_CONSUMER", "ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/**")
                        .hasAnyAuthority("ROLE_CONSUMER", "ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reservations/**")
                        .hasAnyAuthority("ROLE_CONSUMER", "ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/**")
                        .hasAuthority("ROLE_ADMIN")

                        // Actuator
                        .requestMatchers("/actuator/**").permitAll()

                        // Tout le reste
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}