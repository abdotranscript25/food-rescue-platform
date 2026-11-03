package com.foodrescue.reservationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.reservationservice.client.OfferClient;
import com.foodrescue.reservationservice.client.UserServiceClient;
import com.foodrescue.reservationservice.dto.OfferResponse;
import com.foodrescue.reservationservice.dto.UserProfileResponse;
import com.foodrescue.reservationservice.entity.Reservation;
import com.foodrescue.reservationservice.entity.ReservationStatus;
import com.foodrescue.reservationservice.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OfferClient offerClient;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        Mockito.reset(offerClient, userServiceClient, rabbitTemplate);
    }

    private Reservation createTestReservation() {
        Reservation reservation = Reservation.builder()
                .consumerId("consumer-uuid-123") // ⚠️ String (UUID)
                .offerId(1L)
                .quantity(2)
                .totalPrice(new BigDecimal("10.00"))
                .status(ReservationStatus.CONFIRMED)
                .reservationDate(LocalDateTime.now())
                .build();
        return reservationRepository.save(reservation);
    }

    private OfferResponse mockOffer(int remainingQty) {
        return OfferResponse.builder()
                .id(1L)
                .merchantId("merchant-uuid-123") // ⚠️ String (UUID)
                .productId(1L)
                .originalPrice(new BigDecimal("15.00"))
                .discountedPrice(new BigDecimal("5.00"))
                .quantity(10)
                .remainingQuantity(remainingQty)
                .status("AVAILABLE")
                .build();
    }

    // ==========================================
    // Test 1 : GET public → nécessite auth
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/reservations : doit retourner la liste")
    void getAllReservations_shouldReturnList() throws Exception {
        createTestReservation();

        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ==========================================
    // Test 2 : GET sans auth → 401/403
    // ==========================================
    @Test
    @DisplayName("GET /api/reservations sans auth : doit échouer")
    void getAllReservations_shouldFailWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().is4xxClientError());
    }

    // ==========================================
    // Test 3 : Création avec USER (happy path)
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("POST /api/reservations : création réussie")
    void createReservation_shouldWork() throws Exception {
        // Mocker les appels clients
        when(offerClient.getOfferById(1L)).thenReturn(mockOffer(10));

        // Le contrôleur appelle getUserByEmail d'abord
        when(userServiceClient.getUserByEmail(anyString()))
                .thenReturn(new UserProfileResponse("consumer-uuid-123", "Alice", "Dupont",
                        "alice@test.com", "+33612345678", "Paris", "Paris"));

        Reservation request = Reservation.builder()
                .consumerId("consumer-uuid-123") // ⚠️ String
                .offerId(1L)
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumerId").value("consumer-uuid-123"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPrice").value(10.00));

        // Vérifier les appels
        verify(offerClient, times(1)).decrementStock(1L, 2);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ==========================================
    // Test 4 : Création avec stock insuffisant
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("POST /api/reservations : stock insuffisant → 409")
    void createReservation_shouldFailIfInsufficientStock() throws Exception {
        when(offerClient.getOfferById(1L)).thenReturn(mockOffer(1));

        Reservation request = Reservation.builder()
                .consumerId("consumer-uuid-123")
                .offerId(1L)
                .quantity(5)
                .build();

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(offerClient, never()).decrementStock(anyLong(), anyInt());
    }

    // ==========================================
    // Test 5 : Création avec offerId manquant
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("POST /api/reservations : offerId manquant → 400")
    void createReservation_shouldFailIfMissingOfferId() throws Exception {
        Reservation request = Reservation.builder()
                .consumerId("consumer-uuid-123")
                .quantity(2)
                .build();

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // Test 6 : Création sans auth → 401/403
    // ==========================================
    @Test
    @DisplayName("POST /api/reservations sans auth : doit échouer")
    void createReservation_shouldFailWithoutAuth() throws Exception {
        Reservation request = Reservation.builder()
                .consumerId("consumer-uuid-123")
                .offerId(1L).quantity(2).build();

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // ==========================================
    // Test 7 : Annulation d'une réservation
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("PUT /api/reservations/{id}/cancel : doit annuler")
    void cancelReservation_shouldWork() throws Exception {
        Reservation reservation = createTestReservation();

        when(userServiceClient.getUserById(anyString()))
                .thenReturn(new UserProfileResponse("consumer-uuid-123", "Alice", "Dupont",
                        "alice@test.com", "+33612345678", "Paris", "Paris"));

        mockMvc.perform(put("/api/reservations/" + reservation.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());

        verify(offerClient, times(1)).incrementStock(1L, 2);
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ==========================================
    // Test 8 : Annulation d'une réservation déjà annulée
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("PUT /api/reservations/{id}/cancel : déjà annulée → 400")
    void cancelReservation_shouldFailIfAlreadyCancelled() throws Exception {
        Reservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        mockMvc.perform(put("/api/reservations/" + reservation.getId() + "/cancel"))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // Test 9 : Complétion d'une réservation
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("PUT /api/reservations/{id}/complete : doit compléter")
    void completeReservation_shouldWork() throws Exception {
        Reservation reservation = createTestReservation();

        when(userServiceClient.getUserById(anyString()))
                .thenReturn(new UserProfileResponse("consumer-uuid-123", "Alice", "Dupont",
                        "alice@test.com", "+33612345678", "Paris", "Paris"));

        mockMvc.perform(put("/api/reservations/" + reservation.getId() + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    // ==========================================
    // Test 10 : Complétion d'une réservation non-CONFIRMED
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("PUT /api/reservations/{id}/complete : pas CONFIRMED → 400")
    void completeReservation_shouldFailIfNotConfirmed() throws Exception {
        Reservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        mockMvc.perform(put("/api/reservations/" + reservation.getId() + "/complete"))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // Test 11 : GET par ID
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/reservations/{id} : doit retourner la réservation")
    void getReservationById_shouldReturn() throws Exception {
        Reservation reservation = createTestReservation();

        mockMvc.perform(get("/api/reservations/" + reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getId()));
    }

    // ==========================================
    // Test 12 : GET par consumerId
    // ==========================================
    @Test
    @WithMockUser(username = "alice@test.com", roles = "CONSUMER")
    @DisplayName("GET /api/reservations/user/{consumerId} : doit retourner la liste")
    void getReservationsByUser_shouldReturnList() throws Exception {
        createTestReservation();

        mockMvc.perform(get("/api/reservations/user/consumer-uuid-123")) // ⚠️ String (UUID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}