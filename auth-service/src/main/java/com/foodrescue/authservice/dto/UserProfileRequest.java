package com.foodrescue.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileRequest {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
}