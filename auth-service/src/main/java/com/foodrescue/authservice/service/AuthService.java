package com.foodrescue.authservice.service;

import com.foodrescue.authservice.client.UserServiceClient;
import com.foodrescue.authservice.dto.AuthResponse;
import com.foodrescue.authservice.dto.LoginRequest;
import com.foodrescue.authservice.dto.MfaSetupResponse;
import com.foodrescue.authservice.dto.MfaValidateRequest;
import com.foodrescue.authservice.dto.MfaVerifyRequest;
import com.foodrescue.authservice.dto.RegisterRequest;
import com.foodrescue.authservice.dto.UserProfileRequest;
import com.foodrescue.authservice.entity.Role;
import com.foodrescue.authservice.entity.User;
import com.foodrescue.authservice.entity.UserStatus;
import com.foodrescue.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserServiceClient userServiceClient;
    private final MfaService mfaService;

    // ==========================================
    // Inscription
    // ==========================================
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà.");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole() != null ? request.getRole() : Role.CONSUMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .mfaEnabled(false) // Sécurité explicite
                .build();

        User savedUser = userRepository.save(user);

        try {
            UserProfileRequest profileRequest = UserProfileRequest.builder()
                    .id(savedUser.getId())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .email(savedUser.getEmail())
                    .phone(savedUser.getPhone())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .build();

            userServiceClient.createProfile(profileRequest);
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du profil dans user-service : " + e.getMessage());
        }

        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .mfaRequired(false)
                .build();
    }

    // ==========================================
    // Connexion
    // ==========================================
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (user.isMfaEnabled()) {
            String partialToken = jwtService.generatePartialToken(user.getEmail());
            return AuthResponse.builder()
                    .partialToken(partialToken)
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .mfaRequired(true)
                    .build();
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mfaRequired(false)
                .build();
    }

    // ==========================================
    // MFA Setup (Sécurisé contre le ré-enrôlement)
    // ==========================================
    public MfaSetupResponse mfaSetup(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // SÉCURITÉ : Empêcher l'écrasement d'un secret si la MFA est déjà active
        if (user.isMfaEnabled()) {
            throw new RuntimeException("La MFA est déjà active sur ce compte. Désactivez-la d'abord pour la reconfigurer.");
        }

        String secret = mfaService.generateNewSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrCodeImage = mfaService.generateQrCodeImage(secret, user.getEmail());
        String otpauthUri = mfaService.generateQrCodeUri(secret, user.getEmail());

        return MfaSetupResponse.builder()
                .qrCodeImage(qrCodeImage)
                .otpauthUri(otpauthUri)
                .secret(secret)
                .build();
    }

    // ==========================================
    // MFA Verify (Validation initiale du setup)
    // ==========================================
    public void mfaVerify(String email, MfaVerifyRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new RuntimeException("Le MFA n'a pas été initialisé. Appelez /mfa/setup d'abord.");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new RuntimeException("Code MFA invalide.");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    // ==========================================
    // MFA Validate (Validation lors du Login + Anti-Rejeu)
    // ==========================================
    public AuthResponse mfaValidate(MfaValidateRequest request) {
        if (!jwtService.isPartialToken(request.getPartialToken())) {
            throw new RuntimeException("Token partiel invalide.");
        }

        if (jwtService.isTokenExpired(request.getPartialToken())) {
            throw new RuntimeException("Token partiel expiré. Reconnectez-vous.");
        }

        String email = jwtService.extractUsername(request.getPartialToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new RuntimeException("Le MFA n'est pas activé pour cet utilisateur.");
        }

        // Vérification du code TOTP
        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new RuntimeException("Code MFA invalide.");
        }

        // SÉCURITÉ ANTI-REJEU : Empêcher la réutilisation immédiate du même code exact
        long currentWindow = System.currentTimeMillis() / 30000; // Fenêtre de 30 secondes
        if (user.getLastUsedOtpWindow() != null && user.getLastUsedOtpWindow() == currentWindow) {
            throw new RuntimeException("Ce code MFA a déjà été utilisé. Veuillez patienter pour le suivant.");
        }
        user.setLastUsedOtpWindow(currentWindow);
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mfaRequired(false)
                .build();
    }
}