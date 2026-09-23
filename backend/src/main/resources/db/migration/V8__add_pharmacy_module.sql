-- FR-5 Pharmacy: medication inventory with reorder thresholds, and a
-- dispensation record per prescription fill. Dispensing decrements stock and
-- completes the prescription (see PharmacyService).
CREATE TABLE medications (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(150) NOT NULL UNIQUE,
    unit              VARCHAR(30) NOT NULL,
    stock_quantity    INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    reorder_threshold INTEGER NOT NULL DEFAULT 0 CHECK (reorder_threshold >= 0),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP
);

CREATE TABLE dispensations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    prescription_id UUID NOT NULL UNIQUE REFERENCES prescriptions(id) ON DELETE CASCADE,
    medication_id   UUID NOT NULL REFERENCES medications(id),
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    dispensed_by_id UUID REFERENCES users(id),
    dispensed_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_dispensations_patient_id ON dispensations(patient_id);
CREATE INDEX idx_dispensations_medication_id ON dispensations(medication_id);
