package com.foodrescue.offerservice.controller;

import com.foodrescue.offerservice.dto.ReviewDto;
import com.foodrescue.offerservice.model.Review;
import com.foodrescue.offerservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor // ⬅️ Injection par constructeur
public class ReviewController {

    private final ReviewRepository reviewRepository; // ⬅️ Injection par constructeur

    @PostMapping
    public ResponseEntity<ReviewDto> createReview(@RequestBody ReviewDto reviewDto) {
        if (reviewDto.getRating() == null || reviewDto.getRating() < 1 || reviewDto.getRating() > 5) {
            return ResponseEntity.badRequest().build();
        }
        Review review = mapToEntity(reviewDto);
        Review savedReview = reviewRepository.save(review);
        return ResponseEntity.ok(mapToDto(savedReview));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByMerchant(@PathVariable String merchantId) {
        List<ReviewDto> reviews = reviewRepository.findByMerchantId(merchantId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/merchant/{merchantId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable String merchantId) {
        Double avg = reviewRepository.getAverageRatingByMerchantId(merchantId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    // ==========================================
    // MAPPING
    // ==========================================
    private ReviewDto mapToDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .clientId(review.getClientId())
                .merchantId(review.getMerchantId())
                .clientName(review.getClientName())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private Review mapToEntity(ReviewDto dto) {
        Review review = new Review();
        review.setId(dto.getId());
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setClientId(dto.getClientId());
        review.setMerchantId(dto.getMerchantId());
        review.setClientName(dto.getClientName());
        if (dto.getCreatedAt() != null) review.setCreatedAt(dto.getCreatedAt());
        return review;
    }
}