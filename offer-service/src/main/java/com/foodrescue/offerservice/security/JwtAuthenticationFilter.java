package com.foodrescue.offerservice.security;

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

        // 1. Récupérer le header Authorization
        final String authHeader = request.getHeader("Authorization");

        // 2. Si pas de header valide, on passe au filtre suivant
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraire le token
        final String jwt = authHeader.substring(7);

        try {
            // 4. Extraire email + rôles du token
            final String userEmail = jwtService.extractUsername(jwt);
            final List<SimpleGrantedAuthority> authorities = jwtService.extractAuthorities(jwt);

            // 5. Si l'utilisateur n'est pas encore authentifié, on l'authentifie
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null
                    && !jwtService.isTokenExpired(jwt)) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userEmail,
                        null,
                        authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token invalide : on laisse passer sans authentifier (Spring Security renverra 401/403)
            logger.warn("Token JWT invalide : " + e.getMessage());
        }

        // 6. Passer au filtre suivant
        filterChain.doFilter(request, response);
    }
}