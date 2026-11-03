package com.foodrescue.paymentservice.dto;

import lombok.Data;

@Data
public class PaymentRequest { // ⬅️ Le mot-clé "public" est indispensable ici
    private Long reservationId;
    private Long clientId;
    private Double amount;
    private String paymentMethod;
}