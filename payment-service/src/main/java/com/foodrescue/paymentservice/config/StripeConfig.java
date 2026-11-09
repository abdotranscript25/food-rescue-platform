package com.foodrescue.paymentservice.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Value("${stripe.api.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        // Initialise la clé API globalement pour le SDK Stripe
        Stripe.apiKey = stripeSecretKey;
    }
}