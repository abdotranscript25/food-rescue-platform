package com.foodrescue.paymentservice.service;

import com.foodrescue.paymentservice.client.ReservationClient;
import com.foodrescue.paymentservice.model.Payment;
import com.foodrescue.paymentservice.model.PaymentStatus;
import com.foodrescue.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationClient reservationClient;

    public PaymentService(PaymentRepository paymentRepository, ReservationClient reservationClient) {
        this.paymentRepository = paymentRepository;
        this.reservationClient = reservationClient;
    }

    public Payment processPayment(Long reservationId, Long clientId, Double amount, String paymentMethod) {
        Object reservation;

        // 1. Vérification de l'existence de la réservation via Feign Client
        try {
            reservation = reservationClient.getReservationById(reservationId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de communication avec le service de réservation : " + e.getMessage());
        }

        // On vérifie le null EN DEHORS du try-catch
        if (reservation == null) {
            throw new RuntimeException("Réservation introuvable, impossible d'effectuer le paiement.");
        }

        // 2. Traitement et sauvegarde du paiement
        Payment payment = Payment.builder()
                .reservationId(reservationId)
                .clientId(clientId)
                .amount(amount)
                .currency("MAD")
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.SUCCESS)
                .build();

        return paymentRepository.save(payment);
    }

    public Payment getPaymentByReservation(Long reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new RuntimeException("Aucun paiement trouvé pour cette réservation"));
    }
}