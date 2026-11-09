package com.foodrescue.paymentservice.controller;

import com.foodrescue.paymentservice.dto.PaymentDto;
import com.foodrescue.paymentservice.dto.PaymentRequest;
import com.foodrescue.paymentservice.model.Payment;
import com.foodrescue.paymentservice.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // AJOUT STRIPE
    @Value("${stripe.api.webhook-secret}")
    private String stripeWebhookSecret;

    @PostMapping
    public ResponseEntity<PaymentDto> processPayment(@RequestBody PaymentRequest request) {
        try {
            String checkoutUrl = paymentService.processPayment(
                    request.getReservationId(),
                    request.getClientId(),
                    request.getAmount(),
                    request.getPaymentMethod()
            );

            // Récupérer le paiement tout juste créé pour le renvoyer au frontend
            Payment payment = paymentService.getPaymentByReservation(request.getReservationId());
            PaymentDto dto = mapToDto(payment);
            dto.setCheckoutUrl(checkoutUrl); // Ajout de l'URL Stripe

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // AJOUT STRIPE
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);

            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().get();
                paymentService.confirmPayment(session.getId());
            }
            return ResponseEntity.ok("Webhook reçu");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Erreur Webhook: " + e.getMessage());
        }
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<PaymentDto> getByReservation(@PathVariable Long reservationId) {
        try {
            Payment payment = paymentService.getPaymentByReservation(reservationId);
            return ResponseEntity.ok(mapToDto(payment));
        } catch (RuntimeException e) {
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