package com.foodrescue.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reservationId;

    @Column(nullable = false)
    private Long clientId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency; // Ex: "MAD"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; // SUCCESS, PENDING, FAILED

    @Column(nullable = false)
    private String paymentMethod; // "CREDIT_CARD", "APPLE_PAY", etc.

    // AJOUT STRIPE
    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
}