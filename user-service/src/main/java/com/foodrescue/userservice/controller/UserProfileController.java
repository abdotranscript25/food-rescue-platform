package com.foodrescue.userservice.controller;

import com.foodrescue.userservice.dto.UserProfileDto;
import com.foodrescue.userservice.entity.UserProfile;
import com.foodrescue.userservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileRepository userProfileRepository;

    // ==========================================
    // LECTURE (GET)
    // ==========================================

    @GetMapping
    public List<UserProfileDto> getAllProfiles() {
        return userProfileRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/merchants")
    public List<UserProfileDto> getMerchantProfiles() {
        return userProfileRepository.findByRole("MERCHANT").stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDto> getProfileById(@PathVariable String id) {
        return userProfileRepository.findById(id)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserProfileDto> getProfileByEmail(@PathVariable String email) {
        return userProfileRepository.findByEmail(email)
                .map(this::mapToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // ÉCRITURE (POST / PUT)
    // ==========================================

    @PostMapping
    public ResponseEntity<UserProfileDto> createProfile(@RequestBody UserProfileDto profileDto) {
        UserProfile existing = null;

        if (profileDto.getId() != null) {
            existing = userProfileRepository.findById(profileDto.getId()).orElse(null);
        }

        if (existing == null && profileDto.getEmail() != null) {
            existing = userProfileRepository.findByEmail(profileDto.getEmail()).orElse(null);
        }

        if (existing != null) {
            // MISE À JOUR
            updateEntityFromDto(existing, profileDto);
            UserProfile saved = userProfileRepository.save(existing);
            return ResponseEntity.ok(mapToDto(saved));
        } else {
            // CRÉATION
            UserProfile newProfile = mapToEntity(profileDto);
            UserProfile saved = userProfileRepository.save(newProfile);
            return ResponseEntity.ok(mapToDto(saved));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfileDto> updateProfile(@PathVariable String id, @RequestBody UserProfileDto updatedData) {
        return userProfileRepository.findById(id)
                .map(existingProfile -> {
                    updateEntityFromDto(existingProfile, updatedData);
                    UserProfile saved = userProfileRepository.save(existingProfile);
                    return ResponseEntity.ok(mapToDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateMyProfile(
            Authentication authentication,
            @RequestBody UserProfileDto updatedData
    ) {
        String currentEmail = authentication.getName();

        UserProfile profile = userProfileRepository.findByEmail(currentEmail)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setEmail(currentEmail);
                    return newProfile;
                });

        updateEntityFromDto(profile, updatedData);
        UserProfile saved = userProfileRepository.save(profile);
        return ResponseEntity.ok(mapToDto(saved));
    }

    // ==========================================
    // MÉTHODES UTILITAIRES (MAPPING)
    // ==========================================

    private UserProfileDto mapToDto(UserProfile entity) {
        return UserProfileDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .city(entity.getCity())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserProfile mapToEntity(UserProfileDto dto) {
        return UserProfile.builder()
                .id(dto.getId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .address(dto.getAddress())
                .city(dto.getCity())
                .role(dto.getRole())
                .build();
    }

    private void updateEntityFromDto(UserProfile entity, UserProfileDto dto) {
        if (dto.getFirstName() != null) entity.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) entity.setLastName(dto.getLastName());
        if (dto.getPhone() != null) entity.setPhone(dto.getPhone());
        if (dto.getAddress() != null) entity.setAddress(dto.getAddress());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        // ⚠️ SUPPRESSION DE LA LIGNE EMAIL ICI (L'email ne sera plus jamais modifié via ces méthodes)
        if (dto.getRole() != null) entity.setRole(dto.getRole());
    }
}