package com.foodrescue.paymentservice.controller;

import com.foodrescue.paymentservice.model.Payment;
import com.foodrescue.paymentservice.service.PaymentService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.processPayment(
                request.getReservationId(),
                request.getClientId(),
                request.getAmount(),
                request.getPaymentMethod()
        );
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<Payment> getByReservation(@PathVariable Long reservationId) {
        return ResponseEntity.ok(paymentService.getPaymentByReservation(reservationId));
    }
}

@Data
class PaymentRequest {
    private Long reservationId;
    private Long clientId;
    private Double amount;
    private String paymentMethod;
}