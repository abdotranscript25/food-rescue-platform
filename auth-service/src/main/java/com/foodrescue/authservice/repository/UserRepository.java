package com.foodrescue.authservice.repository;

import com.foodrescue.authservice.entity.User;
import com.foodrescue.authservice.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> { // ⬅️ Changé de Long à String (UUID)
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    List<User> findByStatus(UserStatus status);
}