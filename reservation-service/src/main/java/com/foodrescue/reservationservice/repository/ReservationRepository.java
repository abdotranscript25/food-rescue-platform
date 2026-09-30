package com.foodrescue.reservationservice.repository;

import com.foodrescue.reservationservice.entity.Reservation;
import com.foodrescue.reservationservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // Remplacer findByUserId par findByConsumerId
    List<Reservation> findByConsumerId(Long consumerId);

    List<Reservation> findByOfferId(Long offerId);

    // Nouveaux filtres
    List<Reservation> findByStatus(ReservationStatus status);
    List<Reservation> findByConsumerIdAndStatus(Long consumerId, ReservationStatus status);
}