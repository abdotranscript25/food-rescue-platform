package com.foodrescue.authservice.service;

import com.foodrescue.authservice.client.UserServiceClient;
import com.foodrescue.authservice.dto.AuthResponse;
import com.foodrescue.authservice.dto.LoginRequest;
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
                .build();
    }

    // ==========================================
    // Connexion
    // ==========================================
    public AuthResponse login(LoginRequest request) {
        // Authentifier via Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Si on arrive ici, l'authentification a réussi
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

        // Générer le token
        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}