package com.foodrescue.notificationservice.dto;

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
    private String status;  // Exemples : "CREATED", "CANCELLED"
    private String message;
}