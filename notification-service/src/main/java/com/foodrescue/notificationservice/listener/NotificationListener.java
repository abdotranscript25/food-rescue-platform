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
        log.info("Événement de réservation reçu depuis RabbitMQ pour la réservation ID : {}", event.getReservationId());

        String subject = "Notification de Réservation - Food Rescue";
        String content = String.format(
                "Bonjour,\n\nVotre réservation (ID: %s) pour l'offre (ID: %s) a changé de statut : %s.\n\nDétails : %s\n\nMerci d'utiliser Food Rescue !",
                event.getReservationId(),
                event.getOfferId(),
                event.getStatus(),
                event.getMessage()
        );

        emailService.sendEmail(event.getUserEmail(), subject, content);
    }
}