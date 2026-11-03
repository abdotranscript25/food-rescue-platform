package com.foodrescue.productservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private String allergens;
    private Boolean isPerishable;
    private String merchantId; // 💡 Changé en String (UUID)
    private LocalDateTime createdAt;
}