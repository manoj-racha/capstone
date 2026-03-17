# GreenSure - Backend Architectural Summary

## 1. Overview
The GreenSure backend consists of a Spring Boot (Java) application providing RESTful APIs. It implements a fully stateless N-Tier Service-Oriented architecture, authenticated via JWTs, and backed by a relational database via Spring Data JPA. The backend acts as the authoritative source of truth for business logic, automated task scheduling, and integration with 3rd-party services like Groq AI.

---

## 2. Infrastructure & Configuration
*   **Security layer:** Configured via `SecurityConfig.java`. It disables CSRF (as it is a stateless API). Endpoints are locked tightly via role-based access (`hasRole("USER")`, `hasRole("AGENT")`, `hasRole("ADMIN")`). Custom intercepts run using the `JwtAuthFilter` class.
*   **Database:** Configured through `application.properties`. It bridges the entity objects via Hibernate/JPA. 
*   **Exception Handling:** Implementations using `@RestControllerAdvice` wrap global errors to emit standardized API error payloads format containing timestamps, HTTP codes, and sanitized messages regardless of the internal exception thrown.

---

## 3. Core Entities (Domain Layer)
*   **User Management:** `User`, `HouseholdProfile`, `MsmeProfile`. 
*   **Carbon Verification:** `CarbonDeclaration`, `DeclarationVehicle`, `AgentAssignment`, `Verification`, `VerifiedVehicle`.
*   **Insurance & Scoring:** `CarbonScore`, `Policy`, `PolicyPlan`, `UserPolicy`.
*   **AI & Communication:** `Recommendation`, `Notification`.

---

## 4. Modules & API Controllers

### A. Authentication Module (`AuthController`, `AuthService`)
*   **Registration Flow:** Differentiates MSME and Household flows, instantly mapping initial Profiles to the base `User` entity.
*   **JWT Creation:** After credential checks the subsystem encodes Claims (User ID, Role, etc.) into cryptographically signed Bearer tokens returned on login.

### B. Carbon Declaration Flow (`DeclarationController`, `DeclarationService`)
*   **Submission Endpoints:** Allows Households/MSMEs to POST extensive JSON payloads tracking metrics (gas, energy, waste, transport).
*   **Status Management:** Safely oversees state transitions (`DRAFT` -> `SUBMITTED`).
*   **Constraints:** Enforces uniqueness of declarations by user and year (`declaration_year`, `user_id`). 

### C. Agent Workflow (`AgentController`, `AgentService`)
*   **Verification:** Agents retrieve assigned declarations, then POST validation outcomes. 
*   **Rejection Mechanics:** If rejected, agents must supply text `rejectionReason`. A negative increment forces the `User` into a `resubmission_count` update cycle.
*   **Automated Scheduling:** Handled by a Spring `@Scheduled` system ensuring timely assignment distribution from the unassigned backlog to available agents.

### D. Admin Management (`AdminController`)
Master controller for full-scope CRUD access. 
*   **Dashboard metrics:** Aggregate calculations of platform utilization.
*   **Manual Assignment:** Specialized API allowing Admins to intercept and assign any declaration to any specific agent manually. Overrides the CRON-based scheduler flow safely. 
*   **Entity Overrides:** Suspending/Activating Users and managing Policy Catalogs manually.

### E. AI Engine Integration (`RecommendationController`, `RecommendationService`)
*   **Groq API Interface:** Uses Spring RestTemplate or WebClient to POST user context and metrics to the remote Groq language model. 
*   **Contextual Tip Generation:** Formats responses into customized `Recommendations` saved persistently for users.

### F. Policy & Scoring Engine (`CarbonScoreController`, `PolicyService`)
*   **Algorithmic Calculation:** Internal logic calculates true carbon emission values from verified datasets, converting usage hours and kwH into equivalent CO2 scores.
*   **Dynamic Discounting:** Maps a user’s updated `CarbonScore` metric against internal matrices to calculate exactly how much standard Policy premiums are discounted. Secure endpoints ensure users can “purchase” policies.

### G. Notification Dispatcher (`NotificationController`, `NotificationService`, `EmailService`)
*   **In-app Events:** Pushes localized DB events to alert Users on Verification outcome changes or Agents on Task Assignments.
*   **Email Syncing:** Asynchronously triggers JavaMailSender configurations to dispatch external validation updates and reset tokens.
