package com.foodrescue.offerservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rating;

    @Column(length = 500)
    private String comment;

    @Column(name = "client_id", length = 36)
    private String clientId;     // 💡 Changé en String (UUID)

    @Column(name = "merchant_id", length = 36)
    private String merchantId;   // 💡 Changé en String (UUID)

    private String clientName;

    private LocalDateTime createdAt = LocalDateTime.now();
}