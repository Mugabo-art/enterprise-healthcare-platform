# Database Design
## Enterprise Healthcare Platform (PostgreSQL)

## 1. Design Principles
- Normalized to 3NF for transactional tables (patients, visits, prescriptions, invoices) — avoids update anomalies on data that changes independently (e.g. a patient's address shouldn't require touching visit rows).
- Soft deletes (`deleted_at TIMESTAMP NULL`) on clinical tables — clinical records are never hard-deleted, only admin actions can hard-delete, and only non-clinical tables (e.g. expired invite tokens).
- Every table has `created_at` / `updated_at` for audit purposes.
- Foreign keys enforced at the DB level, not just application level — data integrity should not depend on every code path remembering to check.

## 2. Core Tables (v1 scope)

### users
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| email | VARCHAR UNIQUE | |
| password_hash | VARCHAR | bcrypt |
| role | ENUM | ADMIN, DOCTOR, NURSE, LAB_TECH, PHARMACIST, BILLING_CLERK |
| mfa_secret | VARCHAR NULL | TOTP secret, set on MFA enrollment |
| mfa_enabled | BOOLEAN | default false |
| created_at / updated_at | TIMESTAMP | |

### refresh_tokens
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK → users.id | |
| token_hash | VARCHAR | never store raw token |
| expires_at | TIMESTAMP | |
| revoked | BOOLEAN | default false |

### patients
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| first_name / last_name | VARCHAR | |
| date_of_birth | DATE | |
| sex | ENUM | |
| contact_phone / contact_email | VARCHAR | |
| address | VARCHAR | |
| created_at / updated_at / deleted_at | TIMESTAMP | |

### medical_history_entries
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| patient_id | UUID FK → patients.id | indexed |
| entry_type | VARCHAR | e.g. "allergy", "chronic_condition", "surgery" |
| description | TEXT | |
| recorded_by | UUID FK → users.id | |
| recorded_at | TIMESTAMP | |

### visits
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| patient_id | UUID FK → patients.id | indexed |
| attending_staff_id | UUID FK → users.id | indexed; DOCTOR or NURSE — visit creation isn't doctor-only |
| visit_date | TIMESTAMP | |
| visit_type | ENUM | OUTPATIENT, INPATIENT, EMERGENCY, FOLLOW_UP |
| status | ENUM | SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED — DOCTOR-only to update |
| reason | VARCHAR | |
| notes | TEXT | |
| diagnosis_code | VARCHAR NULL | e.g. ICD-10; DOCTOR-only to set |

### prescriptions
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| patient_id | UUID FK → patients.id | indexed |
| visit_id | UUID FK → visits.id | indexed |
| prescribed_by_id | UUID FK → users.id | |
| medication_name | VARCHAR | free-text; becomes a `medication_id` FK once the pharmacy module ships a catalog |
| dosage | VARCHAR | |
| frequency | VARCHAR | |
| duration_days | INT NULL | |
| instructions | TEXT | |
| status | ENUM | ACTIVE, COMPLETED, CANCELLED |

### lab_requests
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| patient_id | UUID FK → patients.id | indexed — denormalized alongside visit_id so listing a patient's lab requests doesn't require joining through visits, matching the `prescriptions` table's shape |
| visit_id | UUID FK → visits.id | indexed |
| test_type | VARCHAR | |
| status | ENUM | REQUESTED, IN_PROGRESS, COMPLETED, CANCELLED — CANCELLED added to match how `visits`/`prescriptions` already model workflow state |
| notes | TEXT | |
| requested_by | UUID FK → users.id | |

### lab_results
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| lab_request_id | UUID FK → lab_requests.id | UNIQUE (1:1) |
| result_data | JSONB | flexible per test type |
| recorded_by | UUID FK → users.id | |
| recorded_at | TIMESTAMP | |

### medications
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| name | VARCHAR(150) | unique |
| unit | VARCHAR(30) | e.g. tablets, ml |
| stock_quantity | INTEGER | CHECK >= 0 |
| reorder_threshold | INTEGER | low-stock when stock_quantity <= threshold |
| active | BOOLEAN | inactive items cannot be dispensed |
| created_at / updated_at | TIMESTAMP | |

`unit_price` is deferred to the billing module.

### dispensations
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| patient_id | UUID FK → patients.id | |
| prescription_id | UUID FK → prescriptions.id | UNIQUE — one fill per prescription |
| medication_id | UUID FK → medications.id | |
| quantity | INTEGER | CHECK > 0 |
| dispensed_by_id | UUID FK → users.id | |
| dispensed_at | TIMESTAMP | |

### invoices
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| patient_id | UUID FK → patients.id | indexed |
| visit_id | UUID FK → visits.id NULL | |
| total_amount | NUMERIC(10,2) | |
| status | ENUM | DRAFT, ISSUED, PARTIALLY_PAID, PAID, VOID |

### payments
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| invoice_id | UUID FK → invoices.id | indexed |
| amount | NUMERIC(10,2) | |
| paid_at | TIMESTAMP | |
| method | ENUM | CASH, CARD, INSURANCE |

## 3. Indexing Strategy
- All foreign key columns indexed (Postgres does not auto-index FKs).
- `patients (last_name, first_name)` composite index for name search.
- `visits (doctor_id, scheduled_at)` for schedule queries.
- `lab_results.result_data` uses a GIN index if querying inside the JSONB becomes common.
- `medications.stock_quantity` combined with a partial index (`WHERE stock_quantity < reorder_threshold`) to make the pharmacy low-stock alert query cheap.

## 4. Migration Strategy
Schema changes are managed via Flyway (`backend/src/main/resources/db/migration/V*.sql`) — every change is a versioned, reviewable SQL file, never a manual production edit.

See [ER_DIAGRAM.md](ER_DIAGRAM.md) for the visual relationships.
