package com.foodrescue.notificationservice.listener;

import com.foodrescue.notificationservice.dto.ReservationEvent;
import com.foodrescue.notificationservice.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationListenerTest {

    @Autowired
    private NotificationListener notificationListener;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        Mockito.reset(emailService);
    }

    // ==========================================
    // Test 1 : Réception d'un événement CREATED
    // ==========================================
    @Test
    @DisplayName("handleReservationEvent : doit envoyer un email pour CREATED")
    void handleEvent_shouldSendEmailForCreated() {
        ReservationEvent event = new ReservationEvent(
                "1", "10", "alice@test.com",
                "CREATED", "Nouvelle réservation créée");

        notificationListener.handleReservationEvent(event);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService, times(1)).sendEmail(
                toCaptor.capture(),
                subjectCaptor.capture(),
                bodyCaptor.capture()
        );

        assertEquals("alice@test.com", toCaptor.getValue());
        // CORRECTION : le sujet contient "confirm" (français)
        assertTrue(subjectCaptor.getValue().toLowerCase().contains("confirm"));
        // CORRECTION : le body contient "confirm"
        assertTrue(bodyCaptor.getValue().toLowerCase().contains("confirm"));
    }

    // ==========================================
    // Test 2 : Réception d'un événement CANCELLED
    // ==========================================
    @Test
    @DisplayName("handleReservationEvent : doit envoyer un email pour CANCELLED")
    void handleEvent_shouldSendEmailForCancelled() {
        ReservationEvent event = new ReservationEvent(
                "2", "20", "bob@test.com",
                "CANCELLED", "Réservation annulée");

        notificationListener.handleReservationEvent(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService, times(1)).sendEmail(
                eq("bob@test.com"),
                anyString(),
                bodyCaptor.capture()
        );

        // CORRECTION : le body contient "annul"
        assertTrue(bodyCaptor.getValue().toLowerCase().contains("annul"));
    }

    // ==========================================
    // Test 3 : Réception d'un événement COMPLETED
    // ==========================================
    @Test
    @DisplayName("handleReservationEvent : doit envoyer un email pour COMPLETED")
    void handleEvent_shouldSendEmailForCompleted() {
        ReservationEvent event = new ReservationEvent(
                "3", "30", "charlie@test.com",
                "COMPLETED", "Réservation complétée");

        notificationListener.handleReservationEvent(event);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService, times(1)).sendEmail(
                eq("charlie@test.com"),
                anyString(),
                bodyCaptor.capture()
        );

        // CORRECTION : le body contient "compl"
        assertTrue(bodyCaptor.getValue().toLowerCase().contains("compl"));
    }

    // ==========================================
    // Test 4 : Ne doit pas planter si email est null
    // ==========================================
    @Test
    @DisplayName("handleReservationEvent : ne doit pas planter avec email null")
    void handleEvent_shouldHandleNullEmail() {
        ReservationEvent event = new ReservationEvent(
                "4", "40", null, "CREATED", "Test");

        assertDoesNotThrow(() -> notificationListener.handleReservationEvent(event));

        // CORRECTION : aucun email envoyé
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    // ==========================================
    // Test 5 : Ne doit pas planter si status est null
    // ==========================================
    @Test
    @DisplayName("handleReservationEvent : ne doit pas planter avec status null")
    void handleEvent_shouldHandleNullStatus() {
        ReservationEvent event = new ReservationEvent();
        // status reste null

        assertDoesNotThrow(() -> notificationListener.handleReservationEvent(event));

        // CORRECTION : aucun email envoyé
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
}