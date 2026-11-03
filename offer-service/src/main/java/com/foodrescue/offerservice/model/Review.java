package com.foodrescue.offerservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder; // ⬅️ IMPORT AJOUTÉ
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // ⬅️ ANNOTATION AJOUTÉE
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rating;

    @Column(length = 500)
    private String comment;

    @Column(name = "client_id", length = 36)
    private String clientId;

    @Column(name = "merchant_id", length = 36)
    private String merchantId;

    private String clientName;

    @Builder.Default // ⬅️ AJOUTÉ pour que Lombok initialise bien la date si on ne la fournit pas
    private LocalDateTime createdAt = LocalDateTime.now();
}