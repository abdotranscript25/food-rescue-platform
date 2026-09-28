package com.foodrescue.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaVerifyRequest {

    /**
     * Code à 6 chiffres généré par Google Authenticator.
     * Utilisé pour confirmer que l'utilisateur a bien configuré son app.
     */
    private String code;
}