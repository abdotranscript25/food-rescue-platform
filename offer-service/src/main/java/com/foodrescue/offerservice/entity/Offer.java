package com.foodrescue.offerservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==========================================
    // AJOUT : Référence au Product (venant de product-service)
    // ==========================================
    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private Long merchantId;

    // ==========================================
    // Champs optionnels (legacy - normalement viennent de Product)
    // ==========================================
    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ==========================================
    // Prix (migrés de Double → BigDecimal)
    // ==========================================
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    // ==========================================
    // Quantité initiale + Quantité restante
    // ==========================================
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer remainingQuantity;

    // ==========================================
    // Période de disponibilité
    // ==========================================
    @Column(nullable = false)
    private LocalDateTime availableFrom;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // ==========================================
    // Statut (DRAFT, PUBLISHED, AVAILABLE, SOLD_OUT, EXPIRED, CANCELLED)
    // ==========================================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status;

    // ==========================================
    // Callbacks JPA : valeurs par défaut avant insertion
    // ==========================================
    @PrePersist
    public void prePersist() {
        if (this.remainingQuantity == null) {
            this.remainingQuantity = this.quantity;
        }
        if (this.availableFrom == null) {
            this.availableFrom = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = OfferStatus.AVAILABLE;
        }
    }
}