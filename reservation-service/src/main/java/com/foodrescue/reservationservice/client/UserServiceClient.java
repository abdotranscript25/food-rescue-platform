package com.foodrescue.reservationservice.client;

import com.foodrescue.reservationservice.config.FeignConfig;
import com.foodrescue.reservationservice.dto.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", configuration = FeignConfig.class)
public interface UserServiceClient {

    // AJOUT : chemin /api/users/profiles/{id} (au lieu de /api/users/{id})
    @GetMapping("/api/users/profiles/{id}")
    UserProfileResponse getUserById(@PathVariable("id") Long id);

    // AJOUT : chemin /api/users/profiles/email/{email}
    @GetMapping("/api/users/profiles/email/{email}")
    UserProfileResponse getUserByEmail(@PathVariable("email") String email);
}