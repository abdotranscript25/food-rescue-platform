package com.foodrescue.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.userservice.entity.UserProfile;
import com.foodrescue.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
    }

    // ==========================================
    // Test 1 : Création de profil (POST) - Public
    // ==========================================
    @Test
    @DisplayName("POST /api/users/profiles : doit créer un profil")
    void createProfile_shouldReturnSavedProfile() throws Exception {
        UserProfile profile = UserProfile.builder()
                .id(1L).firstName("Alice").lastName("Dupont")
                .email("alice@test.com").phone("+33612345678")
                .address("Paris").city("Paris").build();

        mockMvc.perform(post("/api/users/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    // ==========================================
    // Test 2 : Récupération par ID (GET) - Protégé
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/users/profiles/{id} : doit retourner le profil")
    void getProfileById_shouldReturnProfile() throws Exception {
        UserProfile profile = UserProfile.builder()
                .id(2L).firstName("Bob").lastName("Martin")
                .email("bob@test.com").build();
        userProfileRepository.save(profile);

        mockMvc.perform(get("/api/users/profiles/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@test.com"));
    }

    // ==========================================
    // Test 3 : Profil inexistant (404) - Protégé
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/users/profiles/{id} : doit retourner 404 si absent")
    void getProfileById_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/users/profiles/9999"))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // Test 4 : Récupération par email (GET) - Protégé
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/users/profiles/email/{email} : doit retourner le profil")
    void getProfileByEmail_shouldReturnProfile() throws Exception {
        UserProfile profile = UserProfile.builder()
                .id(3L).firstName("Charlie").lastName("Durand")
                .email("charlie@test.com").build();
        userProfileRepository.save(profile);

        mockMvc.perform(get("/api/users/profiles/email/charlie@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Charlie"));
    }

    // ==========================================
    // Test 5 : Lister tous les profils - Protégé
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/users/profiles : doit retourner tous les profils")
    void getAllProfiles_shouldReturnList() throws Exception {
        userProfileRepository.save(UserProfile.builder()
                .id(4L).firstName("A").lastName("A").email("a@test.com").build());
        userProfileRepository.save(UserProfile.builder()
                .id(5L).firstName("B").lastName("B").email("b@test.com").build());

        mockMvc.perform(get("/api/users/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==========================================
    // Test 6 : GET sans authentification → 401/403
    // ==========================================
    @Test
    @DisplayName("GET /api/users/profiles sans auth : doit échouer")
    void getProfiles_withoutAuth_shouldFail() throws Exception {
        mockMvc.perform(get("/api/users/profiles"))
                .andExpect(status().is4xxClientError()); // 401 ou 403
    }
}