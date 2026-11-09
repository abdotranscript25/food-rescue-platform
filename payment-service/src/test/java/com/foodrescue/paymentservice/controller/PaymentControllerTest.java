package com.foodrescue.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationClient reservationClient;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        Mockito.reset(reservationClient);
    }

    // ==========================================
    // Test 1 : POST /api/payments (Authentifié)
    // ==========================================
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/payments : doit traiter le paiement")
    void processPayment_shouldReturnPayment() throws Exception {
        when(reservationClient.getReservationById(anyLong())).thenReturn(new Object());

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_mock123");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/mock");

            // CORRECTION : Spécifier le type pour lever l'ambiguïté
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(mockSession);

            Map<String, Object> request = new HashMap<>();
            request.put("reservationId", 10L);
            request.put("clientId", 500L);
            request.put("amount", 150.0);
            request.put("paymentMethod", "CREDIT_CARD");

            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reservationId").value(10))
                    .andExpect(jsonPath("$.amount").value(150.0))
                    .andExpect(jsonPath("$.currency").value("MAD"))
                    .andExpect(jsonPath("$.status").value("PENDING")) // Changé en PENDING
                    .andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.com/mock")); // Ajout de l'URL
        }
    }

    // ==========================================
    // Test 2 : POST sans Auth → 401/403
    // ==========================================
    @Test
    @DisplayName("POST /api/payments sans auth : doit échouer")
    void processPayment_shouldFailWithoutAuth() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("reservationId", 1L);
        request.put("clientId", 1L);
        request.put("amount", 100.0);
        request.put("paymentMethod", "CARD");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ==========================================
    // Test 3 : POST avec erreur Feign
    // ==========================================
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/payments : erreur Feign → 500")
    void processPayment_shouldReturn500IfFeignFails() throws Exception {
        when(reservationClient.getReservationById(anyLong())).thenThrow(new RuntimeException("Service down"));

        Map<String, Object> request = new HashMap<>();
        request.put("reservationId", 1L);
        request.put("clientId", 1L);
        request.put("amount", 100.0);
        request.put("paymentMethod", "CARD");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // 500
    }

    // ==========================================
    // Test 4 : GET /api/payments/reservation/{id} (Succès)
    // ==========================================
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/payments/reservation/{id} : doit retourner le paiement")
    void getByReservation_shouldReturnPayment() throws Exception {
        Payment payment = Payment.builder()
                .reservationId(20L)
                .clientId(300L)
                .amount(99.0)
                .currency("MAD")
                .status(PaymentStatus.SUCCESS)
                .paymentMethod("PAYPAL")
                .build();
        paymentRepository.save(payment);

        mockMvc.perform(get("/api/payments/reservation/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservationId").value(20))
                .andExpect(jsonPath("$.clientId").value(300));
    }

    // ==========================================
    // Test 5 : GET /api/payments/reservation/{id} (Non trouvé)
    // ==========================================
    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/payments/reservation/{id} : non trouvé → 404")
    void getByReservation_shouldReturn404IfNotFound() throws Exception {
        mockMvc.perform(get("/api/payments/reservation/999"))
                .andExpect(status().isNotFound()); // 404
    }
}