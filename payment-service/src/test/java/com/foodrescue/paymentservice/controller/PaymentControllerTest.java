package com.foodrescue.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.paymentservice.client.ReservationClient;
import com.foodrescue.paymentservice.model.Payment;
import com.foodrescue.paymentservice.model.PaymentStatus;
import com.foodrescue.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.anyLong;
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
        // Mocker Feign
        when(reservationClient.getReservationById(anyLong())).thenReturn(new Object());

        // Construire la requête JSON
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
                .andExpect(jsonPath("$.status").value("SUCCESS"));
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
    // Test 3 : GET /api/payments/reservation/{id}
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
}