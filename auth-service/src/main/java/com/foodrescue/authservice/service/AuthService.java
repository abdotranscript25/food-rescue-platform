package com.foodrescue.authservice.service;

import com.foodrescue.authservice.client.UserServiceClient;
import com.foodrescue.authservice.dto.*;
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
                .mfaEnabled(false)
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
                    .role(savedUser.getRole().name()) // 💡 Ajout indispensable pour transmettre le rôle au user-service
                    .build();

            userServiceClient.createProfile(profileRequest);
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du profil dans user-service : " + e.getMessage());
        }

        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .id(savedUser.getId())
                .token(token)
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .mfaRequired(false)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (user.isMfaEnabled()) {
            String partialToken = jwtService.generatePartialToken(user.getEmail());
            return AuthResponse.builder()
                    .id(user.getId())
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
                .id(user.getId())
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mfaRequired(false)
                .build();
    }

    public MfaSetupResponse mfaSetup(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (user.isMfaEnabled()) {
            throw new RuntimeException("La MFA est déjà active sur ce compte.");
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

    public void mfaVerify(String email, MfaVerifyRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new RuntimeException("Le MFA n'a pas été initialisé.");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new RuntimeException("Code MFA invalide.");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    public AuthResponse mfaValidate(MfaValidateRequest request) {
        if (!jwtService.isPartialToken(request.getPartialToken())) {
            throw new RuntimeException("Token partiel invalide.");
        }

        if (jwtService.isTokenExpired(request.getPartialToken())) {
            throw new RuntimeException("Token partiel expiré.");
        }

        String email = jwtService.extractUsername(request.getPartialToken());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new RuntimeException("Le MFA n'est pas activé.");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new RuntimeException("Code MFA invalide.");
        }

        long currentWindow = System.currentTimeMillis() / 30000;
        if (user.getLastUsedOtpWindow() != null && user.getLastUsedOtpWindow() == currentWindow) {
            throw new RuntimeException("Ce code MFA a déjà été utilisé.");
        }
        user.setLastUsedOtpWindow(currentWindow);
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mfaRequired(false)
                .build();
    }

    public void updatePassword(String email, UpdatePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("L'ancien mot de passe est incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}