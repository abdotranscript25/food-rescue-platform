package com.foodrescue.reservationservice.dto;

import com.foodrescue.reservationservice.entity.ReservationStatus;
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
public class ReservationEnrichedResponse {
    private Long id;
    private Integer quantity;
    private BigDecimal totalPrice;
    private ReservationStatus status;
    private LocalDateTime reservationDate;
    private LocalDateTime cancelledAt;
    private UserProfileResponse consumer;
    private OfferResponse offer;
}