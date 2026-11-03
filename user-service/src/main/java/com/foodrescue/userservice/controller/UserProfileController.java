package com.foodrescue.userservice.controller;

import com.foodrescue.userservice.entity.UserProfile;
import com.foodrescue.userservice.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/profiles")
public class UserProfileController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @GetMapping
    public List<UserProfile> getAllProfiles() {
        return userProfileRepository.findAll();
    }

    @GetMapping("/merchants")
    public List<UserProfile> getMerchantProfiles() {
        return userProfileRepository.findByRole("MERCHANT");
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getProfileById(@PathVariable String id) { // ⬅️ String
        return userProfileRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserProfile> getProfileByEmail(@PathVariable String email) {
        return userProfileRepository.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserProfile> createProfile(@RequestBody UserProfile profile) {
        UserProfile existing = null;

        if (profile.getId() != null) {
            existing = userProfileRepository.findById(profile.getId()).orElse(null);
        }

        if (existing == null && profile.getEmail() != null) {
            existing = userProfileRepository.findByEmail(profile.getEmail()).orElse(null);
        }

        if (existing != null) {
            // MISE À JOUR
            if (profile.getFirstName() != null) existing.setFirstName(profile.getFirstName());
            if (profile.getLastName() != null) existing.setLastName(profile.getLastName());
            if (profile.getPhone() != null) existing.setPhone(profile.getPhone());
            if (profile.getAddress() != null) existing.setAddress(profile.getAddress());
            if (profile.getCity() != null) existing.setCity(profile.getCity());
            if (profile.getRole() != null) existing.setRole(profile.getRole());
            return ResponseEntity.ok(userProfileRepository.save(existing));
        } else {
            // CRÉATION : l'ID String est fourni par auth-service
            return ResponseEntity.ok(userProfileRepository.save(profile));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateProfile(@PathVariable String id, @RequestBody UserProfile updatedData) { // ⬅️ String
        return userProfileRepository.findById(id)
                .map(existingProfile -> {
                    if (updatedData.getFirstName() != null) existingProfile.setFirstName(updatedData.getFirstName());
                    if (updatedData.getLastName() != null) existingProfile.setLastName(updatedData.getLastName());
                    if (updatedData.getPhone() != null) existingProfile.setPhone(updatedData.getPhone());
                    if (updatedData.getAddress() != null) existingProfile.setAddress(updatedData.getAddress());
                    if (updatedData.getCity() != null) existingProfile.setCity(updatedData.getCity());
                    if (updatedData.getEmail() != null) existingProfile.setEmail(updatedData.getEmail());
                    if (updatedData.getRole() != null) existingProfile.setRole(updatedData.getRole());
                    return ResponseEntity.ok(userProfileRepository.save(existingProfile));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfile> updateMyProfile(
            Authentication authentication,
            @RequestBody UserProfile updatedData
    ) {
        String currentEmail = authentication.getName();

        UserProfile profile = userProfileRepository.findByEmail(currentEmail)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setEmail(currentEmail);
                    return newProfile;
                });

        if (updatedData.getFirstName() != null) profile.setFirstName(updatedData.getFirstName());
        if (updatedData.getLastName() != null) profile.setLastName(updatedData.getLastName());
        if (updatedData.getPhone() != null) profile.setPhone(updatedData.getPhone());
        if (updatedData.getAddress() != null) profile.setAddress(updatedData.getAddress());
        if (updatedData.getCity() != null) profile.setCity(updatedData.getCity());
        if (updatedData.getEmail() != null) profile.setEmail(updatedData.getEmail());
        if (updatedData.getRole() != null) profile.setRole(updatedData.getRole());

        return ResponseEntity.ok(userProfileRepository.save(profile));
    }
}