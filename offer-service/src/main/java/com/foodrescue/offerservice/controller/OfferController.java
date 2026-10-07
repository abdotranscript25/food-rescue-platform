package com.foodrescue.offerservice.controller;

import com.foodrescue.offerservice.client.ProductClient;
import com.foodrescue.offerservice.dto.ProductResponseDto;
import com.foodrescue.offerservice.entity.Offer;
import com.foodrescue.offerservice.entity.OfferStatus;
import com.foodrescue.offerservice.repository.OfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private ProductClient productClient;

    // Méthode utilitaire pour attacher le produit à l'offre
    private Offer enrichOfferWithProduct(Offer offer) {
        if (offer.getProductId() != null) {
            try {
                ProductResponseDto product = productClient.getProductById(offer.getProductId());
                offer.setProduct(product);
            } catch (Exception e) {
                // Gérer le cas échéant si le produit n'est pas trouvé
            }
        }
        return offer;
    }

    // ==========================================
    // Lecture : offres publiques
    // ==========================================

    @GetMapping
    public List<Offer> getAllOffers() {
        return offerRepository.findAll().stream()
                .map(this::enrichOfferWithProduct)
                .collect(Collectors.toList());
    }

    @GetMapping("/available")
    public List<Offer> getAvailableOffers() {
        return offerRepository.findByStatusAndRemainingQuantityGreaterThan(
                        OfferStatus.AVAILABLE, 0).stream()
                .map(this::enrichOfferWithProduct)
                .collect(Collectors.toList());
    }

    @GetMapping("/status/{status}")
    public List<Offer> getOffersByStatus(@PathVariable OfferStatus status) {
        return offerRepository.findByStatus(status).stream()
                .map(this::enrichOfferWithProduct)
                .collect(Collectors.toList());
    }

    @GetMapping("/merchant/{merchantId}")
    public List<Offer> getOffersByMerchant(@PathVariable Long merchantId) {
        return offerRepository.findByMerchantId(merchantId).stream()
                .map(this::enrichOfferWithProduct)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public Offer getOfferById(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));
        return enrichOfferWithProduct(offer);
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

        if (offer.getRemainingQuantity() == null) {
            offer.setRemainingQuantity(offer.getQuantity());
        }
        if (offer.getAvailableFrom() == null) {
            offer.setAvailableFrom(LocalDateTime.now());
        }
        if (offer.getStatus() == null) {
            offer.setStatus(OfferStatus.AVAILABLE);
        }

        Offer savedOffer = offerRepository.save(offer);
        return enrichOfferWithProduct(savedOffer);
    }

    // ==========================================
    // Modification : merchant ou admin
    // ==========================================

    @PutMapping("/{id}")
    public Offer updateOffer(@PathVariable Long id, @RequestBody Offer offerDetails) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        if (offerDetails.getProductId() != null) {
            offer.setProductId(offerDetails.getProductId());
        }
        if (offerDetails.getTitle() != null) {
            offer.setTitle(offerDetails.getTitle());
        }
        if (offerDetails.getDescription() != null) {
            offer.setDescription(offerDetails.getDescription());
        }
        if (offerDetails.getOriginalPrice() != null) {
            offer.setOriginalPrice(offerDetails.getOriginalPrice());
        }
        if (offerDetails.getDiscountedPrice() != null) {
            offer.setDiscountedPrice(offerDetails.getDiscountedPrice());
        }

        // Correction de la gestion de la quantité et du stock restant
        if (offerDetails.getQuantity() != null) {
            int oldQuantity = offer.getQuantity() != null ? offer.getQuantity() : 0;
            int newQuantity = offerDetails.getQuantity(); // Utiliser offerDetails ici !

            int diff = newQuantity - oldQuantity;
            int currentRemaining = offer.getRemainingQuantity() != null ? offer.getRemainingQuantity() : oldQuantity;
            int newRemaining = currentRemaining + diff;

            // S'assurer que le stock restant reste cohérent (entre 0 et la nouvelle quantité)
            if (newRemaining > newQuantity) {
                newRemaining = newQuantity;
            }
            if (newRemaining < 0) {
                newRemaining = 0;
            }

            offer.setQuantity(newQuantity);
            offer.setRemainingQuantity(newRemaining);
        }

        if (offerDetails.getExpiresAt() != null) {
            offer.setExpiresAt(offerDetails.getExpiresAt());
        }

        Offer updatedOffer = offerRepository.save(offer);
        return enrichOfferWithProduct(updatedOffer);
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
        Offer savedOffer = offerRepository.save(offer);
        return enrichOfferWithProduct(savedOffer);
    }

    // ==========================================
    // Annulation : merchant
    // ==========================================

    @PutMapping("/{id}/cancel")
    public Offer cancelOffer(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        offer.setStatus(OfferStatus.CANCELLED);
        Offer savedOffer = offerRepository.save(offer);
        return enrichOfferWithProduct(savedOffer);
    }

    // ==========================================
    // Suppression : admin ou merchant
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

        if (offer.getRemainingQuantity() == 0) {
            offer.setStatus(OfferStatus.SOLD_OUT);
        }

        Offer savedOffer = offerRepository.save(offer);
        return enrichOfferWithProduct(savedOffer);
    }

    @PutMapping("/{id}/increment")
    public Offer incrementStock(@PathVariable Long id, @RequestParam Integer quantity) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        int newRemaining = offer.getRemainingQuantity() + quantity;

        if (newRemaining > offer.getQuantity()) {
            newRemaining = offer.getQuantity();
        }

        offer.setRemainingQuantity(newRemaining);

        if (newRemaining > 0 && offer.getStatus() == OfferStatus.SOLD_OUT) {
            offer.setStatus(OfferStatus.AVAILABLE);
        }

        Offer savedOffer = offerRepository.save(offer);
        return enrichOfferWithProduct(savedOffer);
    }
}