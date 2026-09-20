package com.healthplatform.auth.dto;

import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;

import java.util.UUID;

public record UserResponse(UUID id, String email, Role role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
