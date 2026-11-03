package com.foodrescue.authservice.controller;

import com.foodrescue.authservice.dto.*;
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
        AuthResponse authResponse = authService.login(request);

        if (authResponse.isMfaRequired()) {
            return ResponseEntity.ok(authResponse);
        }

        ResponseCookie jwtCookie = ResponseCookie.from("jwt", authResponse.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(authResponse);
    }

    // ==========================================
    // CORRECTION SÉCURITÉ : Renvoie un DTO au lieu de l'Entité
    // ==========================================
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        return ResponseEntity.ok(mapToDto(user));
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

        ResponseCookie jwtCookie = ResponseCookie.from("jwt", authResponse.getToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(authResponse);
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(
            Authentication authentication,
            @RequestBody UpdatePasswordRequest request
    ) {
        String email = authentication.getName();
        authService.updatePassword(email, request);
        return ResponseEntity.ok().body("Mot de passe mis à jour avec succès.");
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());

        userRepository.save(user);
        return ResponseEntity.ok().body("Profil mis à jour dans la base Auth avec succès.");
    }

    // ==========================================
    // MÉTHODE UTILITAIRE : Mapping Entity -> DTO
    // ==========================================
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .mfaEnabled(user.isMfaEnabled())
                .build();
    }
}