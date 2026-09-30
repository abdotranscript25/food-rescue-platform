package com.foodrescue.offerservice.repository;

import com.foodrescue.offerservice.entity.Offer;
import com.foodrescue.offerservice.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    // Recherche par statut
    List<Offer> findByStatus(OfferStatus status);

    // Recherche par merchant
    List<Offer> findByMerchantId(Long merchantId);

    // Recherche par product
    List<Offer> findByProductId(Long productId);

    // Recherche des offres disponibles (à afficher aux clients)
    List<Offer> findByStatusAndRemainingQuantityGreaterThan(OfferStatus status, Integer minQuantity);

    // Recherche des offres expirées (pour un job de nettoyage)
    List<Offer> findByStatusAndExpiresAtBefore(OfferStatus status, LocalDateTime date);
}