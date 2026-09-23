package com.foodrescue.reservationservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        System.out.println("DEBUG - Auth Header reçu : " + authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("DEBUG - Pas de header Authorization ou format invalide.");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtService.extractUsername(jwt);
            final List<SimpleGrantedAuthority> authorities = jwtService.extractAuthorities(jwt);
            System.out.println("DEBUG - Email extrait : " + userEmail + " | Autorités : " + authorities);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null
                    && !jwtService.isTokenExpired(jwt)) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail,
                        null,
                        authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("DEBUG - Utilisateur authentifié avec succès dans le SecurityContext !");
            }
        } catch (Exception e) {
            System.err.println("DEBUG - Erreur JWT : " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}