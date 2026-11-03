package com.foodrescue.offerservice.controller;

import com.foodrescue.offerservice.model.Review;
import com.foodrescue.offerservice.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @PostMapping
    public ResponseEntity<Review> createReview(@RequestBody Review review) {
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return ResponseEntity.badRequest().build();
        }
        Review savedReview = reviewRepository.save(review);
        return ResponseEntity.ok(savedReview);
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Review>> getReviewsByMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(reviewRepository.findByMerchantId(merchantId));
    }

    @GetMapping("/merchant/{merchantId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable String merchantId) {
        Double avg = reviewRepository.getAverageRatingByMerchantId(merchantId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }
}