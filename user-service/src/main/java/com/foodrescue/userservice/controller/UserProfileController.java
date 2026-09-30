package com.foodrescue.userservice.controller;

import com.foodrescue.userservice.entity.UserProfile;
import com.foodrescue.userservice.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/profiles")
public class UserProfileController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    // ==========================================
    // Lecture
    // ==========================================

    @GetMapping
    public List<UserProfile> getAllProfiles() {
        return userProfileRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> getProfileById(@PathVariable Long id) {
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

    // ==========================================
    // Création (appelée par auth-service lors du register)
    // ==========================================

    @PostMapping
    public UserProfile createProfile(@RequestBody UserProfile profile) {
        return userProfileRepository.save(profile);
    }

    // ==========================================
    // Mise à jour
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateProfile(@PathVariable Long id, @RequestBody UserProfile updatedData) {
        return userProfileRepository.findById(id)
                .map(existingProfile -> {
                    // AJOUT : firstName / lastName / phone
                    if (updatedData.getFirstName() != null) existingProfile.setFirstName(updatedData.getFirstName());
                    if (updatedData.getLastName() != null) existingProfile.setLastName(updatedData.getLastName());
                    if (updatedData.getPhone() != null) existingProfile.setPhone(updatedData.getPhone());
                    if (updatedData.getAddress() != null) existingProfile.setAddress(updatedData.getAddress());
                    if (updatedData.getCity() != null) existingProfile.setCity(updatedData.getCity());
                    return ResponseEntity.ok(userProfileRepository.save(existingProfile));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}