package com.foodrescue.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;

    // AJOUT : firstName / lastName (au lieu de fullName)
    private String firstName;
    private String lastName;

    private String email;

    // AJOUT : phone (au lieu de phoneNumber)
    private String phone;

    private String address;
    private String city;
}