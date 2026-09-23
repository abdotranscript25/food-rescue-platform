package com.foodrescue.offerservice.dto;

import lombok.Data;

@Data
public class PartnerResponseDto {
    private Long id;
    private Long userId;
    private String companyName;
    private String category;
    private String status;
}