# Entity-Relationship Diagram
## Enterprise Healthcare Platform (v1 scope)

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : has
    USERS ||--o{ VISITS : "attends as staff"
    USERS ||--o{ MEDICAL_HISTORY_ENTRIES : records
    USERS ||--o{ PRESCRIPTIONS : prescribes
    PATIENTS ||--o{ MEDICAL_HISTORY_ENTRIES : has
    PATIENTS ||--o{ VISITS : has
    PATIENTS ||--o{ PRESCRIPTIONS : has
    PATIENTS ||--o{ INVOICES : billed
    VISITS ||--o{ PRESCRIPTIONS : generates
    VISITS ||--o{ LAB_REQUESTS : generates
    VISITS ||--o| INVOICES : "billed via"
    MEDICATIONS ||--o{ PRESCRIPTIONS : "prescribed as"
    LAB_REQUESTS ||--|| LAB_RESULTS : produces
    INVOICES ||--o{ PAYMENTS : "paid via"

    USERS {
        uuid id PK
        varchar email
        varchar password_hash
        enum role
        boolean mfa_enabled
    }
    PATIENTS {
        uuid id PK
        varchar first_name
        varchar last_name
        date date_of_birth
        enum sex
    }
    MEDICAL_HISTORY_ENTRIES {
        uuid id PK
        uuid patient_id FK
        varchar entry_type
        text description
    }
    VISITS {
        uuid id PK
        uuid patient_id FK
        uuid attending_staff_id FK
        timestamp visit_date
        enum status
        varchar diagnosis_code
    }
    PRESCRIPTIONS {
        uuid id PK
        uuid patient_id FK
        uuid visit_id FK
        uuid prescribed_by_id FK
        varchar medication_name
        varchar dosage
        enum status
    }
    LAB_REQUESTS {
        uuid id PK
        uuid visit_id FK
        varchar test_type
        enum status
    }
    LAB_RESULTS {
        uuid id PK
        uuid lab_request_id FK
        jsonb result_data
    }
    MEDICATIONS {
        uuid id PK
        varchar name
        int stock_quantity
    }
    INVOICES {
        uuid id PK
        uuid patient_id FK
        uuid visit_id FK
        numeric total_amount
        enum status
    }
    PAYMENTS {
        uuid id PK
        uuid invoice_id FK
        numeric amount
        enum method
    }
    REFRESH_TOKENS {
        uuid id PK
        uuid user_id FK
        varchar token_hash
        boolean revoked
    }
```

Rendered version: GitHub renders Mermaid natively in Markdown previews. See [DATABASE_DESIGN.md](DATABASE_DESIGN.md) for column-level detail and indexing strategy.
