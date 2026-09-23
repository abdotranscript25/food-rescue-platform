package com.foodrescue.reservationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationEvent implements Serializable {
    private String reservationId;
    private String offerId;
    private String userEmail;
    private String status;  // "CREATED" ou "CANCELLED"
    private String message;
}