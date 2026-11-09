package com.foodrescue.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping; // N'oubliez pas l'import

@FeignClient(name = "reservation-service", configuration = FeignClientConfig.class)
public interface ReservationClient {

    @GetMapping("/api/reservations/{id}")
    Object getReservationById(@PathVariable("id") Long id);

    // ⬇️ AJOUTEZ CECI
    @PutMapping("/api/reservations/{id}/confirm")
    void confirmReservation(@PathVariable("id") Long id);
}