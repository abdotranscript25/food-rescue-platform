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
                        // 1. Consultation publique des offres
                        .requestMatchers(HttpMethod.GET, "/api/offers/**").permitAll()

                        // 2. Endpoints internes (appelés par reservation-service)
                        .requestMatchers(HttpMethod.PUT, "/api/offers/*/decrement").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/offers/*/increment").permitAll()

                        // 3. Publication et annulation : MERCHANT ou ADMIN
                        .requestMatchers(HttpMethod.PUT, "/api/offers/*/publish").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/offers/*/cancel").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")

                        // 4. CRUD (Création, Modification, Suppression) : MERCHANT ou ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/offers/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/offers/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/offers/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")

                        // 5. Actuator (monitoring)
                        .requestMatchers("/actuator/**").permitAll()

                        // 6. Tout le reste nécessite un token
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}