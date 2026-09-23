-- FR-4 Laboratory: doctors/nurses request a test against a visit, a lab
-- technician records one result against it (1:1, enforced by the UNIQUE
-- constraint on lab_request_id). Status includes CANCELLED alongside the
-- REQUESTED/IN_PROGRESS/COMPLETED set originally sketched in
-- docs/DATABASE_DESIGN.md, to match how visits/prescriptions already model
-- workflow state (see docs/DATABASE_DESIGN.md for the synced version).
CREATE TABLE lab_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    visit_id        UUID NOT NULL REFERENCES visits(id) ON DELETE CASCADE,
    requested_by_id UUID REFERENCES users(id),
    test_type       VARCHAR(100) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP
);
CREATE INDEX idx_lab_requests_patient_id ON lab_requests(patient_id);
CREATE INDEX idx_lab_requests_visit_id ON lab_requests(visit_id);

CREATE TABLE lab_results (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lab_request_id  UUID NOT NULL UNIQUE REFERENCES lab_requests(id) ON DELETE CASCADE,
    recorded_by_id  UUID REFERENCES users(id),
    result_data     JSONB NOT NULL,
    recorded_at     TIMESTAMP NOT NULL DEFAULT now()
);
