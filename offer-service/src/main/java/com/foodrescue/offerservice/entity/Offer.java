package com.foodrescue.offerservice.entity;

import com.foodrescue.offerservice.dto.ProductResponseDto;
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

    @Column(name = "product_id")
    private Long productId;

    @Transient
    private ProductResponseDto product;

    // 💡 Changé de Long à String (UUID)
    @Column(name = "merchant_id", length = 36, nullable = false)
    private String merchantId;

    @Column
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer remainingQuantity;

    @Column(nullable = false)
    private LocalDateTime availableFrom;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status;

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