package com.foodrescue.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaSetupResponse {

    /**
     * Image du QR code encodée en Base64 (Data URI).
     * Peut être utilisée directement dans une balise HTML :
     * <img src="data:image/png;base64,..." />
     */
    private String qrCodeImage;

    /**
     * URL otpauth:// (utile pour les clients qui ne peuvent pas afficher d'image).
     */
    private String otpauthUri;

    /**
     * Le secret TOTP en clair (utile pour tester manuellement
     * ou saisir dans Google Authenticator si le scan ne marche pas).
     */
    private String secret;
}