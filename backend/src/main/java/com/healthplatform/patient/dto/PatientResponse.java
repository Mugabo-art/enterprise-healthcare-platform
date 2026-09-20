package com.healthplatform.patient.dto;

import com.healthplatform.patient.model.Patient;
import com.healthplatform.patient.model.Sex;

import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id, String firstName, String lastName, LocalDate dateOfBirth,
        Sex sex, String contactPhone, String contactEmail, String address
) {
    public static PatientResponse from(Patient p) {
        return new PatientResponse(
                p.getId(), p.getFirstName(), p.getLastName(), p.getDateOfBirth(),
                p.getSex(), p.getContactPhone(), p.getContactEmail(), p.getAddress()
        );
    }
}
