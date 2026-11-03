package com.foodrescue.authservice.dto;

import com.foodrescue.authservice.entity.Role;
import com.foodrescue.authservice.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private boolean mfaEnabled;
    // ⚠️ PAS DE password, PAS DE mfaSecret !
}