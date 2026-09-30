package com.foodrescue.productservice.service;

import com.foodrescue.productservice.dto.ProductRequest;
import com.foodrescue.productservice.dto.ProductResponse;
import com.foodrescue.productservice.entity.Product;
import com.foodrescue.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .allergens(request.getAllergens())
                .isPerishable(request.getIsPerishable())
                .build();

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID: " + id));
        return mapToResponse(product);
    }

    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .toList();
    }

    // ==========================================
    // AJOUT : Mise à jour
    // ==========================================
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit introuvable avec l'ID: " + id));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getAllergens() != null) product.setAllergens(request.getAllergens());
        if (request.getIsPerishable() != null) product.setIsPerishable(request.getIsPerishable());

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    // ==========================================
    // AJOUT : Suppression
    // ==========================================
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Produit introuvable avec l'ID: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .allergens(product.getAllergens())
                .isPerishable(product.getIsPerishable())
                .createdAt(product.getCreatedAt())
                .build();
    }
}