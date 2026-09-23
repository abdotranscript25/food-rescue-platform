package com.foodrescue.authservice.security;

import com.foodrescue.authservice.service.CustomUserDetailsService;
import com.foodrescue.authservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Récupérer le header "Authorization"
        final String authHeader = request.getHeader("Authorization");

        // 2. Si pas de header ou pas de "Bearer ", on passe au filtre suivant
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraire le token (retirer "Bearer ")
        final String jwt = authHeader.substring(7);
        final String userEmail;

        try {
            // 4. Extraire l'email depuis le token
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token invalide ou expiré : on laisse passer sans authentifier
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Si on a un email et que l'utilisateur n'est pas déjà authentifié
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Charger l'utilisateur depuis la base
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 7. Vérifier que le token est valide pour cet utilisateur
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // 8. Créer un objet d'authentification
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 9. Mettre à jour le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 10. Passer au filtre suivant
        filterChain.doFilter(request, response);
    }
}