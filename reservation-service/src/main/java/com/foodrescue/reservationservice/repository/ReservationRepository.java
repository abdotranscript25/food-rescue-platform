package com.foodrescue.reservationservice.repository;

import com.foodrescue.reservationservice.entity.Reservation;
import com.foodrescue.reservationservice.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByConsumerId(String consumerId); // 💡 String (UUID)

    List<Reservation> findByOfferId(Long offerId);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByConsumerIdAndStatus(String consumerId, ReservationStatus status);
}