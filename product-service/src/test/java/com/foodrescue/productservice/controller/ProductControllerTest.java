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
    // 🆕 Test 5 : GET par Merchant (NOUVEAU)
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
}