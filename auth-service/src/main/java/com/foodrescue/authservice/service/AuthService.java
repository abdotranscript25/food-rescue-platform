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
import com.foodrescue.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserServiceClient userServiceClient;
    private final MfaService mfaService;   // AJOUT MFA

    // ==========================================
    // Inscription
    // ==========================================
    public AuthResponse register(RegisterRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà.");
        }

        // Créer l'utilisateur
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // Hachage !
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        // Appel Feign vers user-service pour créer le profil utilisateur associé
        try {
            UserProfileRequest profileRequest = UserProfileRequest.builder()
                    .id(savedUser.getId())
                    .fullName(savedUser.getFullName())
                    .email(savedUser.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .address(request.getAddress())
                    .city(request.getCity())
                    .build();

            userServiceClient.createProfile(profileRequest);
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du profil dans user-service : " + e.getMessage());
        }

        // Générer le token
        String token = jwtService.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .mfaRequired(false)  // AJOUT MFA
                .build();
    }

    // ==========================================
    // Connexion (MODIFIÉE POUR MFA)
    // ==========================================
    public AuthResponse login(LoginRequest request) {
        // Authentifier via Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Si on arrive ici, l'authentification a réussi
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // AJOUT MFA : Si MFA activé → renvoyer un partial token
        if (user.isMfaEnabled()) {
            String partialToken = jwtService.generatePartialToken(user.getEmail());
            return AuthResponse.builder()
                    .partialToken(partialToken)
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .mfaRequired(true)
                    .build();
        }

        // Sinon → comportement classique (token complet)
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .mfaRequired(false)
                .build();
    }

    // ==========================================
    // AJOUT MFA : Initialisation (Setup)
    // ==========================================
    /**
     * Génère un nouveau secret TOTP pour l'utilisateur et retourne le QR code.
     * Le MFA n'est PAS encore activé : il faut appeler mfaVerify() pour le confirmer.
     */
    public MfaSetupResponse mfaSetup(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // Générer un nouveau secret et le stocker temporairement
        String secret = mfaService.generateNewSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        // Générer le QR code + l'URL otpauth
        String qrCodeImage = mfaService.generateQrCodeImage(secret, user.getEmail());
        String otpauthUri = mfaService.generateQrCodeUri(secret, user.getEmail());

        return MfaSetupResponse.builder()
                .qrCodeImage(qrCodeImage)
                .otpauthUri(otpauthUri)
                .secret(secret)
                .build();
    }

    // ==========================================
    // AJOUT MFA : Vérification (Verify)
    // ==========================================
    /**
     * Vérifie le premier code saisi par l'utilisateur pour confirmer l'activation du MFA.
     * Si le code est correct → mfaEnabled passe à true.
     */
    public void mfaVerify(String email, MfaVerifyRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new RuntimeException("Le MFA n'a pas été initialisé. Appelez /mfa/setup d'abord.");
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new RuntimeException("Code MFA invalide.");
        }

        // Activer définitivement le MFA
        user.setMfaEnabled(true);
        userRepository.save(user);
    }

    // ==========================================
    // AJOUT MFA : Validation (Validate)
    // ==========================================
    /**
     * Valide le partial token + le code MFA lors de la connexion.
     * Si tout est correct → génère le token JWT final.
     */
    public AuthResponse mfaValidate(MfaValidateRequest request) {
        // 1. Vérifier que c'est bien un partial token
        if (!jwtService.isPartialToken(request.getPartialToken())) {
            throw new RuntimeException("Token partiel invalide.");
        }

        // 2. Vérifier que le token n'est pas expiré
        if (jwtService.isTokenExpired(request.getPartialToken())) {
            throw new RuntimeException("Token partiel expiré. Reconnectez-vous.");
        }

        // 3. Extraire l'email
        String email = jwtService.extractUsername(request.getPartialToken());

        // 4. Charger l'utilisateur
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // 5. Vérifier que le MFA est bien activé
        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            throw new RuntimeException("Le MFA n'est pas activé pour cet utilisateur.");
        }

        // 6. Vérifier le code MFA
        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            throw new RuntimeException("Code MFA invalide.");
        }

        // 7. Tout est bon → générer le token JWT final
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .mfaRequired(false)
                .build();
    }
}