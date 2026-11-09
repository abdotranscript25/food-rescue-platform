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
        when(reservationClient.getReservationById(1L)).thenReturn(new Object());

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_mock123");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/mock_url");

            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);

            String url = paymentService.processPayment(1L, 100L, 50.0, "CREDIT_CARD");

            assertEquals("https://checkout.stripe.com/mock_url", url);

            Optional<Payment> savedPayment = paymentRepository.findByReservationId(1L);
            assertTrue(savedPayment.isPresent());
            assertEquals(PaymentStatus.PENDING, savedPayment.get().getStatus());
            assertEquals("cs_test_mock123", savedPayment.get().getStripeSessionId());
        }
    }

    // ==========================================
    // Test 2 : Réservation introuvable
    // ==========================================
    @Test
    @DisplayName("processPayment : doit échouer si la réservation n'existe pas")
    void processPayment_shouldFailIfReservationNotFound() {
        when(reservationClient.getReservationById(999L)).thenReturn(null);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            paymentService.processPayment(999L, 100L, 50.0, "CREDIT_CARD");
        });

        assertTrue(exception.getMessage().contains("introuvable"));
        assertEquals(0, paymentRepository.count());
    }

    // ==========================================
    // Test 3 : Erreur de communication Feign
    // ==========================================
    @Test
    @DisplayName("processPayment : doit échouer si Feign plante")
    void processPayment_shouldFailIfFeignThrowsException() {
        when(reservationClient.getReservationById(anyLong())).thenThrow(new RuntimeException("Timeout"));

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
                .reservationId(5L).clientId(200L).amount(100.0)
                .currency("MAD").status(PaymentStatus.SUCCESS).paymentMethod("PAYPAL").build();
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

    // ==========================================
    // 🆕 NOUVEAUX TESTS POUR LE WEBHOOK (Coverage)
    // ==========================================
    @Test
    @DisplayName("confirmPayment : doit mettre à jour le statut et appeler Feign")
    void confirmPayment_shouldUpdateStatusAndCallFeign() {
        Payment payment = Payment.builder()
                .reservationId(1L).clientId(100L).amount(50.0)
                .currency("MAD").paymentMethod("CARD").status(PaymentStatus.PENDING)
                .stripeSessionId("cs_test_123").build();
        paymentRepository.save(payment);

        // On ne fait rien quand Feign est appelé (succès)
        doNothing().when(reservationClient).confirmReservation(1L);

        paymentService.confirmPayment("cs_test_123");

        Payment updatedPayment = paymentRepository.findByReservationId(1L).get();
        assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());
        verify(reservationClient, times(1)).confirmReservation(1L);
    }

    @Test
    @DisplayName("confirmPayment : doit gérer l'erreur Feign sans crasher")
    void confirmPayment_shouldHandleFeignException() {
        Payment payment = Payment.builder()
                .reservationId(1L).clientId(100L).amount(50.0)
                .currency("MAD").paymentMethod("CARD").status(PaymentStatus.PENDING)
                .stripeSessionId("cs_test_123").build();
        paymentRepository.save(payment);

        // On simule une erreur Feign (ex: 403 Forbidden)
        doThrow(new RuntimeException("403 Forbidden")).when(reservationClient).confirmReservation(1L);

        // Ne doit pas lancer d'exception (car le catch dans le service gère l'erreur)
        assertDoesNotThrow(() -> paymentService.confirmPayment("cs_test_123"));

        Payment updatedPayment = paymentRepository.findByReservationId(1L).get();
        assertEquals(PaymentStatus.SUCCESS, updatedPayment.getStatus());
    }
}