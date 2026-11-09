package com.foodrescue.paymentservice.service;

import com.foodrescue.paymentservice.client.ReservationClient;
import com.foodrescue.paymentservice.model.Payment;
import com.foodrescue.paymentservice.model.PaymentStatus;
import com.foodrescue.paymentservice.repository.PaymentRepository;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    // Test 1 : Paiement réussi (Création session Stripe)
    // ==========================================
    @Test
    @DisplayName("processPayment : doit créer un paiement avec succès")
    void processPayment_shouldCreateSuccessPayment() {
        // Mocker la réponse de Feign (réservation trouvée)
        when(reservationClient.getReservationById(1L)).thenReturn(new Object());

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_mock123");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/mock_url");

            // CORRECTION : Spécifier le type pour lever l'ambiguïté
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);

            String url = paymentService.processPayment(1L, 100L, 50.0, "CREDIT_CARD");

            assertEquals("https://checkout.stripe.com/mock_url", url);

            // Vérifier que le paiement a bien été sauvegardé avec le statut PENDING
            Optional<Payment> savedPayment = paymentRepository.findByReservationId(1L);
            assertTrue(savedPayment.isPresent());
            assertEquals(1L, savedPayment.get().getReservationId());
            assertEquals(100L, savedPayment.get().getClientId());
            assertEquals(50.0, savedPayment.get().getAmount());
            assertEquals("MAD", savedPayment.get().getCurrency());
            assertEquals(PaymentStatus.PENDING, savedPayment.get().getStatus()); // PENDING maintenant
            assertEquals("CREDIT_CARD", savedPayment.get().getPaymentMethod());
            assertEquals("cs_test_mock123", savedPayment.get().getStripeSessionId());
            assertNotNull(savedPayment.get().getCreatedAt());
        }
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