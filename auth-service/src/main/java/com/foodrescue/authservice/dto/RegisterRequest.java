package com.foodrescue.authservice.dto;

import com.foodrescue.authservice.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    private String fullName;
    private String email;
    private String password;
    private Role role;

    // Nouveaux champs pour la synchronisation avec user-service
    private String phoneNumber;
    private String address;
    private String city;
}