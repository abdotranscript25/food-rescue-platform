package com.foodrescue.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String id; // Changé de Long à String (UUID)
    private String token;
    private String partialToken;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private boolean mfaRequired;
}