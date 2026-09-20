# UML Diagrams
## Enterprise Healthcare Platform

## 1. Class Diagram — Auth Module

```mermaid
classDiagram
    class User {
        -UUID id
        -String email
        -String passwordHash
        -Role role
        -boolean mfaEnabled
        -String mfaSecret
    }
    class Role {
        <<enumeration>>
        ADMIN
        DOCTOR
        NURSE
        LAB_TECH
        PHARMACIST
        BILLING_CLERK
    }
    class RefreshToken {
        -UUID id
        -UUID userId
        -String tokenHash
        -Instant expiresAt
        -boolean revoked
    }
    class AuthController {
        +register(RegisterRequest) ResponseEntity
        +login(LoginRequest) ResponseEntity
        +refresh(RefreshRequest) ResponseEntity
        +logout(String token) ResponseEntity
        +enrollMfa(UUID userId) ResponseEntity
    }
    class AuthService {
        +register(RegisterRequest) User
        +authenticate(LoginRequest) AuthTokens
        +rotateRefreshToken(String) AuthTokens
        +verifyMfaCode(UUID, String) boolean
    }
    class JwtService {
        +generateAccessToken(User) String
        +generateRefreshToken(User) String
        +validateToken(String) Claims
    }
    class UserRepository {
        <<interface>>
        +findByEmail(String) Optional~User~
    }
    class RefreshTokenRepository {
        <<interface>>
        +findByTokenHash(String) Optional~RefreshToken~
    }

    User "1" --> "0..*" RefreshToken
    User --> Role
    AuthController --> AuthService
    AuthService --> JwtService
    AuthService --> UserRepository
    AuthService --> RefreshTokenRepository
```

## 2. Sequence Diagram — Login with MFA

```mermaid
sequenceDiagram
    actor U as User
    participant C as AuthController
    participant S as AuthService
    participant R as UserRepository
    participant J as JwtService

    U->>C: POST /api/auth/login {email, password}
    C->>S: authenticate(request)
    S->>R: findByEmail(email)
    R-->>S: User
    S->>S: verify password (bcrypt)
    alt MFA enabled
        S-->>C: 202 MFA_REQUIRED (mfaChallengeId)
        C-->>U: 202 MFA_REQUIRED
        U->>C: POST /api/auth/mfa/verify {challengeId, code}
        C->>S: verifyMfaCode(...)
        S-->>C: success
    end
    S->>J: generateAccessToken(user)
    S->>J: generateRefreshToken(user)
    J-->>S: tokens
    S-->>C: AuthTokens
    C-->>U: 200 {accessToken, refreshToken}
```

## 3. Component Diagram

```mermaid
flowchart LR
    subgraph Frontend
        RC[React Components]
        Ctx[Auth Context]
        API[API Service Layer]
    end
    subgraph Backend Modules
        AuthM[auth]
        PatientM[patient]
        DoctorM[doctor]
    end
    RC --> Ctx --> API
    API -->|REST/JSON + JWT| AuthM
    API -->|REST/JSON + JWT| PatientM
    API -->|REST/JSON + JWT| DoctorM
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the system-level component diagram and [ER_DIAGRAM.md](ER_DIAGRAM.md) for the data model.
