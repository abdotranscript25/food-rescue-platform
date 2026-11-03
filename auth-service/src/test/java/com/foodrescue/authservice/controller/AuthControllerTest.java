package com.foodrescue.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.authservice.dto.AuthResponse;
import com.foodrescue.authservice.dto.LoginRequest;
import com.foodrescue.authservice.dto.RegisterRequest;
import com.foodrescue.authservice.entity.Role;
import com.foodrescue.authservice.entity.User;
import com.foodrescue.authservice.entity.UserStatus;
import com.foodrescue.authservice.repository.UserRepository;
import com.foodrescue.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .password("hashed_password") // Ne doit pas apparaître dans le DTO
                .role(Role.CONSUMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

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
                .andExpect(cookie().exists("jwt")) // Vérifie la présence du cookie HttpOnly
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
                // Vérifie que les champs sensibles ne sont PAS exposés
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
}