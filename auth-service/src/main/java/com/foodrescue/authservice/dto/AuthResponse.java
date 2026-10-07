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
    private String token;
    private String partialToken; // ⬅️ Nécessaire pour le retour MFA
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private boolean mfaRequired; // ⬅️ Aligné avec mfaRequired(...) utilisé dans AuthService
}