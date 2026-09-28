package com.foodrescue.authservice.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
public class MfaService {

    @Value("${mfa.issuer}")
    private String issuer;

    @Value("${mfa.digits}")
    private int digits;

    @Value("${mfa.period}")
    private int period;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier codeVerifier;

    public MfaService() {
        // Configuration du vérificateur de codes TOTP
        this.codeVerifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6),
                timeProvider
        );
    }

    /**
     * Génère un nouveau secret TOTP pour un utilisateur.
     * @return une chaîne Base32 (ex: "JBSWY3DPEHPK3PXP")
     */
    public String generateNewSecret() {
        return secretGenerator.generate();
    }

    /**
     * Génère une URL otpauth:// que Google Authenticator peut interpréter.
     * @param secret Le secret TOTP de l'utilisateur
     * @param email L'email de l'utilisateur (pour l'affichage)
     * @return URL au format otpauth://totp/...
     */
    public String generateQrCodeUri(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(digits)
                .period(period)
                .build();
        return data.getUri();
    }

    /**
     * Génère une image PNG du QR code encodée en Base64 (Data URI).
     * Cette chaîne peut être utilisée directement dans une balise HTML <img src="...">
     * @param secret Le secret TOTP
     * @param email L'email de l'utilisateur
     * @return Data URI de l'image PNG (base64)
     */
    public String generateQrCodeImage(String secret, String email) {
        try {
            QrData data = new QrData.Builder()
                    .label(email)
                    .secret(secret)
                    .issuer(issuer)
                    .algorithm(HashingAlgorithm.SHA1)
                    .digits(digits)
                    .period(period)
                    .build();
            byte[] imageData = qrGenerator.generate(data);
            return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du QR code", e);
        }
    }

    /**
     * Vérifie qu'un code à 6 chiffres correspond au secret de l'utilisateur.
     * @param secret Le secret TOTP de l'utilisateur
     * @param code Le code à 6 chiffres saisi par l'utilisateur
     * @return true si le code est valide, false sinon
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.isBlank()) {
            return false;
        }
        return codeVerifier.isValidCode(secret, code);
    }
}