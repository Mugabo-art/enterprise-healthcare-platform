package com.healthplatform.doctor.dto;

import com.healthplatform.auth.dto.UserResponse;

import java.util.UUID;

public record DoctorSummary(UUID id, String email) {
    public static DoctorSummary from(UserResponse user) {
        return new DoctorSummary(user.id(), user.email());
    }
}
