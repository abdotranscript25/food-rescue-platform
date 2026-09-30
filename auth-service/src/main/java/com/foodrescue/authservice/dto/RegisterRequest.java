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

    // AJOUT : firstName / lastName (au lieu de fullName)
    private String firstName;
    private String lastName;

    private String email;
    private String password;
    private Role role;

    // AJOUT : Phone
    private String phone;

    // Champs pour la synchronisation avec user-service
    private String address;
    private String city;
}