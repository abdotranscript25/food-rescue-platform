package com.foodrescue.productservice.config;

import com.foodrescue.productservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. GET : public
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // 2. POST/PUT : MERCHANT ou ADMIN uniquement
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyAuthority("ROLE_MERCHANT", "ROLE_ADMIN")

                        // 3. DELETE : ADMIN uniquement
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("ROLE_ADMIN")

                        // 4. Actuator
                        .requestMatchers("/actuator/**").permitAll()

                        // 5. Tout le reste nécessite un token
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}