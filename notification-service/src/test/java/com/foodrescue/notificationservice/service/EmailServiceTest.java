package com.foodrescue.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        Mockito.reset(mailSender);
    }

    // ==========================================
    // Test 1 : Envoi d'un email réussi
    // ==========================================
    @Test
    @DisplayName("sendEmail : doit envoyer un email via JavaMailSender")
    void sendEmail_shouldSendViaMailSender() {
        // Arrange
        String to = "alice@test.com";
        String subject = "Test Subject";
        String body = "Test Body";

        // Act
        emailService.sendEmail(to, subject, body);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertNotNull(capturedMessage.getTo());
        assertEquals(to, capturedMessage.getTo()[0]);
        assertEquals(subject, capturedMessage.getSubject());
        assertEquals(body, capturedMessage.getText());
    }

    // ==========================================
    // Test 2 : Fallback si l'envoi échoue
    // ==========================================
    @Test
    @DisplayName("sendEmail : doit simuler l'envoi si SMTP échoue (mode dégradé)")
    void sendEmail_shouldFallbackOnSmtpFailure() {
        // Arrange : faire planter l'envoi
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act + Assert : ne doit PAS lancer d'exception
        assertDoesNotThrow(() ->
                emailService.sendEmail("bob@test.com", "Test", "Content")
        );

        // Vérifier que send a bien été tenté
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // ==========================================
    // Test 3 : Envoi avec email null (cas limite)
    // ==========================================
    @Test
    @DisplayName("sendEmail : ne doit pas planter avec un body vide")
    void sendEmail_shouldHandleEmptyBody() {
        emailService.sendEmail("test@test.com", "Subject", "");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}