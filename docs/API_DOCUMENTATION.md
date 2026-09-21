# API Documentation
## Enterprise Healthcare Platform

Base URL: `http://localhost:8080/api`
Full machine-readable spec: [`openapi.yaml`](openapi.yaml) — also served live at `/swagger-ui.html` when the backend is running (springdoc-openapi).

All endpoints except `/auth/register` and `/auth/login` require:
```
Authorization: Bearer <accessToken>
```

## Auth

### `POST /auth/register`
Registers a new staff user (admin-only in production; open in dev seed data).

**Request**
```json
{ "email": "doctor@hospital.test", "password": "P@ssw0rd!", "role": "DOCTOR" }
```
**Response `201`**
```json
{ "id": "uuid", "email": "doctor@hospital.test", "role": "DOCTOR" }
```

### `POST /auth/login`
**Request**
```json
{ "email": "doctor@hospital.test", "password": "P@ssw0rd!" }
```
**Response `200`**
```json
{ "accessToken": "...", "refreshToken": "...", "expiresIn": 900 }
```
**Response `202`** (if MFA enabled)
```json
{ "mfaRequired": true, "challengeId": "uuid" }
```

### `POST /auth/refresh`
**Request** `{ "refreshToken": "..." }` → **Response `200`** new token pair.

### `POST /auth/logout`
Revokes the given refresh token. **Response `204`**.

## Patients

| Method | Path | Role required | Description |
|---|---|---|---|
| GET | `/patients` | any staff | Paginated list, `?search=` by name |
| GET | `/patients/{id}` | any staff | Full record incl. history summary |
| POST | `/patients` | ADMIN, NURSE | Create patient |
| PUT | `/patients/{id}` | ADMIN, NURSE | Update demographics |
| POST | `/patients/{id}/history` | DOCTOR, NURSE | Add medical history entry |

## Visits

| Method | Path | Role required | Description |
|---|---|---|---|
| GET | `/patients/{patientId}/visits` | any staff | List a patient's visits |
| GET | `/doctors/{id}/visits` | DOCTOR (self), ADMIN | Doctor's schedule |
| POST | `/patients/{patientId}/visits` | ADMIN, NURSE, DOCTOR | Create/record a visit |
| PUT | `/patients/{patientId}/visits/{visitId}` | DOCTOR | Update notes/diagnosis code/status |

## Prescriptions

| Method | Path | Role required | Description |
|---|---|---|---|
| GET | `/patients/{patientId}/prescriptions` | any staff | List a patient's prescriptions |
| POST | `/patients/{patientId}/visits/{visitId}/prescriptions` | DOCTOR | Issue a prescription linked to the visit |

## Lab

| Method | Path | Role required | Description |
|---|---|---|---|
| POST | `/visits/{id}/lab-requests` | DOCTOR, NURSE | Request a test |
| GET | `/lab-requests?status=REQUESTED` | LAB_TECH | Work queue |
| POST | `/lab-requests/{id}/results` | LAB_TECH | Submit results |

## Pharmacy

| Method | Path | Role required | Description |
|---|---|---|---|
| GET | `/medications` | PHARMACIST, DOCTOR | Inventory list |
| POST | `/medications/{id}/dispense` | PHARMACIST | Dispense against a prescription |

## Billing

| Method | Path | Role required | Description |
|---|---|---|---|
| POST | `/invoices` | BILLING_CLERK | Generate invoice for a visit |
| POST | `/invoices/{id}/payments` | BILLING_CLERK | Record a payment |
| GET | `/reports/billing?from=&to=` | BILLING_CLERK, ADMIN | Billing report |

## Analytics

| Method | Path | Role required | Description |
|---|---|---|---|
| GET | `/analytics/dashboard` | ADMIN | Hospital-wide KPIs |
| GET | `/analytics/dashboard/mine` | DOCTOR | Own patient-load KPIs |

## Error Format

All errors follow a consistent shape:
```json
{ "timestamp": "2026-09-17T10:00:00Z", "status": 403, "error": "Forbidden", "message": "Role NURSE cannot access this resource", "path": "/api/invoices" }
```

## Pagination

List endpoints accept `?page=0&size=20&sort=lastName,asc` and return:
```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 143, "totalPages": 8 }
```
