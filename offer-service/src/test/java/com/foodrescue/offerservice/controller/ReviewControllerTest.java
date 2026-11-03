package com.foodrescue.offerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.offerservice.dto.ReviewDto;
import com.foodrescue.offerservice.model.Review;
import com.foodrescue.offerservice.repository.ReviewRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
    }

    private Review createTestReview(String merchantId, int rating) {
        return reviewRepository.save(Review.builder()
                .rating(rating)
                .comment("Super !")
                .clientId("client-1")
                .merchantId(merchantId)
                .clientName("Alice")
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ==========================================
    // Test 1 : Créer un avis valide
    // ==========================================
    @Test
    @WithMockUser(username = "client@test.com", roles = "CONSUMER")
    @DisplayName("POST /api/reviews : doit créer un avis")
    void createReview_shouldCreateReview() throws Exception {
        ReviewDto dto = ReviewDto.builder()
                .rating(5)
                .comment("Excellent")
                .merchantId("merchant-1")
                .clientName("Bob")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent"));
    }

    // ==========================================
    // Test 2 : Rejeter un avis avec note invalide
    // ==========================================
    @Test
    @WithMockUser(username = "client@test.com", roles = "CONSUMER")
    @DisplayName("POST /api/reviews : doit échouer si note invalide")
    void createReview_shouldFailIfRatingInvalid() throws Exception {
        ReviewDto dto = ReviewDto.builder().rating(6).build(); // Note > 5

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // Test 3 : Récupérer les avis d'un marchand
    // ==========================================
    @Test
    @WithMockUser(username = "client@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/reviews/merchant/{merchantId} : doit retourner les avis")
    void getReviewsByMerchant_shouldReturnReviews() throws Exception {
        createTestReview("merchant-A", 4);
        createTestReview("merchant-A", 5);
        createTestReview("merchant-B", 3);

        mockMvc.perform(get("/api/reviews/merchant/merchant-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==========================================
    // Test 4 : Calculer la moyenne (avec résultats)
    // ==========================================
    @Test
    @WithMockUser(username = "client@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/reviews/merchant/{id}/average : doit calculer la moyenne")
    void getAverageRating_shouldReturnAverage() throws Exception {
        createTestReview("merchant-A", 4);
        createTestReview("merchant-A", 2); // Moyenne = 3.0

        mockMvc.perform(get("/api/reviews/merchant/merchant-A/average"))
                .andExpect(status().isOk())
                .andExpect(content().string("3.0"));
    }

    // ==========================================
    // Test 5 : Calculer la moyenne (aucun avis)
    // ==========================================
    @Test
    @WithMockUser(username = "client@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/reviews/merchant/{id}/average : retourne 0.0 si aucun avis")
    void getAverageRating_shouldReturnZeroIfNoReviews() throws Exception {
        mockMvc.perform(get("/api/reviews/merchant/merchant-inconnu/average"))
                .andExpect(status().isOk())
                .andExpect(content().string("0.0"));
    }
}