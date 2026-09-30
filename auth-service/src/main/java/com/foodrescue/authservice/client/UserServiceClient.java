package com.foodrescue.authservice.client;

import com.foodrescue.authservice.dto.UserProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Crée un profil utilisateur dans user-service.
     * Appelé par auth-service lors de l'inscription.
     */
    @PostMapping("/api/users/profiles")
    UserProfileRequest createProfile(@RequestBody UserProfileRequest profileRequest);
}