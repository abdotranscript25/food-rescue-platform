package com.foodrescue.userservice.repository;

import com.foodrescue.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByEmail(String email);

    // 💡 AJOUT : Permet de récupérer la liste des utilisateurs par leur rôle
    List<UserProfile> findByRole(String role);
}