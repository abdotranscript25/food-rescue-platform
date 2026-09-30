package com.foodrescue.notificationservice.listener;

import com.foodrescue.notificationservice.config.RabbitMQConfig;
import com.foodrescue.notificationservice.dto.ReservationEvent;
import com.foodrescue.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleReservationEvent(ReservationEvent event) {
        log.info("Événement reçu : reservationId={}, status={}",
                event.getReservationId(), event.getStatus());

        // ==========================================
        // AJOUT : gestion des cas invalides
        // ==========================================
        if (event.getStatus() == null) {
            log.warn("Statut null dans l'événement, ignoré.");
            return;
        }

        if (event.getUserEmail() == null) {
            log.warn("Email destinataire null, envoi annulé.");
            return;
        }

        String subject;
        String content;

        switch (event.getStatus()) {
            case "CREATED" -> {
                subject = "✅ Réservation confirmée - Food Rescue";
                content = String.format(
                        "Bonjour,\n\nVotre réservation (ID: %s) pour l'offre (ID: %s) a bien été confirmée.\n\n%s\n\nMerci de venir la récupérer dans les temps !\n\nL'équipe Food Rescue",
                        event.getReservationId(), event.getOfferId(), event.getMessage()
                );
            }
            case "CANCELLED" -> {
                subject = "❌ Réservation annulée - Food Rescue";
                content = String.format(
                        "Bonjour,\n\nVotre réservation (ID: %s) pour l'offre (ID: %s) a été annulée.\n\n%s\n\nL'équipe Food Rescue",
                        event.getReservationId(), event.getOfferId(), event.getMessage()
                );
            }
            case "COMPLETED" -> {
                subject = "🎉 Merci d'avoir sauvé de la nourriture ! - Food Rescue";
                content = String.format(
                        "Bonjour,\n\nVotre réservation (ID: %s) a été complétée avec succès.\n\n%s\n\nMerci de contribuer à la lutte contre le gaspillage alimentaire !\n\nL'équipe Food Rescue",
                        event.getReservationId(), event.getMessage()
                );
            }
            default -> {
                subject = "Notification de Réservation - Food Rescue";
                content = String.format(
                        "Bonjour,\n\nMise à jour de votre réservation (ID: %s) : %s\n\n%s\n\nL'équipe Food Rescue",
                        event.getReservationId(), event.getStatus(), event.getMessage()
                );
            }
        }

        emailService.sendEmail(event.getUserEmail(), subject, content);
    }
}