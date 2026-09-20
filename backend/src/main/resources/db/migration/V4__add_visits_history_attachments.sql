CREATE TABLE visits (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id          UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    visit_date          TIMESTAMP NOT NULL,
    visit_type          VARCHAR(30) NOT NULL,
    reason              VARCHAR(500) NOT NULL,
    notes               TEXT,
    attending_staff_id  UUID REFERENCES users(id),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP
);
CREATE INDEX idx_visits_patient_id ON visits(patient_id);

CREATE TABLE medical_history_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    category        VARCHAR(30) NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    recorded_date   DATE NOT NULL,
    recorded_by_id  UUID REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_medical_history_patient_id ON medical_history_entries(patient_id);

-- file_data stores the raw bytes in Postgres rather than a filesystem/object
-- store: Docker Compose has no volume-backed upload dir or MinIO service, and
-- bytea keeps uploads transactional and the backend container stateless.
CREATE TABLE attachments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    visit_id        UUID REFERENCES visits(id) ON DELETE SET NULL,
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_data       BYTEA NOT NULL,
    uploaded_by_id  UUID REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachments_patient_id ON attachments(patient_id);
