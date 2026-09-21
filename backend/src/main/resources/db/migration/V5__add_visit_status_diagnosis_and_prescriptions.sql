ALTER TABLE visits ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED';
ALTER TABLE visits ADD COLUMN diagnosis_code VARCHAR(20);
CREATE INDEX idx_visits_attending_staff_id ON visits(attending_staff_id);

CREATE TABLE prescriptions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id          UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    visit_id            UUID NOT NULL REFERENCES visits(id) ON DELETE CASCADE,
    prescribed_by_id    UUID REFERENCES users(id),
    medication_name     VARCHAR(200) NOT NULL,
    dosage              VARCHAR(100) NOT NULL,
    frequency           VARCHAR(100) NOT NULL,
    duration_days       INT,
    instructions        TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);
CREATE INDEX idx_prescriptions_patient_id ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_visit_id ON prescriptions(visit_id);
