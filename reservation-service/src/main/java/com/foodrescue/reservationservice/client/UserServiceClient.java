package com.foodrescue.reservationservice.client;

import com.foodrescue.reservationservice.config.FeignConfig;
import com.foodrescue.reservationservice.dto.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "USER-SERVICE", configuration = FeignConfig.class)
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserProfileResponse getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/users/email/{email}")
    UserProfileResponse getUserByEmail(@PathVariable("email") String email);
}