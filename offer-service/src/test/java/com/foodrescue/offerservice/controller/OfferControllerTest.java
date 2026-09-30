package com.foodrescue.offerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.offerservice.entity.Offer;
import com.foodrescue.offerservice.entity.OfferStatus;
import com.foodrescue.offerservice.repository.OfferRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        offerRepository.deleteAll();
    }

    // ==========================================
    // Helper : créer une offre en base
    // ==========================================
    private Offer createTestOffer(int quantity) {
        Offer offer = Offer.builder()
                .title("Panier surprise")
                .description("Viennoiseries")
                .merchantId(1L)
                .productId(1L)
                .originalPrice(new BigDecimal("15.00"))
                .discountedPrice(new BigDecimal("5.00"))
                .quantity(quantity)
                .remainingQuantity(quantity)
                .availableFrom(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .status(OfferStatus.AVAILABLE)
                .build();
        return offerRepository.save(offer);
    }

    // ==========================================
    // Test 1 : GET public (sans auth)
    // ==========================================
    @Test
    @DisplayName("GET /api/offers : public, sans auth")
    void getAllOffers_shouldBePublic() throws Exception {
        createTestOffer(5);
        createTestOffer(10);

        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==========================================
    // Test 2 : GET /available
    // ==========================================
    @Test
    @DisplayName("GET /api/offers/available : retourne uniquement les disponibles")
    void getAvailableOffers_shouldFilter() throws Exception {
        createTestOffer(5);

        // Créer une offre épuisée
        Offer soldOut = createTestOffer(0);
        soldOut.setStatus(OfferStatus.SOLD_OUT);
        offerRepository.save(soldOut);

        mockMvc.perform(get("/api/offers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================
    // Test 3 : POST avec MERCHANT
    // ==========================================
    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("POST /api/offers : créer une offre (MERCHANT)")
    void createOffer_shouldReturnSavedOffer() throws Exception {
        Offer offer = Offer.builder()
                .title("Panier test")
                .description("Test")
                .merchantId(1L)
                .productId(1L)
                .originalPrice(new BigDecimal("10.00"))
                .discountedPrice(new BigDecimal("3.50"))
                .quantity(5)
                .availableFrom(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Panier test"))
                .andExpect(jsonPath("$.remainingQuantity").value(5));
    }

    // ==========================================
    // Test 4 : POST sans auth → 401/403
    // ==========================================
    @Test
    @DisplayName("POST /api/offers : doit échouer sans auth")
    void createOffer_shouldFailWithoutAuth() throws Exception {
        Offer offer = Offer.builder().title("Test").merchantId(1L).build();

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().is4xxClientError());
    }

    // ==========================================
    // Test 5 : POST avec CONSUMER → 403
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("POST /api/offers : doit échouer avec CONSUMER")
    void createOffer_shouldFailForConsumer() throws Exception {
        Offer offer = Offer.builder().title("Test").merchantId(1L).build();

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // Test 6 : Decrement (endpoint interne, public)
    // ==========================================
    @Test
    @DisplayName("PUT /api/offers/{id}/decrement : doit décrémenter le stock")
    void decrementStock_shouldDecrease() throws Exception {
        Offer offer = createTestOffer(10);

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/decrement?quantity=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(7));
    }

    // ==========================================
    // Test 7 : Decrement avec stock insuffisant
    // ==========================================
    @Test
    @DisplayName("PUT /api/offers/{id}/decrement : doit échouer si stock insuffisant")
    void decrementStock_shouldFailIfInsufficient() throws Exception {
        Offer offer = createTestOffer(2);

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/decrement?quantity=10"))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // Test 8 : Increment (restauration)
    // ==========================================
    @Test
    @DisplayName("PUT /api/offers/{id}/increment : doit restaurer le stock")
    void incrementStock_shouldIncrease() throws Exception {
        Offer offer = createTestOffer(5);
        offer.setRemainingQuantity(3);
        offerRepository.save(offer);

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/increment?quantity=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(5));
    }

    // ==========================================
    // Test 9 : Decrement → SOLD_OUT automatique
    // ==========================================
    @Test
    @DisplayName("PUT /api/offers/{id}/decrement : SOLD_OUT si stock atteint 0")
    void decrementStock_shouldSetSoldOut() throws Exception {
        Offer offer = createTestOffer(3);

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/decrement?quantity=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(0))
                .andExpect(jsonPath("$.status").value("SOLD_OUT"));
    }

    // ==========================================
    // Test 10 : Cancel
    // ==========================================
    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("PUT /api/offers/{id}/cancel : doit annuler l'offre")
    void cancelOffer_shouldWork() throws Exception {
        Offer offer = createTestOffer(5);

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}