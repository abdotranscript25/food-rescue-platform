package com.foodrescue.offerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {
    private Long id;
    private Integer rating;
    private String comment;
    private String clientId;
    private String merchantId;
    private String clientName;
    private LocalDateTime createdAt;
}