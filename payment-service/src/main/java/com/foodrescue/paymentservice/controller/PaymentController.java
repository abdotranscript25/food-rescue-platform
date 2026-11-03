package com.foodrescue.paymentservice.controller;

import com.foodrescue.paymentservice.dto.PaymentDto;
import com.foodrescue.paymentservice.dto.PaymentRequest;
import com.foodrescue.paymentservice.model.Payment;
import com.foodrescue.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentDto> processPayment(@RequestBody PaymentRequest request) {
        try {
            Payment payment = paymentService.processPayment(
                    request.getReservationId(),
                    request.getClientId(),
                    request.getAmount(),
                    request.getPaymentMethod()
            );
            return ResponseEntity.ok(mapToDto(payment));
        } catch (Exception e) {
            // Renvoie 500 si le service lance une exception (ex: Feign down)
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<PaymentDto> getByReservation(@PathVariable Long reservationId) {
        try {
            Payment payment = paymentService.getPaymentByReservation(reservationId);
            return ResponseEntity.ok(mapToDto(payment));
        } catch (RuntimeException e) {
            // Retourne 404 si le service lance une exception (paiement non trouvé)
            return ResponseEntity.notFound().build();
        }
    }

    private PaymentDto mapToDto(Payment payment) {
        return PaymentDto.builder()
                .id(payment.getId())
                .reservationId(payment.getReservationId())
                .clientId(payment.getClientId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}