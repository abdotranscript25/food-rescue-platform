package com.foodrescue.offerservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.offerservice.client.ProductClient;
import com.foodrescue.offerservice.dto.ProductResponseDto;
import com.foodrescue.offerservice.entity.Offer;
import com.foodrescue.offerservice.entity.OfferStatus;
import com.foodrescue.offerservice.repository.OfferRepository;
import com.foodrescue.offerservice.security.JwtService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @MockitoBean
    private ProductClient productClient;

    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        offerRepository.deleteAll();
        Mockito.reset(productClient, jwtService);
    }

    private Offer createTestOffer(int quantity, String merchantId) {
        Offer offer = Offer.builder()
                .title("Panier surprise")
                .description("Viennoiseries")
                .merchantId(merchantId)
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
    // TESTS EXISTANTS (CONSERVÉS À L'IDENTIQUE)
    // ==========================================

    @Test
    @DisplayName("GET /api/offers : public, sans auth")
    void getAllOffers_shouldBePublic() throws Exception {
        createTestOffer(5, "merchant-1");
        createTestOffer(10, "merchant-2");
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/offers/available : retourne uniquement les disponibles")
    void getAvailableOffers_shouldFilter() throws Exception {
        createTestOffer(5, "merchant-1");
        Offer soldOut = createTestOffer(0, "merchant-1");
        soldOut.setStatus(OfferStatus.SOLD_OUT);
        offerRepository.save(soldOut);
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(get("/api/offers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("POST /api/offers : créer une offre (MERCHANT)")
    void createOffer_shouldReturnSavedOffer() throws Exception {
        Offer offer = Offer.builder()
                .title("Panier test")
                .description("Test")
                .merchantId("merchant-uuid-123")
                .productId(1L)
                .originalPrice(new BigDecimal("10.00"))
                .discountedPrice(new BigDecimal("3.50"))
                .quantity(5)
                .availableFrom(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Panier test"))
                .andExpect(jsonPath("$.remainingQuantity").value(5));
    }

    @Test
    @DisplayName("POST /api/offers : doit échouer sans auth")
    void createOffer_shouldFailWithoutAuth() throws Exception {
        Offer offer = Offer.builder().title("Test").merchantId("merchant-1").build();
        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("POST /api/offers : doit échouer avec CONSUMER")
    void createOffer_shouldFailForConsumer() throws Exception {
        Offer offer = Offer.builder().title("Test").merchantId("merchant-1").build();
        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/offers/{id}/decrement : doit décrémenter le stock")
    void decrementStock_shouldDecrease() throws Exception {
        Offer offer = createTestOffer(10, "merchant-1");
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/decrement?quantity=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(7));
    }

    @Test
    @DisplayName("PUT /api/offers/{id}/decrement : doit échouer si stock insuffisant")
    void decrementStock_shouldFailIfInsufficient() throws Exception {
        Offer offer = createTestOffer(2, "merchant-1");
        mockMvc.perform(put("/api/offers/" + offer.getId() + "/decrement?quantity=10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/offers/{id}/increment : doit restaurer le stock")
    void incrementStock_shouldIncrease() throws Exception {
        Offer offer = createTestOffer(5, "merchant-1");
        offer.setRemainingQuantity(3);
        offerRepository.save(offer);
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/increment?quantity=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(5));
    }

    @Test
    @DisplayName("PUT /api/offers/{id}/decrement : SOLD_OUT si stock atteint 0")
    void decrementStock_shouldSetSoldOut() throws Exception {
        Offer offer = createTestOffer(3, "merchant-1");
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/decrement?quantity=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingQuantity").value(0))
                .andExpect(jsonPath("$.status").value("SOLD_OUT"));
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("PUT /api/offers/{id}/cancel : doit annuler l'offre")
    void cancelOffer_shouldWork() throws Exception {
        Offer offer = createTestOffer(5, "merchant-1");
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("GET /api/offers/my-offers : retourne uniquement les offres du marchand connecté")
    void getMyOffers_shouldReturnOnlyMyOffers() throws Exception {
        createTestOffer(5, "merchant-uuid-123");
        createTestOffer(10, "merchant-uuid-123");
        createTestOffer(3, "other-merchant");
        Mockito.when(jwtService.extractUserId("fake-jwt-token")).thenReturn("merchant-uuid-123");
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(get("/api/offers/my-offers")
                        .header("Authorization", "Bearer fake-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].merchantId").value("merchant-uuid-123"));
    }

    // ==========================================
    // 🆕 NOUVEAUX TESTS (AJOUTÉS POUR LA COUVERTURE)
    // ==========================================

    @Test
    @DisplayName("GET /api/offers/status/{status} : doit filtrer par statut")
    void getOffersByStatus_shouldReturnFilteredList() throws Exception {
        createTestOffer(5, "m1"); // AVAILABLE
        Offer draft = createTestOffer(5, "m1");
        draft.setStatus(OfferStatus.DRAFT);
        offerRepository.save(draft);
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(get("/api/offers/status/AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/offers/merchant/{merchantId} : doit filtrer par marchand")
    void getOffersByMerchant_shouldReturnFilteredList() throws Exception {
        createTestOffer(5, "merchant-A");
        createTestOffer(5, "merchant-A");
        createTestOffer(5, "merchant-B");
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(get("/api/offers/merchant/merchant-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/offers/{id} : doit retourner l'offre")
    void getOfferById_shouldReturnOffer() throws Exception {
        Offer offer = createTestOffer(5, "m1");
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(get("/api/offers/" + offer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(offer.getId()))
                .andExpect(jsonPath("$.title").value("Panier surprise"));
    }

    @Test
    @DisplayName("GET /api/offers/{id} : doit retourner 404 si introuvable")
    void getOfferById_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/offers/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("POST /api/offers : doit échouer si merchantId manquant")
    void createOffer_shouldFailIfMerchantIdMissing() throws Exception {
        Offer offer = Offer.builder().title("Test").quantity(5).build();

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("POST /api/offers : doit échouer si quantité invalide")
    void createOffer_shouldFailIfQuantityInvalid() throws Exception {
        Offer offer = Offer.builder().title("Test").merchantId("m1").quantity(0).build();

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("POST /api/offers : doit gérer l'erreur Feign sans planter")
    void createOffer_shouldHandleFeignException() throws Exception {
        Offer offer = Offer.builder()
                .title("Test").merchantId("m1").quantity(5)
                .originalPrice(new BigDecimal("10.00")).discountedPrice(new BigDecimal("5.00"))
                .expiresAt(LocalDateTime.now().plusDays(1)).build();

        // On simule un crash de Feign
        Mockito.when(productClient.getProductById(1L)).thenThrow(new RuntimeException("Service down"));

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().isOk()); // L'offre est créée, même si le produit n'est pas trouvé
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("PUT /api/offers/{id} : doit mettre à jour les champs")
    void updateOffer_shouldUpdateFields() throws Exception {
        Offer offer = createTestOffer(5, "m1");
        Offer update = Offer.builder().title("Nouveau Titre").description("Nouvelle Desc").build();
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(put("/api/offers/" + offer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nouveau Titre"))
                .andExpect(jsonPath("$.description").value("Nouvelle Desc"));
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("PUT /api/offers/{id} : doit recalculer le stock restant")
    void updateOffer_shouldUpdateQuantityAndRemaining() throws Exception {
        Offer offer = createTestOffer(10, "m1"); // 10 restants
        Offer update = Offer.builder().quantity(15).build(); // +5
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(put("/api/offers/" + offer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(15))
                .andExpect(jsonPath("$.remainingQuantity").value(15)); // 10 + 5
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("PUT /api/offers/{id} : doit retourner 404 si introuvable")
    void updateOffer_shouldReturn404() throws Exception {
        Offer update = Offer.builder().title("Test").build();
        mockMvc.perform(put("/api/offers/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("PUT /api/offers/{id}/publish : doit passer en AVAILABLE")
    void publishOffer_shouldChangeStatus() throws Exception {
        Offer offer = createTestOffer(5, "m1");
        offer.setStatus(OfferStatus.DRAFT);
        offerRepository.save(offer);
        Mockito.when(productClient.getProductById(1L)).thenReturn(new ProductResponseDto());

        mockMvc.perform(put("/api/offers/" + offer.getId() + "/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("PUT /api/offers/{id}/publish : échoue si pas DRAFT")
    void publishOffer_shouldFailIfNotDraft() throws Exception {
        Offer offer = createTestOffer(5, "m1"); // Déjà AVAILABLE
        mockMvc.perform(put("/api/offers/" + offer.getId() + "/publish"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    @DisplayName("DELETE /api/offers/{id} : doit supprimer l'offre")
    void deleteOffer_shouldDelete() throws Exception {
        Offer offer = createTestOffer(5, "m1");

        mockMvc.perform(delete("/api/offers/" + offer.getId()))
                .andExpect(status().isOk());

        assertFalse(offerRepository.existsById(offer.getId()));
    }
}