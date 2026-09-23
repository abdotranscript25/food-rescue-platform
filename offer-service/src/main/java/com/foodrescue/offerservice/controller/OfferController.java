package com.foodrescue.offerservice.controller;

import com.foodrescue.offerservice.entity.Offer;
import com.foodrescue.offerservice.repository.OfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    @Autowired
    private OfferRepository offerRepository;

    @GetMapping
    public List<Offer> getAllOffers() {
        return offerRepository.findAll();
    }

    @PostMapping
    public Offer createOffer(@RequestBody Offer offer) {
        if (offer.getMerchantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le merchantId est obligatoire.");
        }
        return offerRepository.save(offer);
    }

    @GetMapping("/{id}")
    public Offer getOfferById(@PathVariable Long id) {
        return offerRepository.findById(id).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void deleteOffer(@PathVariable Long id) {
        offerRepository.deleteById(id);
    }

    @PutMapping("/{id}/decrement")
    public Offer decrementStock(@PathVariable Long id, @RequestParam Integer quantity) {
        Offer offer = offerRepository.findById(id).orElse(null);
        if (offer == null) {
            throw new RuntimeException("Offre introuvable avec l'id : " + id);
        }
        if (offer.getQuantity() < quantity) {
            throw new RuntimeException("Stock insuffisant. Disponible : " + offer.getQuantity());
        }
        offer.setQuantity(offer.getQuantity() - quantity);
        return offerRepository.save(offer);
    }

    @PutMapping("/{id}/increment")
    public Offer incrementStock(@PathVariable Long id, @RequestParam Integer quantity) {
        Offer offer = offerRepository.findById(id).orElse(null);
        if (offer == null) {
            throw new RuntimeException("Offre introuvable avec l'id : " + id);
        }
        offer.setQuantity(offer.getQuantity() + quantity);
        return offerRepository.save(offer);
    }
}