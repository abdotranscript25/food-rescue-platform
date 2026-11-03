package com.foodrescue.reservationservice.controller;

import com.foodrescue.reservationservice.client.OfferClient;
import com.foodrescue.reservationservice.client.UserServiceClient;
import com.foodrescue.reservationservice.config.RabbitMQConfig;
import com.foodrescue.reservationservice.dto.OfferResponse;
import com.foodrescue.reservationservice.dto.ReservationEnrichedResponse;
import com.foodrescue.reservationservice.dto.ReservationEvent;
import com.foodrescue.reservationservice.dto.UserProfileResponse;
import com.foodrescue.reservationservice.entity.Reservation;
import com.foodrescue.reservationservice.entity.ReservationStatus;
import com.foodrescue.reservationservice.repository.ReservationRepository;
import com.foodrescue.reservationservice.security.JwtService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private JwtService jwtService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${MAIL_USERNAME}")
    private String mailUsername;

    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @GetMapping("/user/{consumerId}")
    public List<ReservationEnrichedResponse> getReservationsByUser(@PathVariable String consumerId) {
        try {
            List<Reservation> reservations = reservationRepository.findByConsumerId(consumerId);
            if (reservations == null || reservations.isEmpty()) {
                return List.of();
            }

            Set<Long> offerIds = reservations.stream()
                    .map(Reservation::getOfferId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());

            Map<Long, OfferResponse> offerMap = new HashMap<>();
            for (Long offerId : offerIds) {
                try {
                    OfferResponse offer = offerClient.getOfferById(offerId);
                    if (offer != null) {
                        offerMap.put(offerId, offer);
                    }
                } catch (Exception e) {
                    System.err.println("Impossible de récupérer l'offre ID " + offerId + " : " + e.getMessage());
                }
            }

            List<ReservationEnrichedResponse> enrichedList = new ArrayList<>();
            for (Reservation r : reservations) {
                ReservationEnrichedResponse dto = new ReservationEnrichedResponse();
                dto.setId(r.getId());
                dto.setQuantity(r.getQuantity());
                dto.setTotalPrice(r.getTotalPrice());
                dto.setStatus(r.getStatus());
                dto.setReservationDate(r.getReservationDate());
                dto.setCancelledAt(r.getCancelledAt());

                if (r.getOfferId() != null) {
                    dto.setOffer(offerMap.get(r.getOfferId()));
                }

                if (r.getConsumerId() != null) {
                    try {
                        UserProfileResponse userProfile = userServiceClient.getUserById(r.getConsumerId());
                        dto.setConsumer(userProfile);
                    } catch (Exception e) {
                        dto.setConsumer(null);
                    }
                }

                enrichedList.add(dto);
            }

            return enrichedList;

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la récupération des réservations de l'utilisateur : " + e.getMessage());
        }
    }

    @GetMapping("/merchant/{merchantId}")
    public List<ReservationEnrichedResponse> getReservationsByMerchant(@PathVariable String merchantId) {
        try {
            List<OfferResponse> merchantOffers = offerClient.getOffersByMerchant(merchantId);
            if (merchantOffers == null || merchantOffers.isEmpty()) {
                return List.of();
            }

            Map<Long, OfferResponse> offerMap = merchantOffers.stream()
                    .collect(Collectors.toMap(OfferResponse::getId, o -> o));

            List<Long> offerIds = new ArrayList<>(offerMap.keySet());

            List<Reservation> reservations = reservationRepository.findAll().stream()
                    .filter(r -> r.getOfferId() != null && offerIds.contains(r.getOfferId()))
                    .collect(Collectors.toList());

            List<ReservationEnrichedResponse> enrichedList = new ArrayList<>();
            for (Reservation r : reservations) {
                ReservationEnrichedResponse dto = new ReservationEnrichedResponse();
                dto.setId(r.getId());
                dto.setQuantity(r.getQuantity());
                dto.setTotalPrice(r.getTotalPrice());
                dto.setStatus(r.getStatus());
                dto.setReservationDate(r.getReservationDate());
                dto.setCancelledAt(r.getCancelledAt());
                dto.setOffer(offerMap.get(r.getOfferId()));

                if (r.getConsumerId() != null) {
                    try {
                        UserProfileResponse userProfile = userServiceClient.getUserById(r.getConsumerId());
                        dto.setConsumer(userProfile);
                    } catch (Exception e) {
                        dto.setConsumer(null);
                    }
                }

                enrichedList.add(dto);
            }

            return enrichedList;

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la récupération des réservations du marchand : " + e.getMessage());
        }
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
    public Reservation createReservation(
            @RequestBody Reservation reservationRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            Principal principal
    ) {
        if (reservationRequest.getOfferId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant de l'offre est obligatoire.");
        }
        if (reservationRequest.getQuantity() == null || reservationRequest.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être supérieure à zéro.");
        }

        String userEmail = (principal != null) ? principal.getName() : mailUsername;
        String consumerId = reservationRequest.getConsumerId();
        String recipientEmail = userEmail;

        // Extraction propre de l'UUID du consommateur depuis le JWT si présent
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String tokenUuid = jwtService.extractUserId(token);
                if (tokenUuid != null) {
                    consumerId = tokenUuid;
                }
            } catch (Exception ignored) {}
        }

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
            if (consumerId == null) {
                consumerId = "default-uuid";
            }
        }

        OfferResponse offer;
        try {
            offer = offerClient.getOfferById(reservationRequest.getOfferId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable : " + e.getMessage());
        }

        if (offer.getStatus() != null && !"AVAILABLE".equalsIgnoreCase(offer.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de réserver : cette offre n'est plus disponible.");
        }
        if (offer.getExpiresAt() != null && offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de réserver : cette offre a expiré.");
        }

        if (offer.getRemainingQuantity() == null || offer.getRemainingQuantity() < reservationRequest.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuffisant. Disponible : " + offer.getRemainingQuantity());
        }

        offerClient.decrementStock(reservationRequest.getOfferId(), reservationRequest.getQuantity());

        BigDecimal totalPrice = offer.getDiscountedPrice()
                .multiply(BigDecimal.valueOf(reservationRequest.getQuantity()));

        Reservation reservation = new Reservation();
        reservation.setOfferId(reservationRequest.getOfferId());
        reservation.setQuantity(reservationRequest.getQuantity());
        reservation.setConsumerId(consumerId);
        reservation.setTotalPrice(totalPrice);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setReservationDate(LocalDateTime.now());

        Reservation savedReservation = reservationRepository.save(reservation);

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