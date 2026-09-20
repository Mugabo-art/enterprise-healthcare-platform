# Software Requirements Specification (SRS)
## Enterprise Healthcare Platform

**Version:** 1.0
**Status:** Draft for v1 scope

---

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements for the Enterprise Healthcare Platform (EHP), a web-based system enabling hospitals to manage patients, clinical staff, laboratory workflows, pharmacy inventory, billing, and reporting.

### 1.2 Scope
EHP is an internal, multi-role hospital operations system. It is **not** a public patient portal, a payment processor, or a medical device — it does not make diagnostic decisions; it records and surfaces information for licensed clinical staff.

### 1.3 Intended Audience
Engineers extending the system, technical reviewers assessing the codebase, and (fictionally) hospital IT stakeholders evaluating fit.

### 1.4 Definitions

| Term | Meaning |
|---|---|
| JWT | JSON Web Token — signed, stateless auth token |
| MFA | Multi-Factor Authentication |
| RBAC | Role-Based Access Control |
| PHI | Protected Health Information |
| SRS | This document |

---

## 2. Overall Description

### 2.1 User Roles

| Role | Description |
|---|---|
| Admin | Manages users, roles, hospital configuration |
| Doctor | Views/edits assigned patients, writes prescriptions & notes |
| Nurse | Views patient records, updates vitals/visit notes |
| Lab Technician | Manages test requests and results |
| Pharmacist | Manages inventory and dispensing |
| Billing Clerk | Manages invoices and payments |
| Patient (future) | Read-only self-service portal — out of scope for v1 |

### 2.2 Assumptions & Constraints
- Single-tenant per deployment (one hospital per running instance) for v1; multi-tenancy is a documented future extension.
- English-only UI for v1.
- Deployed within a hospital's own infrastructure or private cloud — not exposed as a public SaaS.

---

## 3. Functional Requirements

### FR-1 Authentication & Authorization
- FR-1.1 Users authenticate via email + password.
- FR-1.2 System issues a short-lived JWT access token (15 min) and a long-lived refresh token (7 days), refresh tokens are rotated and revocable.
- FR-1.3 System supports optional TOTP-based MFA per user.
- FR-1.4 All endpoints enforce RBAC by role.
- FR-1.5 Failed logins are rate-limited (5 attempts / 15 min per account).

### FR-2 Patient Management
- FR-2.1 Create, view, update patient demographic records.
- FR-2.2 Attach and version medical history entries per patient.
- FR-2.3 Record visits linked to a patient and an attending doctor.
- FR-2.4 Support file attachments (e.g. scanned documents) per patient, stored outside the primary DB.

### FR-3 Doctor Module
- FR-3.1 Doctors view a schedule of assigned appointments.
- FR-3.2 Doctors issue prescriptions linked to a patient and visit.
- FR-3.3 Doctors record diagnosis codes and free-text notes per visit.

### FR-4 Laboratory
- FR-4.1 Doctors or nurses create lab test requests for a patient.
- FR-4.2 Lab technicians record results against a request.
- FR-4.3 System generates a printable/exportable lab report.

### FR-5 Pharmacy
- FR-5.1 Track medication inventory with stock levels and reorder thresholds.
- FR-5.2 Dispense against a valid prescription, decrementing stock.
- FR-5.3 Alert when stock falls below a configured threshold.

### FR-6 Billing
- FR-6.1 Generate invoices from visits, lab tests, and dispensed medication.
- FR-6.2 Record payments against invoices (partial payments supported).
- FR-6.3 Produce billing reports by date range and department.

### FR-7 Analytics
- FR-7.1 Dashboard of hospital KPIs (patient volume, avg. wait time, bed/lab turnaround, revenue).
- FR-7.2 Role-scoped views (e.g. a doctor sees their own patient load, an admin sees hospital-wide).

### FR-8 Notifications
- FR-8.1 Email notification on appointment creation/change.
- FR-8.2 (Future) SMS and push channels behind the same notification interface.

### FR-9 AI Module
- FR-9.1 Generate a plain-language summary of a patient's recent visit history for a clinician.
- FR-9.2 Conversational appointment-scheduling assistant.
- FR-9.3 AI outputs are advisory only and must be clearly labeled as such; they never auto-write clinical records.

---

## 4. Non-Functional Requirements

| Category | Requirement |
|---|---|
| Performance | P95 API response time < 300ms under 100 concurrent users |
| Availability | 99.5% uptime target for the core (non-AI) modules |
| Security | All PHI encrypted at rest and in transit (TLS 1.2+); passwords hashed with bcrypt; audit log for all PHI access |
| Scalability | Stateless API layer, horizontally scalable behind a load balancer |
| Compliance | Designed with HIPAA-style safeguards in mind (access controls, audit trails, encryption) — this is a portfolio project, not a certified compliant system |
| Observability | All services expose Prometheus metrics; centralized structured logging |
| Maintainability | Modules are independently deployable services in the target architecture (see ARCHITECTURE.md) |
| Data retention | Soft-delete only for clinical records; hard-delete restricted to admin + audit trail |

---

## 5. Out of Scope (v1)
- Multi-hospital / multi-tenant support
- Public patient-facing portal
- Real payment gateway integration (billing module simulates payment capture)
- Native mobile apps
