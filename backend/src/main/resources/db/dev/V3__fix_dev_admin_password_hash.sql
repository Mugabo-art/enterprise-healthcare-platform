-- V2's bcrypt hash for admin@hospital.test did not actually correspond to
-- "Password123!" (the hash was wrong at authoring time), so the documented
-- dev login never worked. Replace it with a hash verified against a real
-- BCryptPasswordEncoder round-trip of "Password123!".
UPDATE users
SET password_hash = '$2a$10$kBAap/Mg8d4UhUFoTNyBSO2jnXYgxO/kiJpQHZ2AL6hW5qj2nxGAm'
WHERE email = 'admin@hospital.test';
