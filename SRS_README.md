# Software Requirements Specification (SRS) - GreenSure

## 1. Project Overview
**Project Name:** GreenSure  
**Purpose:** GreenSure is a comprehensive platform designed to help households and Micro, Small, and Medium Enterprises (MSMEs) track, manage, and reduce their carbon footprints. It incentivizes sustainable practices by calculating carbon scores based on user declarations and offering discounted green insurance policies and actionable AI-driven recommendations.  
**Target Users:** Households, MSMEs, Agents (Verifiers), and System Administrators.  
**Technology Stack:**
*   **Frontend:** Angular 21, Tailwind CSS, TypeScript
*   **Backend:** Java, Spring Boot 3+, Spring Security (JWT)
*   **Database:** Relational Database (e.g., MySQL/PostgreSQL, H2 for dev)
*   **Architecture:** RESTful API Layered Architecture
*   **AI Integration:** Groq API for dynamic recommendations and onboarding

## 2. User Roles & Responsibilities
*   **User (Household / MSME):** Can register, log in, manage their profile, submit annual carbon declarations, view their carbon scores, read personalized recommendations, and purchase green insurance policies at discounted rates.
*   **Agent:** Responsible for data integrity. Can log in, view assigned user declarations, perform verification (approve or reject with reasons), and manage their verification pipeline.
*   **Admin:** Has overarching control. Can manually assign/reassign agents to declarations, manage user accounts, oversee platform activities, and manage the insurance policy catalog.

## 3. Key Features / Modules
*   **Authentication & Authorization:** Secure JWT-based login, role-based access control (USER, AGENT, ADMIN).
*   **Carbon Declaration Module:** Dynamic forms capturing detailed metrics (energy usage, transport, lifestyle for households; operations, supply chain for MSMEs).
*   **Agent Assignment & Verification:** Automated chronological scheduling and manual admin overrides for assigning declarations to agents for audit.
*   **Carbon Scoring Engine:** Processes verified data to compute an accurate carbon emission score.
*   **Recommendations Engine (AI-Powered):** Generates actionable, tailored advice using the Groq API based on user profiles and declaration data.
*   **Green Insurance Policy Module:** Offers curated insurance plans and dynamic premium discounts tied directly to the user's carbon score.
*   **Notification System:** Alerts users and agents regarding assignment updates, declaration statuses, and new recommendations.

## 4. Functional Requirements
*   **Authentication:** The system must securely authenticate users via stateless JWTs.
*   **Declarations:** The system must handle unique attributes depending on user type (e.g., AC usage and dietary patterns for Households vs. commercial vehicles and raw materials for MSMEs). 
*   **Lifecycle Flow:** Declarations follow a strict lifecycle flow: `DRAFT` → `SUBMITTED` → `UNDER_VERIFICATION` → `VERIFIED` (or `REJECTED`).
*   **Verification:** Agents must be able to reject declarations and provide a specific text reason, incrementing the user's `resubmission_count`.
*   **Scoring & Policies:** The system must calculate a carbon score post-verification and use this score as a variable to compute policy premium discounts.

## 5. Non-Functional Requirements
*   **Security:** Passwords must be hashed using BCrypt. APIs must be secured by Spring Security. CORS must be configured strictly for the Angular frontend.
*   **Performance:** The REST API should ideally respond in under 200ms for standard CRUD operations to ensure a snappy frontend experience.
*   **Scalability:** Stateless backend design allows horizontal scaling of the Spring Boot application.
*   **Usability:** The UI must be highly responsive, utilizing modern Tailwind CSS classes for an aesthetic and accessible user experience across various devices.

## 6. Database Design
*   **Core Tables:**
    *   `users`: Stores credentials, roles, profiles, and statuses.
    *   `carbon_declarations`: Stores submitted yearly environmental metrics. Includes unique constraint on `user_id` + `declaration_year`.
    *   `carbon_scores`: Stores the final output of the scoring engine.
    *   `verifications`: Audit trail of agent approvals/rejections.
    *   `agent_assignments`: Mapping table for Agent-to-Declaration scheduling.
    *   `policies` & `user_policies`: Insurance catalog and user purchases.
    *   `recommendations`: AI-generated tips and insights.

## 7. System Architecture
*   **Pattern:** N-Tier Layered Architecture / MVC.
*   **Layers:**
    *   **Presentation Layer:** Angular Single Page Application (SPA).
    *   **Controllers:** RESTful Spring `@RestController` endpoints.
    *   **Service Layer:** Business logic, integration with AI (Groq), and scoring calculations.
    *   **Data Access Layer:** Spring Data JPA Repositories for database interaction.
    *   **Security Layer:** Spring Security filter chain intercepting requests for JWT validation.

## 8. Workflows & Diagrams
*   **End-to-End Workflow:**
    1. User registers and completes onboarding.
    2. User fills out and submits a Carbon Declaration.
    3. Custom Scheduler / Admin assigns the declaration to an Agent.
    4. Agent reviews the data and marks it as `VERIFIED`.
    5. Scoring Engine triggers, calculating the user's Carbon Score.
    6. Recommendation Engine triggers, providing next steps.
    7. User uses their new score to purchase a discounted Green Insurance Policy.
*   **Suggested Diagrams to Create:** Use Case Diagram, Entity-Relationship Diagram (ERD), Activity Diagram (Declaration Lifecycle).

## 9. Admin / Search Features
*   **Dashboard Filters:** Admins need filtering capabilities to sort Users by type (Household/MSME) and status. 
*   **Assignment Management:** Admins can filter declarations by Unassigned vs. Active, and filter by status (`SUBMITTED`, `UNDER_VERIFICATION`).
*   **Agent Views:** Agents can filter their assigned tasks to differentiate between pending reviews and completed verifications.

## 10. Anything Else
*   **UI/UX:** The frontend requires robust form validation (e.g., highlighting invalid fields with specific styling) and proper error handling. Currency symbols and localizations should be consistently applied.
*   **Integrations:** Potential future scope for deep third-party API integrations (e.g., external carbon registries or advanced email/SMS providers).
*   **Schedulers:** The system relies on a Spring `@Scheduled` task (`AgentAssignmentScheduler`) to automatically distribute the verification workload among available agents.
