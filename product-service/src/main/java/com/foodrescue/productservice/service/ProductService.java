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

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .description(product.getDescription())
                .allergens(product.getAllergens())
                .isPerishable(product.getIsPerishable())
                .createdAt(product.getCreatedAt())
                .build();
    }
}