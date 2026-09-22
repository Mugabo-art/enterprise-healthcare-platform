-- PATIENT is a new Role enum value (no DB constraint change needed — role is
-- stored as VARCHAR). This migration adds the user->patient link: a
-- self-registered PATIENT account starts unlinked (NULL) until staff attach
-- it to an existing patients row via PatientController's link-user endpoint.
ALTER TABLE users ADD COLUMN patient_id UUID REFERENCES patients(id);
CREATE INDEX idx_users_patient_id ON users(patient_id);
