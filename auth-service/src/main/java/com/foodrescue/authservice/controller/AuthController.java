package com.foodrescue.authservice.controller;

import com.foodrescue.authservice.dto.AuthResponse;
import com.foodrescue.authservice.dto.LoginRequest;
import com.foodrescue.authservice.dto.MfaSetupResponse;
import com.foodrescue.authservice.dto.MfaValidateRequest;
import com.foodrescue.authservice.dto.MfaVerifyRequest;
import com.foodrescue.authservice.dto.RegisterRequest;
import com.foodrescue.authservice.entity.User;
import com.foodrescue.authservice.repository.UserRepository;
import com.foodrescue.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. On récupère la réponse normale (qui contient le token ou l'état MFA)
        AuthResponse authResponse = authService.login(request);

        // Si l'utilisateur a besoin du MFA, on ne met pas encore le cookie final, on renvoie la réponse classique
        if (authResponse.isMfaRequired()) {
            return ResponseEntity.ok(authResponse);
        }

        // 2. Si le login est direct et validé, on crée le Cookie HttpOnly sécurisé
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", authResponse.getToken())
                .httpOnly(true)    // Inaccessible par JavaScript (protection XSS)
                .secure(false)     // Mettre à TRUE en production (HTTPS obligatoires), FALSE en local (HTTP)
                .path("/")         // Valide pour toute l'application
                .maxAge(24 * 60 * 60) // Durée de vie : 24 heures
                .sameSite("Lax") // Protection CSRF
                .build();

        // 3. On retourne la réponse en glissant le cookie dans les Headers
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(authResponse); // Ou simplement un message de succès si vous ne voulez plus exposer le token en JSON
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));
        return ResponseEntity.ok(user);
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> mfaSetup(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(authService.mfaSetup(email));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<Void> mfaVerify(
            Authentication authentication,
            @RequestBody MfaVerifyRequest request
    ) {
        String email = authentication.getName();
        authService.mfaVerify(email, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mfa/validate")
    public ResponseEntity<?> mfaValidate(@RequestBody MfaValidateRequest request) {
        AuthResponse authResponse = authService.mfaValidate(request);

        // De même pour la validation MFA finale : on sécurise le token dans un Cookie HttpOnly
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", authResponse.getToken())
                .httpOnly(true)
                .secure(false) // true en production (HTTPS)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(authResponse);
    }
}