package com.foodrescue.paymentservice.service;

import com.foodrescue.paymentservice.client.ReservationClient;
import com.foodrescue.paymentservice.model.Payment;
import com.foodrescue.paymentservice.model.PaymentStatus;
import com.foodrescue.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private ReservationClient reservationClient;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        Mockito.reset(reservationClient);
    }

    // ==========================================
    // Test 1 : Paiement réussi
    // ==========================================
    @Test
    @DisplayName("processPayment : doit créer un paiement avec succès")
    void processPayment_shouldCreateSuccessPayment() {
        // Mocker la réponse de Feign (réservation trouvée)
        when(reservationClient.getReservationById(1L)).thenReturn(new Object());

        Payment payment = paymentService.processPayment(1L, 100L, 50.0, "CREDIT_CARD");

        assertNotNull(payment.getId());
        assertEquals(1L, payment.getReservationId());
        assertEquals(100L, payment.getClientId());
        assertEquals(50.0, payment.getAmount());
        assertEquals("MAD", payment.getCurrency());
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("CREDIT_CARD", payment.getPaymentMethod());
        assertNotNull(payment.getCreatedAt());

        // Vérifier que le paiement a bien été sauvegardé
        Optional<Payment> savedPayment = paymentRepository.findByReservationId(1L);
        assertTrue(savedPayment.isPresent());
    }

    // ==========================================
    // Test 2 : Réservation introuvable
    // ==========================================
    @Test
    @DisplayName("processPayment : doit échouer si la réservation n'existe pas")
    void processPayment_shouldFailIfReservationNotFound() {
        // Mocker Feign pour retourner null
        when(reservationClient.getReservationById(999L)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(999L, 100L, 50.0, "CREDIT_CARD");
        });

        assertTrue(exception.getMessage().contains("introuvable"));
        // S'assurer qu'aucun paiement n'a été sauvegardé
        assertEquals(0, paymentRepository.count());
    }

    // ==========================================
    // Test 3 : Erreur de communication Feign
    // ==========================================
    @Test
    @DisplayName("processPayment : doit échouer si Feign plante")
    void processPayment_shouldFailIfFeignThrowsException() {
        when(reservationClient.getReservationById(anyLong()))
                .thenThrow(new RuntimeException("Timeout"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(1L, 100L, 50.0, "CREDIT_CARD");
        });

        assertTrue(exception.getMessage().contains("Erreur de communication"));
    }

    // ==========================================
    // Test 4 : Récupération de paiement par Réservation
    // ==========================================
    @Test
    @DisplayName("getPaymentByReservation : doit retourner le paiement")
    void getPaymentByReservation_shouldReturnPayment() {
        Payment payment = Payment.builder()
                .reservationId(5L)
                .clientId(200L)
                .amount(100.0)
                .currency("MAD")
                .status(PaymentStatus.SUCCESS)
                .paymentMethod("PAYPAL")
                .build();
        paymentRepository.save(payment);

        Payment found = paymentService.getPaymentByReservation(5L);

        assertNotNull(found);
        assertEquals(200L, found.getClientId());
    }

    // ==========================================
    // Test 5 : Paiement introuvable
    // ==========================================
    @Test
    @DisplayName("getPaymentByReservation : doit échouer si aucun paiement")
    void getPaymentByReservation_shouldThrowIfNotFound() {
        assertThrows(RuntimeException.class, () -> paymentService.getPaymentByReservation(999L));
    }
}