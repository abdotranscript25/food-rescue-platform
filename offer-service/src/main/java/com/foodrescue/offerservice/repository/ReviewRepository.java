package com.foodrescue.offerservice.repository;

import com.foodrescue.offerservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByMerchantId(String merchantId); // 💡 String (UUID)

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.merchantId = :merchantId")
    Double getAverageRatingByMerchantId(@Param("merchantId") String merchantId);
}