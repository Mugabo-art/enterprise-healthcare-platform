package com.healthplatform.common.security;

import com.healthplatform.auth.model.Role;
import com.healthplatform.auth.model.User;
import com.healthplatform.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ownership check for every "...patients/{patientId}/..." endpoint. Staff
 * roles (ADMIN, DOCTOR, NURSE, LAB_TECH, PHARMACIST, BILLING_CLERK) are
 * unrestricted here, same as before this guard existed — this only narrows
 * access for PATIENT, which must match its own linked patientId. Mirrors the
 * manual self-check already used in DoctorController.schedule.
 */
@Component
public class PatientAccessGuard {

    public void assertAccess(User currentUser, UUID patientId) {
        if (currentUser.getRole() == Role.PATIENT
                && (currentUser.getPatientId() == null || !currentUser.getPatientId().equals(patientId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You may only access your own records");
        }
    }
}
