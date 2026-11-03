package com.foodrescue.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "reservation-service", configuration = FeignClientConfig.class)
public interface ReservationClient {
    @GetMapping("/api/reservations/{id}")
    Object getReservationById(@PathVariable("id") Long id);
}