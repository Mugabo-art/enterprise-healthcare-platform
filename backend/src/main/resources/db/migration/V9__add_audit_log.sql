-- Append-only audit trail of who touched what (HIPAA-style access accountability).
-- Rows are written by the application (AuditService) and can never be updated or deleted.
CREATE TABLE audit_log (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at   TIMESTAMP NOT NULL DEFAULT now(),
    actor_id      UUID,
    actor_email   VARCHAR(255),
    actor_role    VARCHAR(30),
    action        VARCHAR(40) NOT NULL,
    resource_type VARCHAR(60),
    resource_id   VARCHAR(64),
    http_method   VARCHAR(10),
    path          VARCHAR(255),
    status_code   INTEGER,
    outcome       VARCHAR(20) NOT NULL,
    ip_address    VARCHAR(64),
    request_id    VARCHAR(64),
    detail        VARCHAR(500)
);
CREATE INDEX idx_audit_log_occurred_at ON audit_log(occurred_at DESC);
CREATE INDEX idx_audit_log_actor_id ON audit_log(actor_id);
CREATE INDEX idx_audit_log_resource ON audit_log(resource_type, resource_id);

CREATE FUNCTION audit_log_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_log_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_immutable();
