package com.foodrescue.offerservice.dto;

import com.foodrescue.offerservice.entity.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferDto {
    private Long id;
    private Long productId;
    private ProductResponseDto product; // Garde le produit enrichi
    private String merchantId;
    private String title;
    private String description;
    private String imageUrl;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private Integer quantity;
    private Integer remainingQuantity;
    private LocalDateTime availableFrom;
    private LocalDateTime expiresAt;
    private OfferStatus status;
}