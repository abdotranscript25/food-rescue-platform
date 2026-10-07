package com.foodrescue.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank(message = "Le nom du produit est obligatoire")
    private String name;

    private String category;
    private String description;
    private String imageUrl;
    private String allergens;
    private Boolean isPerishable = true;
    private Long merchantId;
}