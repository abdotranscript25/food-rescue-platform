package com.foodrescue.reservationservice.client;

import com.foodrescue.reservationservice.config.FeignConfig;
import com.foodrescue.reservationservice.dto.OfferResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "offer-service", configuration = FeignConfig.class)
public interface OfferClient {

    // AJOUT : récupérer les infos de l'offre
    @GetMapping("/api/offers/{id}")
    OfferResponse getOfferById(@PathVariable("id") Long id);

    @PutMapping("/api/offers/{id}/decrement")
    void decrementStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PutMapping("/api/offers/{id}/increment")
    void incrementStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}