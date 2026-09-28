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

    // AJOUT MFA
    private String partialToken;       // Rempli uniquement si MFA activé (login étape 1)
    private boolean mfaRequired;       // true si l'utilisateur doit valider le MFA
}