package com.foodrescue.productservice.controller;

import com.foodrescue.productservice.dto.ProductRequest;
import com.foodrescue.productservice.dto.ProductResponse;
import com.foodrescue.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ==========================================
    // Création (MERCHANT ou ADMIN)
    // ==========================================

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    // ==========================================
    // Lecture (public)
    // ==========================================

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // ==========================================
    // AJOUT : Récupération par Marchand
    // ==========================================

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<ProductResponse>> getProductsByMerchant(@PathVariable Long merchantId) {
        return ResponseEntity.ok(productService.getProductsByMerchant(merchantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    // ==========================================
    // AJOUT : Mise à jour (MERCHANT ou ADMIN)
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // ==========================================
    // AJOUT : Suppression (ADMIN)
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}