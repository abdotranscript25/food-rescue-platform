package com.foodrescue.productservice.service;

import com.foodrescue.productservice.dto.ProductRequest;
import com.foodrescue.productservice.dto.ProductResponse;
import com.foodrescue.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    // ==========================================
    // Test 1 : Création d'un produit
    // ==========================================
    @Test
    @DisplayName("createProduct : doit créer un produit avec tous les champs")
    void createProduct_shouldCreateProduct() {
        ProductRequest request = new ProductRequest();
        request.setName("Pain Bio");
        request.setCategory("Boulangerie");
        request.setDescription("Pain frais du jour");
        request.setImageUrl("https://example.com/pain.jpg");
        request.setAllergens("Gluten");
        request.setIsPerishable(true);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response.getId());
        assertEquals("Pain Bio", response.getName());
        assertEquals("Boulangerie", response.getCategory());
        assertEquals("https://example.com/pain.jpg", response.getImageUrl());
        assertNotNull(response.getCreatedAt());
    }

    // ==========================================
    // Test 2 : Récupération par ID
    // ==========================================
    @Test
    @DisplayName("getProductById : doit retourner le produit")
    void getProductById_shouldReturnProduct() {
        ProductRequest request = new ProductRequest();
        request.setName("Croissant");
        request.setCategory("Boulangerie");
        ProductResponse created = productService.createProduct(request);

        ProductResponse found = productService.getProductById(created.getId());

        assertEquals("Croissant", found.getName());
    }

    // ==========================================
    // Test 3 : Produit inexistant → exception
    // ==========================================
    @Test
    @DisplayName("getProductById : doit échouer si le produit n'existe pas")
    void getProductById_shouldThrowIfNotFound() {
        assertThrows(RuntimeException.class, () -> productService.getProductById(9999L));
    }

    // ==========================================
    // Test 4 : Liste tous les produits
    // ==========================================
    @Test
    @DisplayName("getAllProducts : doit retourner tous les produits")
    void getAllProducts_shouldReturnList() {
        ProductRequest r1 = new ProductRequest();
        r1.setName("Pain"); r1.setCategory("Boulangerie");
        productService.createProduct(r1);

        ProductRequest r2 = new ProductRequest();
        r2.setName("Pomme"); r2.setCategory("Fruits");
        productService.createProduct(r2);

        List<ProductResponse> products = productService.getAllProducts();

        assertEquals(2, products.size());
    }

    // ==========================================
    // Test 5 : Filtrer par catégorie
    // ==========================================
    @Test
    @DisplayName("getProductsByCategory : doit filtrer par catégorie")
    void getProductsByCategory_shouldFilter() {
        ProductRequest r1 = new ProductRequest();
        r1.setName("Pain"); r1.setCategory("Boulangerie");
        productService.createProduct(r1);

        ProductRequest r2 = new ProductRequest();
        r2.setName("Pomme"); r2.setCategory("Fruits");
        productService.createProduct(r2);

        List<ProductResponse> boulangerie = productService.getProductsByCategory("Boulangerie");

        assertEquals(1, boulangerie.size());
        assertEquals("Pain", boulangerie.get(0).getName());
    }

    // ==========================================
    // Test 6 : Mise à jour d'un produit
    // ==========================================
    @Test
    @DisplayName("updateProduct : doit modifier le produit")
    void updateProduct_shouldUpdate() {
        ProductRequest createReq = new ProductRequest();
        createReq.setName("Ancien nom");
        createReq.setCategory("Ancienne catégorie");
        ProductResponse created = productService.createProduct(createReq);

        ProductRequest updateReq = new ProductRequest();
        updateReq.setName("Nouveau nom");
        updateReq.setCategory("Nouvelle catégorie");

        ProductResponse updated = productService.updateProduct(created.getId(), updateReq);

        assertEquals("Nouveau nom", updated.getName());
        assertEquals("Nouvelle catégorie", updated.getCategory());
    }

    // ==========================================
    // Test 7 : Suppression
    // ==========================================
    @Test
    @DisplayName("deleteProduct : doit supprimer le produit")
    void deleteProduct_shouldDelete() {
        ProductRequest request = new ProductRequest();
        request.setName("À supprimer");
        ProductResponse created = productService.createProduct(request);

        productService.deleteProduct(created.getId());

        assertFalse(productRepository.existsById(created.getId()));
    }

    // ==========================================
    // Test 8 : Suppression d'un produit inexistant
    // ==========================================
    @Test
    @DisplayName("deleteProduct : doit échouer si le produit n'existe pas")
    void deleteProduct_shouldThrowIfNotFound() {
        assertThrows(RuntimeException.class, () -> productService.deleteProduct(9999L));
    }
}