package com.foodrescue.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.productservice.dto.ProductRequest;
import com.foodrescue.productservice.entity.Product;
import com.foodrescue.productservice.repository.ProductRepository;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    // Helper pour créer un produit en base
    private Product createTestProduct(String name, String merchantId) {
        return productRepository.save(Product.builder()
                .name(name)
                .merchantId(merchantId)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // 🆕 Helper pour créer un produit avec catégorie (utile pour les tests de filtre)
    private Product createTestProductWithCategory(String name, String merchantId, String category) {
        return productRepository.save(Product.builder()
                .name(name)
                .merchantId(merchantId)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ==========================================
    // Test 1 : Création avec MERCHANT
    // ==========================================
    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("POST /api/products : doit créer un produit (MERCHANT)")
    void createProduct_shouldReturn201() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Pain Bio");
        request.setCategory("Boulangerie");
        request.setMerchantId("merchant-uuid-123");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pain Bio"))
                .andExpect(jsonPath("$.category").value("Boulangerie"))
                .andExpect(jsonPath("$.merchantId").value("merchant-uuid-123"));
    }

    // ==========================================
    // Test 2 : Création sans auth → 401/403
    // ==========================================
    @Test
    @DisplayName("POST /api/products : doit échouer sans auth")
    void createProduct_shouldFailWithoutAuth() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Pain");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ==========================================
    // Test 3 : Création avec CONSUMER → 403
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("POST /api/products : doit échouer avec CONSUMER")
    void createProduct_shouldFailForConsumer() throws Exception {
        ProductRequest request = new ProductRequest();
        request.setName("Pain");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // Test 4 : GET public (sans auth)
    // ==========================================
    @Test
    @DisplayName("GET /api/products : public, sans auth")
    void getAllProducts_shouldBePublic() throws Exception {
        createTestProduct("Pain", "merchant-1");

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================
    // Test 5 : GET par Merchant
    // ==========================================
    @Test
    @DisplayName("GET /api/products/merchant/{merchantId} : doit filtrer par marchand")
    void getProductsByMerchant_shouldReturnFilteredList() throws Exception {
        createTestProduct("Pain", "merchant-A");
        createTestProduct("Croissant", "merchant-A");
        createTestProduct("Pomme", "merchant-B");

        mockMvc.perform(get("/api/products/merchant/merchant-A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].merchantId").value("merchant-A"));
    }

    // ==========================================
    // 🆕 Test 6 : GET par ID
    // ==========================================
    @Test
    @DisplayName("GET /api/products/{id} : doit retourner le produit")
    void getProductById_shouldReturnProduct() throws Exception {
        Product product = createTestProduct("Lait", "merchant-1");

        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Lait"));
    }

    // ==========================================
    // 🆕 Test 7 : GET par catégorie
    // ==========================================
    @Test
    @DisplayName("GET /api/products/category/{category} : doit filtrer par catégorie")
    void getProductsByCategory_shouldReturnFilteredList() throws Exception {
        createTestProductWithCategory("Pain", "merchant-1", "Boulangerie");
        createTestProductWithCategory("Baguette", "merchant-1", "Boulangerie");
        createTestProductWithCategory("Pomme", "merchant-2", "Fruits");

        mockMvc.perform(get("/api/products/category/Boulangerie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ==========================================
    // 🆕 Test 8 : PUT mise à jour
    // ==========================================
    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("PUT /api/products/{id} : doit mettre à jour le produit")
    void updateProduct_shouldUpdate() throws Exception {
        Product product = createTestProduct("Ancien nom", "merchant-1");

        ProductRequest updateRequest = new ProductRequest();
        updateRequest.setName("Nouveau nom");
        updateRequest.setCategory("Nouvelle catégorie");
        updateRequest.setDescription("Nouvelle description");

        mockMvc.perform(put("/api/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nouveau nom"))
                .andExpect(jsonPath("$.category").value("Nouvelle catégorie"))
                .andExpect(jsonPath("$.description").value("Nouvelle description"));
    }

    // ==========================================
    // 🆕 Test 9 : DELETE suppression
    // ==========================================
    @Test
    @WithMockUser(username = "martin@test.com", roles = "MERCHANT")
    @DisplayName("DELETE /api/products/{id} : doit supprimer le produit")
    void deleteProduct_shouldDelete() throws Exception {
        Product product = createTestProduct("À supprimer", "merchant-1");

        mockMvc.perform(delete("/api/products/" + product.getId()))
                .andExpect(status().isNoContent());

        // Vérifier en base qu'il a bien été supprimé
        assertFalse(productRepository.existsById(product.getId()));
    }
}