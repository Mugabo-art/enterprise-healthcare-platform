-- Dev-only convenience seed: admin@hospital.test / Password123!
-- (bcrypt hash below corresponds to "Password123!" — for local/dev environments only,
-- never applied against a production datasource in the intended pipeline.)
INSERT INTO users (email, password_hash, role)
VALUES ('admin@hospital.test', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5b2z0JGSeGAJZbgpNqOZ1hrQpG9Wu', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
