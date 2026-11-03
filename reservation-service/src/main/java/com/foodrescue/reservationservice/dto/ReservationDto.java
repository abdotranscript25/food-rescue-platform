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
public class ReservationDto {
    private Long id;
    private String consumerId;
    private Long offerId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private ReservationStatus status;
    private LocalDateTime reservationDate;
    private LocalDateTime cancelledAt;
}