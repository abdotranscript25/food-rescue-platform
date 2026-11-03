package com.foodrescue.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.authservice.dto.*;
import com.foodrescue.authservice.entity.Role;
import com.foodrescue.authservice.entity.User;
import com.foodrescue.authservice.entity.UserStatus;
import com.foodrescue.authservice.repository.UserRepository;
import com.foodrescue.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID().toString())
                .firstName("Alice")
                .lastName("Test")
                .email("alice@test.com")
                .password("hashed_password")
                .role(Role.CONSUMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    // ==========================================
    // TESTS EXISTANTS (CONSERVÉS)
    // ==========================================

    @Test
    @DisplayName("POST /api/auth/register : doit appeler le service et retourner 200")
    void register_shouldReturnAuthResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@test.com");
        request.setFirstName("Alice");

        AuthResponse response = AuthResponse.builder()
                .email("alice@test.com")
                .token("jwt.token.here")
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login : doit retourner un cookie JWT si pas de MFA")
    void login_shouldReturnCookie() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@test.com");

        AuthResponse response = AuthResponse.builder()
                .email("alice@test.com")
                .token("jwt.token.here")
                .mfaRequired(false)
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @DisplayName("GET /api/auth/me : doit retourner le DTO (sans mot de passe)")
    @WithMockUser(username = "alice@test.com")
    void getCurrentUser_shouldReturnDtoWithoutPassword() throws Exception {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.id").value(mockUser.getId()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.mfaSecret").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/auth/update-profile : doit mettre à jour le prénom")
    @WithMockUser(username = "alice@test.com")
    void updateProfile_shouldUpdateUser() throws Exception {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(mockUser));

        String requestJson = "{\"firstName\":\"Nouveau\",\"lastName\":\"Nom\"}";

        mockMvc.perform(put("/api/auth/update-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Profil mis à jour dans la base Auth avec succès."));
    }

    // ==========================================
    // 🆕 NOUVEAUX TESTS (AJOUTÉS POUR LA COUVERTURE)
    // ==========================================

    @Test
    @DisplayName("PUT /api/auth/update-password : doit mettre à jour le mot de passe")
    @WithMockUser(username = "alice@test.com")
    void updatePassword_shouldWork() throws Exception {
        String requestJson = "{\"oldPassword\":\"old\",\"newPassword\":\"new\"}";

        // On mocke l'appel au service (qui ne renvoie rien, donc void)
        Mockito.doNothing().when(authService).updatePassword(anyString(), any(UpdatePasswordRequest.class));

        mockMvc.perform(put("/api/auth/update-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Mot de passe mis à jour avec succès."));
    }

    @Test
    @DisplayName("POST /api/auth/mfa/setup : doit retourner le QR code")
    @WithMockUser(username = "alice@test.com")
    void mfaSetup_shouldReturnQrCode() throws Exception {
        MfaSetupResponse mockResponse = MfaSetupResponse.builder()
                .secret("SECRET123")
                .qrCodeImage("data:image/png;base64,...")
                .build();

        when(authService.mfaSetup("alice@test.com")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/mfa/setup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").value("SECRET123"));
    }

    @Test
    @DisplayName("POST /api/auth/mfa/verify : doit valider le code MFA")
    @WithMockUser(username = "alice@test.com")
    void mfaVerify_shouldWork() throws Exception {
        MfaVerifyRequest request = new MfaVerifyRequest("123456");

        Mockito.doNothing().when(authService).mfaVerify(anyString(), any(MfaVerifyRequest.class));

        mockMvc.perform(post("/api/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/mfa/validate : doit retourner un cookie JWT")
    void mfaValidate_shouldReturnCookie() throws Exception {
        MfaValidateRequest request = new MfaValidateRequest("partial_token", "123456");

        AuthResponse response = AuthResponse.builder()
                .email("alice@test.com")
                .token("full_jwt_token")
                .build();

        when(authService.mfaValidate(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/mfa/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(jsonPath("$.token").value("full_jwt_token"));
    }
}