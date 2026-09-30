package com.foodrescue.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String email;
    private String role;

    // AJOUT : Informations utilisateur
    private String firstName;
    private String lastName;

    // MFA
    private String partialToken;
    private boolean mfaRequired;
}