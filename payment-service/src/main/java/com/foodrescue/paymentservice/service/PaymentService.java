package com.foodrescue.paymentservice.service;

import com.foodrescue.paymentservice.client.ReservationClient;
import com.foodrescue.paymentservice.model.Payment;
import com.foodrescue.paymentservice.model.PaymentStatus;
import com.foodrescue.paymentservice.repository.PaymentRepository;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationClient reservationClient;

    public PaymentService(PaymentRepository paymentRepository, ReservationClient reservationClient) {
        this.paymentRepository = paymentRepository;
        this.reservationClient = reservationClient;
    }

    // Retourne l'URL Stripe (String) au lieu de l'entité Payment
    public String processPayment(Long reservationId, Long clientId, Double amount, String paymentMethod) {
        Object reservation;

        try {
            reservation = reservationClient.getReservationById(reservationId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de communication avec le service de réservation : " + e.getMessage());
        }

        if (reservation == null) {
            throw new RuntimeException("Réservation introuvable, impossible d'effectuer le paiement.");
        }

        try {
            // Création de la session Stripe
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:5173/payment-success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:5173/payment-cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("mad") // Doit être en minuscules
                                                    .setUnitAmount((long) (amount * 100)) // Stripe utilise les centimes
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Réservation Food Rescue #" + reservationId)
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);

            // Sauvegarde en PENDING
            Payment payment = Payment.builder()
                    .reservationId(reservationId)
                    .clientId(clientId)
                    .amount(amount)
                    .currency("MAD")
                    .paymentMethod(paymentMethod)
                    .status(PaymentStatus.PENDING)
                    .stripeSessionId(session.getId())
                    .build();

            paymentRepository.save(payment);

            return session.getUrl(); // On renvoie l'URL au frontend

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la session Stripe : " + e.getMessage());
        }
    }

    public Payment getPaymentByReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new RuntimeException("Aucun paiement trouvé pour cette réservation"));
    }

    // Méthode appelée par le Webhook
    public void confirmPayment(String stripeSessionId) {
        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(stripeSessionId);

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            // ⬇️ APPEL FEIGN VERS LE RESERVATION SERVICE
            try {
                reservationClient.confirmReservation(payment.getReservationId());
                System.out.println("✅ Réservation " + payment.getReservationId() + " notifiée avec succès au reservation-service.");
            } catch (Exception e) {
                // On catch l'erreur pour que Stripe reçoive quand même un 200 OK
                System.err.println("❌ Erreur Feign lors de la confirmation de la réservation : " + e.getMessage());
            }
        }
    }
}