package com.foodrescue.authservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // ==========================================
    // AJOUT MFA : Champs pour l'authentification à deux facteurs
    // ==========================================

    /**
     * Indique si le MFA est activé pour cet utilisateur.
     * Par défaut : false (le login reste classique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    /**
     * Secret TOTP (clé partagée entre le serveur et l'app Google Authenticator).
     * Nullable : null si le MFA n'est pas configuré.
     */
    @Column(length = 64)
    private String mfaSecret;

    // ==========================================
    // Méthodes requises par l'interface UserDetails
    // ==========================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email; // On utilise l'email comme identifiant
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}