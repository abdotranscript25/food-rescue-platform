package com.foodrescue.offerservice.client;

import com.foodrescue.offerservice.config.FeignConfig;
import com.foodrescue.offerservice.dto.PartnerResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Ajout de url = "http://localhost:8086" pour contourner temporairement la résolution Eureka
@FeignClient(name = "PARTNER-SERVICE", url = "http://localhost:8086", configuration = FeignConfig.class)
public interface PartnerClient {

    @GetMapping("/api/partners/{id}")
    PartnerResponseDto getPartnerById(@PathVariable("id") Long id);
}