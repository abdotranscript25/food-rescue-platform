package com.foodrescue.offerservice.security;

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
                        // 1. Route publique : consulter les offres (GET)
                        .requestMatchers(HttpMethod.GET, "/api/offers/**").permitAll()

                        // 2. Route interne : décrémentation du stock (utilisation de * pour l'ID)
                        .requestMatchers(HttpMethod.PUT, "/api/offers/*/decrement").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/offers/*/increment").permitAll()

                        // 3. Routes réservées aux MERCHANT ou ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/offers/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/offers/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/offers/**").hasAuthority("ROLE_ADMIN")

                        // 4. Routes actuator (monitoring)
                        .requestMatchers("/actuator/**").permitAll()

                        // 5. Toutes les autres routes nécessitent un token
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}