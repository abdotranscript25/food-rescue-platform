package com.foodrescue.reservationservice.controller;

import com.foodrescue.reservationservice.client.OfferClient;
import com.foodrescue.reservationservice.client.UserServiceClient;
import com.foodrescue.reservationservice.config.RabbitMQConfig;
import com.foodrescue.reservationservice.dto.ReservationEvent;
import com.foodrescue.reservationservice.dto.UserProfileResponse;
import com.foodrescue.reservationservice.entity.Reservation;
import com.foodrescue.reservationservice.repository.ReservationRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OfferClient offerClient;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${MAIL_USERNAME}")
    private String mailUsername;

    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @PostMapping
    public Reservation createReservation(@RequestBody Reservation reservation) {
        // Validation indispensable pour éviter le double slash (//) dans l'URL Feign
        if (reservation.getOfferId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant de l'offre (offerId) est obligatoire.");
        }
        if (reservation.getQuantity() == null || reservation.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être supérieure à zéro.");
        }

        // 1. Appeler le offer-service pour décrémenter le stock
        offerClient.decrementStock(reservation.getOfferId(), reservation.getQuantity());

        // 2. Sauvegarder la réservation
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        reservation.setReservationDate(LocalDateTime.now());
        Reservation savedReservation = reservationRepository.save(reservation);

        // 3. Récupérer l'e-mail de l'utilisateur auprès de user-service
        String recipientEmail = mailUsername;
        try {
            if (savedReservation.getUserId() != null) {
                UserProfileResponse userProfile = userServiceClient.getUserById(savedReservation.getUserId());
                if (userProfile != null && userProfile.getEmail() != null) {
                    recipientEmail = userProfile.getEmail();
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de récupérer l'e-mail de l'utilisateur depuis user-service : " + e.getMessage());
        }

        // 4. Envoi de l'événement asynchrone à RabbitMQ
        try {
            ReservationEvent event = new ReservationEvent(
                    savedReservation.getId() != null ? savedReservation.getId().toString() : "",
                    savedReservation.getOfferId() != null ? savedReservation.getOfferId().toString() : "",
                    recipientEmail,
                    "CREATED",
                    "Nouvelle réservation effectuée avec succès pour une quantité de " + savedReservation.getQuantity() + "."
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event);
        } catch (Exception e) {
            System.err.println("Échec de l'envoi de l'événement RabbitMQ : " + e.getMessage());
        }

        return savedReservation;
    }

    @GetMapping("/{id}")
    public Reservation getReservationById(@PathVariable Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    @PutMapping("/{id}/cancel")
    public Reservation cancelReservation(@PathVariable Long id) {
        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation != null) {
            if (reservation.getOfferId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'annuler : offerId est introuvable pour cette réservation.");
            }

            // 1. Restaurer le stock dans offer-service
            offerClient.incrementStock(reservation.getOfferId(), reservation.getQuantity());

            // 2. Mettre à jour le statut
            reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
            Reservation updatedReservation = reservationRepository.save(reservation);

            // 3. Récupérer l'e-mail de l'utilisateur auprès de user-service
            String recipientEmail = mailUsername;
            try {
                if (updatedReservation.getUserId() != null) {
                    UserProfileResponse userProfile = userServiceClient.getUserById(updatedReservation.getUserId());
                    if (userProfile != null && userProfile.getEmail() != null) {
                        recipientEmail = userProfile.getEmail();
                    }
                }
            } catch (Exception e) {
                System.err.println("Impossible de récupérer l'e-mail de l'utilisateur depuis user-service : " + e.getMessage());
            }

            // 4. Envoi de l'événement asynchrone à RabbitMQ
            try {
                ReservationEvent event = new ReservationEvent(
                        updatedReservation.getId().toString(),
                        updatedReservation.getOfferId() != null ? updatedReservation.getOfferId().toString() : "",
                        recipientEmail,
                        "CANCELLED",
                        "La réservation a été annulée avec succès."
                );
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event);
            } catch (Exception e) {
                System.err.println("Échec de l'envoi de l'événement RabbitMQ : " + e.getMessage());
            }

            return updatedReservation;
        }
        return null;
    }

    @GetMapping("/user/{userId}")
    public List<Reservation> getReservationsByUser(@PathVariable Long userId) {
        return reservationRepository.findByUserId(userId);
    }
}