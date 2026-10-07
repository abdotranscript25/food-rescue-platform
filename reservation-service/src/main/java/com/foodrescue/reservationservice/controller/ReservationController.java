package com.foodrescue.reservationservice.controller;

import com.foodrescue.reservationservice.client.OfferClient;
import com.foodrescue.reservationservice.client.UserServiceClient;
import com.foodrescue.reservationservice.config.RabbitMQConfig;
import com.foodrescue.reservationservice.dto.OfferResponse;
import com.foodrescue.reservationservice.dto.ReservationEvent;
import com.foodrescue.reservationservice.dto.UserProfileResponse;
import com.foodrescue.reservationservice.entity.Reservation;
import com.foodrescue.reservationservice.entity.ReservationStatus;
import com.foodrescue.reservationservice.repository.ReservationRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
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

    @GetMapping("/user/{consumerId}")
    public List<Reservation> getReservationsByUser(@PathVariable Long consumerId) {
        return reservationRepository.findByConsumerId(consumerId);
    }

    @GetMapping("/status/{status}")
    public List<Reservation> getReservationsByStatus(@PathVariable ReservationStatus status) {
        return reservationRepository.findByStatus(status);
    }

    @GetMapping("/{id}")
    public Reservation getReservationById(@PathVariable Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    @PostMapping
    public Reservation createReservation(@RequestBody Reservation reservationRequest, Principal principal) {
        if (reservationRequest.getOfferId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant de l'offre est obligatoire.");
        }
        if (reservationRequest.getQuantity() == null || reservationRequest.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être supérieure à zéro.");
        }

        // 1. Récupération sécurisée de l'email depuis le Token JWT
        String userEmail = (principal != null) ? principal.getName() : mailUsername;

        // 2. Récupération dynamique et propre du vrai consumerId via le user-service par e-mail
        Long consumerId = reservationRequest.getConsumerId();
        String recipientEmail = userEmail;

        try {
            UserProfileResponse userProfile = userServiceClient.getUserByEmail(userEmail);
            if (userProfile != null) {
                if (userProfile.getId() != null) {
                    consumerId = userProfile.getId();
                }
                if (userProfile.getEmail() != null) {
                    recipientEmail = userProfile.getEmail();
                }
            }
        } catch (Exception e) {
            // Si le profil n'est pas trouvé par email, on bascule sur un fallback ou une erreur propre
            if (consumerId == null) {
                consumerId = 1L; // Fallback de secours si vraiment introuvable
            }
        }

        // 3. Vérification de l'offre et du stock
        OfferResponse offer;
        try {
            offer = offerClient.getOfferById(reservationRequest.getOfferId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable : " + e.getMessage());
        }

        if (offer.getRemainingQuantity() == null || offer.getRemainingQuantity() < reservationRequest.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuffisant. Disponible : " + offer.getRemainingQuantity());
        }

        offerClient.decrementStock(reservationRequest.getOfferId(), reservationRequest.getQuantity());

        BigDecimal totalPrice = offer.getDiscountedPrice()
                .multiply(BigDecimal.valueOf(reservationRequest.getQuantity()));

        // 4. Enregistrement de la réservation avec le bon ID
        Reservation reservation = new Reservation();
        reservation.setOfferId(reservationRequest.getOfferId());
        reservation.setQuantity(reservationRequest.getQuantity());
        reservation.setConsumerId(consumerId);
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setReservationDate(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);

        // 5. Publication de l'événement RabbitMQ avec le bon e-mail de notification
        publishEvent(savedReservation, "CREATED",
                "Nouvelle réservation confirmée pour " + savedReservation.getQuantity() + " portion(s).",
                recipientEmail);

        return savedReservation;
    }

    @PutMapping("/{id}/cancel")
    public Reservation cancelReservation(@PathVariable Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable."));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Réservation déjà annulée.");
        }

        if (reservation.getOfferId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offerId introuvable.");
        }

        offerClient.incrementStock(reservation.getOfferId(), reservation.getQuantity());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        Reservation updatedReservation = reservationRepository.save(reservation);

        String recipientEmail = mailUsername;
        try {
            if (updatedReservation.getConsumerId() != null) {
                UserProfileResponse userProfile = userServiceClient.getUserById(updatedReservation.getConsumerId());
                if (userProfile != null && userProfile.getEmail() != null) {
                    recipientEmail = userProfile.getEmail();
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de récupérer l'email pour l'annulation : " + e.getMessage());
        }

        publishEvent(updatedReservation, "CANCELLED",
                "La réservation a été annulée avec succès.", recipientEmail);

        return updatedReservation;
    }

    @PutMapping("/{id}/complete")
    public Reservation completeReservation(@PathVariable Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Réservation introuvable."));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les réservations CONFIRMED peuvent être marquées COMPLETED.");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        Reservation updatedReservation = reservationRepository.save(reservation);

        String recipientEmail = mailUsername;
        try {
            if (updatedReservation.getConsumerId() != null) {
                UserProfileResponse userProfile = userServiceClient.getUserById(updatedReservation.getConsumerId());
                if (userProfile != null && userProfile.getEmail() != null) {
                    recipientEmail = userProfile.getEmail();
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de récupérer l'email pour la complétion : " + e.getMessage());
        }

        publishEvent(updatedReservation, "COMPLETED",
                "Réservation complétée. Merci d'avoir sauvé de la nourriture !", recipientEmail);

        return updatedReservation;
    }

    private void publishEvent(Reservation reservation, String status, String message, String recipientEmail) {
        try {
            ReservationEvent event = new ReservationEvent(
                    reservation.getId() != null ? reservation.getId().toString() : "",
                    reservation.getOfferId() != null ? reservation.getOfferId().toString() : "",
                    recipientEmail,
                    status,
                    message
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY, event);
        } catch (Exception e) {
            System.err.println("Échec de l'envoi de l'événement RabbitMQ : " + e.getMessage());
        }
    }
}