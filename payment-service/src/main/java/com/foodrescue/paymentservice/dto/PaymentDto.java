package com.foodrescue.paymentservice.dto;

import com.foodrescue.paymentservice.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDto {
    private Long id;
    private Long reservationId;
    private Long clientId;
    private Double amount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;
    private LocalDateTime createdAt;

    // AJOUT STRIPE : Permet d'envoyer l'URL au frontend
    private String checkoutUrl;
}