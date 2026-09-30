package com.foodrescue.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferResponse {
    private Long id;
    private Long productId;
    private Long merchantId;
    private String title;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private Integer quantity;
    private Integer remainingQuantity;
    private String status;
}