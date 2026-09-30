package com.foodrescue.authservice.util;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import org.springframework.stereotype.Component;

/**
 * Helper de test pour générer des codes TOTP valides.
 * NE DOIT PAS être utilisé en production.
 *
 * Ce fichier est dans src/test → il n'est PAS inclus dans le JAR final.
 * Il est uniquement utilisé pendant l'exécution des tests unitaires.
 */
@Component
public class TestMfaHelper {

    private final DefaultCodeGenerator generator =
            new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);

    /**
     * Génère le code TOTP courant pour un secret donné.
     * Le code généré est identique à celui de Google Authenticator.
     *
     * @param secret Le secret TOTP (Base32, ex: "JBSWY3DPEHPK3PXP")
     * @param period La période en secondes (généralement 30)
     * @return Le code à 6 chiffres
     */
    public String generateCurrentCode(String secret, int period) {
        try {
            long timeStep = System.currentTimeMillis() / 1000 / period;
            return generator.generate(secret, timeStep);
        } catch (CodeGenerationException e) {
            throw new RuntimeException("Erreur génération code TOTP", e);
        }
    }
}