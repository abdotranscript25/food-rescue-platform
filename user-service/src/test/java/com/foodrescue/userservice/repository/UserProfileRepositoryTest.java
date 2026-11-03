package com.foodrescue.userservice.repository;

import com.foodrescue.userservice.entity.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserProfileRepositoryTest {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void setUp() {
        userProfileRepository.deleteAll();
    }

    @Test
    @DisplayName("Save : doit créer un profil")
    void save_shouldCreateProfile() {
        UserProfile profile = UserProfile.builder()
                .id("1") // ⬅️ String au lieu de 1L
                .firstName("Alice").lastName("Dupont")
                .email("alice@test.com").phone("+33612345678")
                .address("1 rue de Paris").city("Paris")
                .role("CONSUMER")
                .build();

        UserProfile saved = userProfileRepository.save(profile);

        assertNotNull(saved);
        assertEquals("1", saved.getId()); // ⬅️ String
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("findByEmail : doit trouver un profil par email")
    void findByEmail_shouldReturnProfile() {
        UserProfile profile = UserProfile.builder()
                .id("2").firstName("Bob").lastName("Martin")
                .email("bob@test.com").role("MERCHANT").build();
        userProfileRepository.save(profile);

        Optional<UserProfile> found = userProfileRepository.findByEmail("bob@test.com");

        assertTrue(found.isPresent());
        assertEquals("Bob", found.get().getFirstName());
        assertEquals("MERCHANT", found.get().getRole());
    }

    @Test
    @DisplayName("findByEmail : doit retourner vide si email inconnu")
    void findByEmail_shouldReturnEmptyIfNotFound() {
        Optional<UserProfile> found = userProfileRepository.findByEmail("inconnu@test.com");
        assertTrue(found.isEmpty());
    }
}