package com.foodrescue.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.userservice.dto.UserProfileDto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    // TESTS EXISTANTS (CONSERVÉS)
    // ==========================================

    @Test
    @DisplayName("POST /api/users/profiles : doit créer un profil")
    void createProfile_shouldReturnSavedProfile() throws Exception {
        UserProfile profile = UserProfile.builder()
                .id("1")
                .firstName("Alice").lastName("Dupont")
                .email("alice@test.com").phone("+33612345678")
                .address("Paris").city("Paris")
                .role("CONSUMER")
                .build();

        mockMvc.perform(post("/api/users/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@test.com"));
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/users/profiles/{id} : doit retourner le profil")
    void getProfileById_shouldReturnProfile() throws Exception {
        UserProfile profile = UserProfile.builder()
                .id("2").firstName("Bob").lastName("Martin")
                .email("bob@test.com").role("MERCHANT").build();
        userProfileRepository.save(profile);

        mockMvc.perform(get("/api/users/profiles/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@test.com"));
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/users/profiles/{id} : doit retourner 404 si absent")
    void getProfileById_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/users/profiles/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/users/profiles/email/{email} : doit retourner le profil")
    void getProfileByEmail_shouldReturnProfile() throws Exception {
        UserProfile profile = UserProfile.builder()
                .id("3").firstName("Charlie").lastName("Durand")
                .email("charlie@test.com").role("CONSUMER").build();
        userProfileRepository.save(profile);

        mockMvc.perform(get("/api/users/profiles/email/charlie@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Charlie"));
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/users/profiles : doit retourner tous les profils")
    void getAllProfiles_shouldReturnList() throws Exception {
        userProfileRepository.save(UserProfile.builder()
                .id("4").firstName("A").lastName("A").email("a@test.com").role("CONSUMER").build());
        userProfileRepository.save(UserProfile.builder()
                .id("5").firstName("B").lastName("B").email("b@test.com").role("CONSUMER").build());

        mockMvc.perform(get("/api/users/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/users/profiles sans auth : doit échouer")
    void getProfiles_withoutAuth_shouldFail() throws Exception {
        mockMvc.perform(get("/api/users/profiles"))
                .andExpect(status().is4xxClientError());
    }

    // ==========================================
    // 🆕 NOUVEAUX TESTS (AJOUTÉS POUR LA COUVERTURE)
    // ==========================================

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("GET /api/users/profiles/merchants : doit retourner uniquement les marchands")
    void getMerchantProfiles_shouldReturnList() throws Exception {
        userProfileRepository.save(UserProfile.builder()
                .id("6").firstName("A").lastName("A").email("a@test.com").role("MERCHANT").build());
        userProfileRepository.save(UserProfile.builder()
                .id("7").firstName("B").lastName("B").email("b@test.com").role("CONSUMER").build());

        mockMvc.perform(get("/api/users/profiles/merchants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("MERCHANT"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("POST /api/users/profiles : doit mettre à jour un profil existant (par ID)")
    void createProfile_shouldUpdateExistingProfileById() throws Exception {
        UserProfile existing = UserProfile.builder()
                .id("10").firstName("Old").lastName("Name")
                .email("old@test.com").role("CONSUMER").build();
        userProfileRepository.save(existing);

        UserProfileDto updateDto = UserProfileDto.builder()
                .id("10").firstName("New").lastName("Name")
                .email("new@test.com").role("CONSUMER").build();

        mockMvc.perform(post("/api/users/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.email").value("new@test.com"));

        assertEquals(1, userProfileRepository.count());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("POST /api/users/profiles : doit mettre à jour un profil existant (par Email)")
    void createProfile_shouldUpdateExistingProfileByEmail() throws Exception {
        UserProfile existing = UserProfile.builder()
                .id("11").firstName("Old").lastName("Name")
                .email("same@test.com").role("CONSUMER").build();
        userProfileRepository.save(existing);

        UserProfileDto updateDto = UserProfileDto.builder()
                .id("different-id").firstName("New").lastName("Name")
                .email("same@test.com").role("CONSUMER").build();

        mockMvc.perform(post("/api/users/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.id").value("11")); // L'ID ne change pas

        assertEquals(1, userProfileRepository.count());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("PUT /api/users/profiles/{id} : doit mettre à jour le profil")
    void updateProfile_shouldUpdateFields() throws Exception {
        UserProfile existing = UserProfile.builder()
                .id("20").firstName("Old").lastName("Name")
                .email("test@test.com").role("CONSUMER").build();
        userProfileRepository.save(existing);

        UserProfileDto updateDto = UserProfileDto.builder()
                .firstName("Updated").lastName("Name")
                .email("new@test.com").role("CONSUMER").build();

        mockMvc.perform(put("/api/users/profiles/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("PUT /api/users/profiles/{id} : doit retourner 404 si absent")
    void updateProfile_shouldReturn404() throws Exception {
        UserProfileDto updateDto = UserProfileDto.builder().firstName("Test").build();

        mockMvc.perform(put("/api/users/profiles/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "me@test.com", roles = "CONSUMER")
    @DisplayName("PUT /api/users/profiles/profile : doit mettre à jour le profil de l'utilisateur connecté")
    void updateMyProfile_shouldUpdateCurrentUser() throws Exception {
        UserProfile existing = UserProfile.builder()
                .id("30").firstName("Old").lastName("Name")
                .email("me@test.com").role("CONSUMER").build();
        userProfileRepository.save(existing);

        UserProfileDto updateDto = UserProfileDto.builder()
                .firstName("New").lastName("Name")
                .phone("123456").address("Paris").city("Paris").build();

        mockMvc.perform(put("/api/users/profiles/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.email").value("me@test.com")); // L'email ne change pas
    }
}