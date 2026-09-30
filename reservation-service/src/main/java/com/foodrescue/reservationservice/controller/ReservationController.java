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

    // ==========================================
    // Lecture
    // ==========================================

    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @GetMapping("/{id}")
    public Reservation getReservationById(@PathVariable Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    @GetMapping("/user/{consumerId}")
    public List<Reservation> getReservationsByUser(@PathVariable Long consumerId) {
        return reservationRepository.findByConsumerId(consumerId);
    }

    @GetMapping("/status/{status}")
    public List<Reservation> getReservationsByStatus(@PathVariable ReservationStatus status) {
        return reservationRepository.findByStatus(status);
    }

    // ==========================================
    // Création d'une réservation
    // ==========================================

    @PostMapping
    public Reservation createReservation(@RequestBody Reservation reservation) {
        // 1. Validations
        if (reservation.getOfferId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant de l'offre est obligatoire.");
        }
        if (reservation.getQuantity() == null || reservation.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être supérieure à zéro.");
        }
        if (reservation.getConsumerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant du consommateur est obligatoire.");
        }

        // 2. Récupérer les infos de l'offre (pour calculer totalPrice)
        OfferResponse offer;
        try {
            offer = offerClient.getOfferById(reservation.getOfferId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable : " + e.getMessage());
        }

        // 3. Vérifier le stock disponible
        if (offer.getRemainingQuantity() == null || offer.getRemainingQuantity() < reservation.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuffisant. Disponible : " + offer.getRemainingQuantity());
        }

        // 4. Décrémenter le stock dans offer-service
        offerClient.decrementStock(reservation.getOfferId(), reservation.getQuantity());

        // 5. Calculer le prix total
        BigDecimal totalPrice = offer.getDiscountedPrice()
                .multiply(BigDecimal.valueOf(reservation.getQuantity()));
        reservation.setTotalPrice(totalPrice);

        // 6. Définir les valeurs par défaut
        reservation.setStatus(ReservationStatus.CONFIRMED);  // Directement CONFIRMED
        reservation.setReservationDate(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);

        // 7. Récupérer l'email de l'utilisateur
        String recipientEmail = mailUsername;
        try {
            if (savedReservation.getConsumerId() != null) {
                UserProfileResponse userProfile = userServiceClient.getUserById(savedReservation.getConsumerId());
                if (userProfile != null && userProfile.getEmail() != null) {
                    recipientEmail = userProfile.getEmail();
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de récupérer l'email : " + e.getMessage());
        }

        // 8. Publier l'événement RabbitMQ
        publishEvent(savedReservation, "CREATED",
                "Nouvelle réservation confirmée pour " + savedReservation.getQuantity() + " portion(s).",
                recipientEmail);

        return savedReservation;
    }

    // ==========================================
    // Annulation
    // ==========================================

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

        // 1. Restaurer le stock dans offer-service
        offerClient.incrementStock(reservation.getOfferId(), reservation.getQuantity());

        // 2. Mettre à jour le statut + cancelledAt
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        Reservation updatedReservation = reservationRepository.save(reservation);

        // 3. Récupérer l'email
        String recipientEmail = mailUsername;
        try {
            if (updatedReservation.getConsumerId() != null) {
                UserProfileResponse userProfile = userServiceClient.getUserById(updatedReservation.getConsumerId());
                if (userProfile != null && userProfile.getEmail() != null) {
                    recipientEmail = userProfile.getEmail();
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de récupérer l'email : " + e.getMessage());
        }

        // 4. Publier l'événement
        publishEvent(updatedReservation, "CANCELLED",
                "La réservation a été annulée avec succès.", recipientEmail);

        return updatedReservation;
    }

    // ==========================================
    // Marquer comme COMPLETED (retrait effectué)
    // ==========================================

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

        // Récupérer l'email
        String recipientEmail = mailUsername;
        try {
            if (updatedReservation.getConsumerId() != null) {
                UserProfileResponse userProfile = userServiceClient.getUserById(updatedReservation.getConsumerId());
                if (userProfile != null && userProfile.getEmail() != null) {
                    recipientEmail = userProfile.getEmail();
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de récupérer l'email : " + e.getMessage());
        }

        publishEvent(updatedReservation, "COMPLETED",
                "Réservation complétée. Merci d'avoir sauvé de la nourriture !", recipientEmail);

        return updatedReservation;
    }

    // ==========================================
    // Méthode utilitaire pour publier les événements RabbitMQ
    // ==========================================

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