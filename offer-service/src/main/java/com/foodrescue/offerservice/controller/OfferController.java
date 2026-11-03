package com.foodrescue.offerservice.controller;

import com.foodrescue.offerservice.client.ProductClient;
import com.foodrescue.offerservice.dto.OfferDto;
import com.foodrescue.offerservice.dto.ProductResponseDto;
import com.foodrescue.offerservice.entity.Offer;
import com.foodrescue.offerservice.entity.OfferStatus;
import com.foodrescue.offerservice.repository.OfferRepository;
import com.foodrescue.offerservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor // ⬅️ AJOUT
public class OfferController {

    private final OfferRepository offerRepository; // ⬅️ Injection par constructeur
    private final ProductClient productClient;
    private final JwtService jwtService;

    private Offer enrichOfferWithProduct(Offer offer) {
        if (offer.getProductId() != null) {
            try {
                ProductResponseDto product = productClient.getProductById(offer.getProductId());
                offer.setProduct(product);
                if (product != null && product.getImageUrl() != null) {
                    offer.setImageUrl(product.getImageUrl());
                }
            } catch (Exception e) {
                // Gérer le cas échéant
            }
        }
        return offer;
    }

    // ==========================================
    // LECTURE
    // ==========================================

    @GetMapping
    public List<OfferDto> getAllOffers() {
        return offerRepository.findAll().stream()
                .map(this::enrichOfferWithProduct)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/available")
    public List<OfferDto> getAvailableOffers() {
        return offerRepository.findByStatusAndRemainingQuantityGreaterThan(
                        OfferStatus.AVAILABLE, 0).stream()
                .map(this::enrichOfferWithProduct)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/status/{status}")
    public List<OfferDto> getOffersByStatus(@PathVariable OfferStatus status) {
        return offerRepository.findByStatus(status).stream()
                .map(this::enrichOfferWithProduct)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/merchant/{merchantId}")
    public List<OfferDto> getOffersByMerchant(@PathVariable String merchantId) {
        return offerRepository.findByMerchantId(merchantId).stream()
                .map(this::enrichOfferWithProduct)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/my-offers")
    public List<OfferDto> getMyOffers(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String merchantUuid = jwtService.extractUserId(token);
            if (merchantUuid == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UUID introuvable dans le token.");
            }
            return offerRepository.findByMerchantId(merchantUuid).stream()
                    .map(this::enrichOfferWithProduct)
                    .map(this::mapToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Impossible d'identifier le marchand connecté : " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public OfferDto getOfferById(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));
        return mapToDto(enrichOfferWithProduct(offer));
    }

    // ==========================================
    // CRÉATION & MODIFICATION
    // ==========================================

    @PostMapping
    public OfferDto createOffer(@RequestBody OfferDto offerDto) {
        if (offerDto.getMerchantId() == null || offerDto.getMerchantId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le merchantId (UUID) est obligatoire.");
        }
        if (offerDto.getQuantity() == null || offerDto.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La quantité doit être > 0.");
        }

        Offer offer = mapToEntity(offerDto);

        if (offer.getRemainingQuantity() == null) offer.setRemainingQuantity(offer.getQuantity());
        if (offer.getAvailableFrom() == null) offer.setAvailableFrom(LocalDateTime.now());
        if (offer.getStatus() == null) offer.setStatus(OfferStatus.AVAILABLE);

        Offer savedOffer = offerRepository.save(offer);
        return mapToDto(enrichOfferWithProduct(savedOffer));
    }

    @PutMapping("/{id}")
    public OfferDto updateOffer(@PathVariable Long id, @RequestBody OfferDto offerDetails) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        if (offerDetails.getProductId() != null) offer.setProductId(offerDetails.getProductId());
        if (offerDetails.getTitle() != null) offer.setTitle(offerDetails.getTitle());
        if (offerDetails.getDescription() != null) offer.setDescription(offerDetails.getDescription());
        if (offerDetails.getOriginalPrice() != null) offer.setOriginalPrice(offerDetails.getOriginalPrice());
        if (offerDetails.getDiscountedPrice() != null) offer.setDiscountedPrice(offerDetails.getDiscountedPrice());

        if (offerDetails.getQuantity() != null) {
            int oldQuantity = offer.getQuantity() != null ? offer.getQuantity() : 0;
            int newQuantity = offerDetails.getQuantity();
            int diff = newQuantity - oldQuantity;
            int currentRemaining = offer.getRemainingQuantity() != null ? offer.getRemainingQuantity() : oldQuantity;
            int newRemaining = currentRemaining + diff;

            if (newRemaining > newQuantity) newRemaining = newQuantity;
            if (newRemaining < 0) newRemaining = 0;

            offer.setQuantity(newQuantity);
            offer.setRemainingQuantity(newRemaining);
        }
        if (offerDetails.getExpiresAt() != null) offer.setExpiresAt(offerDetails.getExpiresAt());

        Offer updatedOffer = offerRepository.save(offer);
        return mapToDto(enrichOfferWithProduct(updatedOffer));
    }

    // ==========================================
    // PUBLICATION, ANNULATION, STOCK, DELETE
    // ==========================================

    @PutMapping("/{id}/publish")
    public OfferDto publishOffer(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seules les offres en DRAFT peuvent être publiées.");
        }

        offer.setStatus(OfferStatus.AVAILABLE);
        return mapToDto(enrichOfferWithProduct(offerRepository.save(offer)));
    }

    @PutMapping("/{id}/cancel")
    public OfferDto cancelOffer(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));
        offer.setStatus(OfferStatus.CANCELLED);
        return mapToDto(enrichOfferWithProduct(offerRepository.save(offer)));
    }

    @DeleteMapping("/{id}")
    public void deleteOffer(@PathVariable Long id) {
        offerRepository.deleteById(id);
    }

    @PutMapping("/{id}/decrement")
    public OfferDto decrementStock(@PathVariable Long id, @RequestParam Integer quantity) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        if (offer.getRemainingQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock insuffisant. Disponible : " + offer.getRemainingQuantity());
        }
        offer.setRemainingQuantity(offer.getRemainingQuantity() - quantity);
        if (offer.getRemainingQuantity() == 0) offer.setStatus(OfferStatus.SOLD_OUT);

        return mapToDto(enrichOfferWithProduct(offerRepository.save(offer)));
    }

    @PutMapping("/{id}/increment")
    public OfferDto incrementStock(@PathVariable Long id, @RequestParam Integer quantity) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        int newRemaining = offer.getRemainingQuantity() + quantity;
        if (newRemaining > offer.getQuantity()) newRemaining = offer.getQuantity();

        offer.setRemainingQuantity(newRemaining);
        if (newRemaining > 0 && offer.getStatus() == OfferStatus.SOLD_OUT) {
            offer.setStatus(OfferStatus.AVAILABLE);
        }
        return mapToDto(enrichOfferWithProduct(offerRepository.save(offer)));
    }

    // ==========================================
    // MAPPING
    // ==========================================
    private OfferDto mapToDto(Offer offer) {
        return OfferDto.builder()
                .id(offer.getId())
                .productId(offer.getProductId())
                .product(offer.getProduct())
                .merchantId(offer.getMerchantId())
                .title(offer.getTitle())
                .description(offer.getDescription())
                .imageUrl(offer.getImageUrl())
                .originalPrice(offer.getOriginalPrice())
                .discountedPrice(offer.getDiscountedPrice())
                .quantity(offer.getQuantity())
                .remainingQuantity(offer.getRemainingQuantity())
                .availableFrom(offer.getAvailableFrom())
                .expiresAt(offer.getExpiresAt())
                .status(offer.getStatus())
                .build();
    }

    private Offer mapToEntity(OfferDto dto) {
        return Offer.builder()
                .id(dto.getId())
                .productId(dto.getProductId())
                .merchantId(dto.getMerchantId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .originalPrice(dto.getOriginalPrice())
                .discountedPrice(dto.getDiscountedPrice())
                .quantity(dto.getQuantity())
                .remainingQuantity(dto.getRemainingQuantity())
                .availableFrom(dto.getAvailableFrom())
                .expiresAt(dto.getExpiresAt())
                .status(dto.getStatus())
                .build();
    }
}