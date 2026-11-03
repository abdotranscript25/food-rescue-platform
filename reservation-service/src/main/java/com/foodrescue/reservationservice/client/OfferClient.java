package com.foodrescue.reservationservice.client;

import com.foodrescue.reservationservice.config.FeignConfig;
import com.foodrescue.reservationservice.dto.OfferResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "offer-service", configuration = FeignConfig.class)
public interface OfferClient {

    @GetMapping("/api/offers/{id}")
    OfferResponse getOfferById(@PathVariable("id") Long id);

    @GetMapping("/api/offers/merchant/{merchantId}")
    List<OfferResponse> getOffersByMerchant(@PathVariable("merchantId") String merchantId); // 💡 String (UUID)

    @PutMapping("/api/offers/{id}/decrement")
    void decrementStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PutMapping("/api/offers/{id}/increment")
    void incrementStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}