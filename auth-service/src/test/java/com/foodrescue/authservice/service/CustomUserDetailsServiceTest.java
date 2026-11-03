package com.foodrescue.authservice.service;

import com.foodrescue.authservice.entity.Role;
import com.foodrescue.authservice.entity.User;
import com.foodrescue.authservice.entity.UserStatus;
import com.foodrescue.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("loadUserByUsername : doit retourner l'utilisateur")
    void loadUserByUsername_shouldReturnUser() {
        User user = User.builder()
                .id("test-id")
                .firstName("John").lastName("Doe")
                .email("john@test.com")
                .password("hashed_pass")
                .role(Role.CONSUMER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        UserDetails result = customUserDetailsService.loadUserByUsername("john@test.com");

        assertNotNull(result);
        assertEquals("john@test.com", result.getUsername());
    }

    @Test
    @DisplayName("loadUserByUsername : doit lancer une exception si l'utilisateur n'existe pas")
    void loadUserByUsername_shouldThrowIfNotFound() {
        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("inconnu@test.com"));
    }
}