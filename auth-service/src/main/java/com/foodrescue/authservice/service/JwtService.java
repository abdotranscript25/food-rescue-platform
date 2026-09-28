package com.foodrescue.authservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.partial-expiration}")  // AJOUT MFA
    private long partialJwtExpiration;   // AJOUT MFA

    // ==========================================
    // 1. Génération du token
    // ==========================================
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Convertir les autorités en liste de String (ex: ["ROLE_USER"])
        extraClaims.put("roles", userDetails.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList()));
        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    // ==========================================
    // AJOUT MFA : Génération du token partiel
    // ==========================================
    /**
     * Génère un token partiel valide 5 minutes.
     * Ce token contient uniquement :
     *  - L'email de l'utilisateur (subject)
     *  - Le claim spécial "mfa_pending": true
     * Il ne contient PAS les rôles.
     * Il sert uniquement à l'étape de validation MFA.
     */
    public String generatePartialToken(String email) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("mfa_pending", true);   // Marqueur clé
        return Jwts.builder()
                .claims(extraClaims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + partialJwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    // ==========================================
    // AJOUT MFA : Vérification du token partiel
    // ==========================================
    /**
     * Vérifie si un token est un partial token MFA.
     * @return true si le claim "mfa_pending" vaut true.
     */
    public boolean isPartialToken(String token) {
        try {
            Boolean mfaPending = extractClaim(token, claims -> claims.get("mfa_pending", Boolean.class));
            return Boolean.TRUE.equals(mfaPending);
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================
    // 2. Validation du token
    // ==========================================
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ==========================================
    // 3. Extraction des informations
    // ==========================================
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ==========================================
    // 4. Clé de signature
    // ==========================================
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}