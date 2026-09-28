package com.foodrescue.authservice.controller;

import com.foodrescue.authservice.dto.AuthResponse;
import com.foodrescue.authservice.dto.LoginRequest;
import com.foodrescue.authservice.dto.MfaSetupResponse;
import com.foodrescue.authservice.dto.MfaValidateRequest;
import com.foodrescue.authservice.dto.MfaVerifyRequest;
import com.foodrescue.authservice.dto.RegisterRequest;
import com.foodrescue.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ==========================================
    // AJOUT MFA : Endpoints MFA
    // ==========================================

    /**
     * Étape 1 (activation) : Génère un secret TOTP + QR code pour l'utilisateur connecté.
     * L'utilisateur scanne le QR avec Google Authenticator.
     * Route protégée : nécessite un token JWT valide.
     */
    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> mfaSetup(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(authService.mfaSetup(email));
    }

    /**
     * Étape 2 (activation) : Vérifie le premier code saisi par l'utilisateur.
     * Si le code est correct, le MFA est définitivement activé.
     * Route protégée : nécessite un token JWT valide.
     */
    @PostMapping("/mfa/verify")
    public ResponseEntity<Void> mfaVerify(
            Authentication authentication,
            @RequestBody MfaVerifyRequest request
    ) {
        String email = authentication.getName();
        authService.mfaVerify(email, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Étape 3 (connexion) : Valide le partialToken + code MFA pour finaliser la connexion.
     * Route publique : utilise le partialToken (pas de JWT complet nécessaire).
     */
    @PostMapping("/mfa/validate")
    public ResponseEntity<AuthResponse> mfaValidate(@RequestBody MfaValidateRequest request) {
        return ResponseEntity.ok(authService.mfaValidate(request));
    }
}