package com.foodrescue.offerservice.controller;

import com.foodrescue.offerservice.entity.Offer;
import com.foodrescue.offerservice.entity.OfferStatus;
import com.foodrescue.offerservice.repository.OfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    @Autowired
    private OfferRepository offerRepository;

    // ==========================================
    // Lecture : offres publiques
    // ==========================================

    /**
     * Liste TOUTES les offres (usage admin / debug).
     */
    @GetMapping
    public List<Offer> getAllOffers() {
        return offerRepository.findAll();
    }

    /**
     * Liste uniquement les offres disponibles (à afficher aux clients).
     */
    @GetMapping("/available")
    public List<Offer> getAvailableOffers() {
        return offerRepository.findByStatusAndRemainingQuantityGreaterThan(
                OfferStatus.AVAILABLE, 0);
    }

    /**
     * Filtre par statut.
     */
    @GetMapping("/status/{status}")
    public List<Offer> getOffersByStatus(@PathVariable OfferStatus status) {
        return offerRepository.findByStatus(status);
    }

    /**
     * Filtre par merchant.
     */
    @GetMapping("/merchant/{merchantId}")
    public List<Offer> getOffersByMerchant(@PathVariable Long merchantId) {
        return offerRepository.findByMerchantId(merchantId);
    }

    @GetMapping("/{id}")
    public Offer getOfferById(@PathVariable Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));
    }

    // ==========================================
    // Création : merchant uniquement
    // ==========================================

    @PostMapping
    public Offer createOffer(@RequestBody Offer offer) {
        if (offer.getMerchantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le merchantId est obligatoire.");
        }
        if (offer.getQuantity() == null || offer.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être > 0.");
        }

        // Initialiser les valeurs par défaut
        if (offer.getRemainingQuantity() == null) {
            offer.setRemainingQuantity(offer.getQuantity());
        }
        if (offer.getAvailableFrom() == null) {
            offer.setAvailableFrom(LocalDateTime.now());
        }
        if (offer.getStatus() == null) {
            offer.setStatus(OfferStatus.AVAILABLE); // Par défaut : directement disponible
        }

        return offerRepository.save(offer);
    }

    // ==========================================
    // Publication : DRAFT → AVAILABLE
    // ==========================================

    @PutMapping("/{id}/publish")
    public Offer publishOffer(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seules les offres en DRAFT peuvent être publiées.");
        }

        offer.setStatus(OfferStatus.AVAILABLE);
        return offerRepository.save(offer);
    }

    // ==========================================
    // Annulation : merchant
    // ==========================================

    @PutMapping("/{id}/cancel")
    public Offer cancelOffer(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        offer.setStatus(OfferStatus.CANCELLED);
        return offerRepository.save(offer);
    }

    // ==========================================
    // Suppression : admin
    // ==========================================

    @DeleteMapping("/{id}")
    public void deleteOffer(@PathVariable Long id) {
        offerRepository.deleteById(id);
    }

    // ==========================================
    // Endpoints internes (appelés par reservation-service via Feign)
    // ==========================================

    @PutMapping("/{id}/decrement")
    public Offer decrementStock(@PathVariable Long id, @RequestParam Integer quantity) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        if (offer.getRemainingQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock insuffisant. Disponible : " + offer.getRemainingQuantity());
        }

        offer.setRemainingQuantity(offer.getRemainingQuantity() - quantity);

        // Mise à jour automatique du statut si épuisé
        if (offer.getRemainingQuantity() == 0) {
            offer.setStatus(OfferStatus.SOLD_OUT);
        }

        return offerRepository.save(offer);
    }

    @PutMapping("/{id}/increment")
    public Offer incrementStock(@PathVariable Long id, @RequestParam Integer quantity) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        int newRemaining = offer.getRemainingQuantity() + quantity;

        // Ne pas dépasser la quantité initiale
        if (newRemaining > offer.getQuantity()) {
            newRemaining = offer.getQuantity();
        }

        offer.setRemainingQuantity(newRemaining);

        // Si on redevient disponible, on remet le statut
        if (newRemaining > 0 && offer.getStatus() == OfferStatus.SOLD_OUT) {
            offer.setStatus(OfferStatus.AVAILABLE);
        }

        return offerRepository.save(offer);
    }
}