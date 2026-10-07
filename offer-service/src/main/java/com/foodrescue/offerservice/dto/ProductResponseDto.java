package com.foodrescue.offerservice.dto;

import lombok.Data;

@Data
public class ProductResponseDto {
    private Long id;
    private String name;
    private String category;
    private String description;
    private String imageUrl;
}