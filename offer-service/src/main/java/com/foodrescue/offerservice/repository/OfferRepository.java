package com.foodrescue.offerservice.repository;

import com.foodrescue.offerservice.entity.Offer;
import com.foodrescue.offerservice.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByStatus(OfferStatus status);

    // 💡 merchantId est maintenant un String (UUID)
    List<Offer> findByMerchantId(String merchantId);

    List<Offer> findByProductId(Long productId);

    List<Offer> findByStatusAndRemainingQuantityGreaterThan(OfferStatus status, Integer minQuantity);

    List<Offer> findByStatusAndExpiresAtBefore(OfferStatus status, LocalDateTime date);
}