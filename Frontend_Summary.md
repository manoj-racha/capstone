# GreenSure - Frontend Architectural Summary

## 1. Overview
The GreenSure frontend is a Single Page Application (SPA) built using Angular 21, TypeScript, and Tailwind CSS. The application is highly modularized into distinct **features** representing the core modules and roles inside the platform. It leverages Angular's router to serve dynamic pages and guards to restrict access based on the currently logged-in user's role.

---

## 2. Global application structure
*   **Routing & Guards:** Uses Angular Guards (`AuthGuard`, `RoleGuard`) to ensure users can only access endpoints aligned with their `userType`.
*   **Interceptors:** Maintains an HTTP Interceptor (`JwtInterceptor`) to attach Bearer Tokens to outgoing requests targeting the backend server and a global error catching interceptor.
*   **State Management:** Core user state and roles are managed centrally via an `AuthService` and `RxJS` Behavior Subjects. 
*   **Styling:** Designed entirely with Tailwind CSS using utility classes for maximum responsiveness and a cohesive, modern UI (dark mode support, gradient backgrounds, hover micro-animations). 

---

## 3. Core Feature Modules
The frontend source code under `src/app/features/` is split logically into the following directories based on responsibility:

### A. Auth Feature (`/auth`)
Handles everything related to user access and onboarding.
*   **`login` Component:** The centralized login screen for all 3 user types.
*   **`register` Component:** Stepped form capturing user details (Household vs MSME) via dynamic validation parameters.
*   **`forgot-password` & `reset-password` Components:** Forms to request and set up a new password using email resets.

### B. User Feature (`/user`)
The main dashboard and interactive elements for Households and MSMEs.
*   **`welcome` Component:** An introductory onboarding screen where AI prompts can be triggered natively.
*   **`dashboard` Component:** A high-level overview of the user’s current carbon score, active policies, and recent notifications.
*   **`profile` Component:** A form to view and edit personal and operational details.
*   **`score-history` Component:** A visual display (often using charts or lists) charting the evolution of a user's carbon footprint over the years.
*   **`policies` & `my-policies` Components:** Screens differentiating between the global catalog of green insurance and the ones the user has acquired.
*   **`recommendations` Component:** A dedicated screen to view AI-generated sustainability tips.
*   **`notifications` Component:** A unified inbox for system alerts.

### C. Declaration Feature (`/declaration`)
The data-entry pipeline for yearly carbon metrics. Contains a wizard-style flow.
*   **`declaration-start` Component:** Information and consent screen.
*   **`declaration-fill` Component:** The heavily validated dynamic form adapting completely depending on if the user is a Household or MSME.
*   **`declaration-vehicles` Component:** A nested sub-form managing complex arrays of transport/commercial vehicles.
*   **`declaration-review` Component:** A summary screen allowing users to confirm the data before submission.
*   **`declaration-history` Component:** View past submitted declarations and their Verification Statuses.

### D. Agent Feature (`/agent`)
The verification interface for agents auditing the platform's data.
*   **`dashboard` Component:** Overview of an agent's current workload, recent tasks, and general statistics.
*   **`task-detail` & `verify` Components:** The core operational screens where agents review user submissions and execute the Approve/Reject flow (with mandatory text reasons on rejection).
*   **`performance` Component:** Agent statistics (# of declarations verified, average time, etc.).
*   **`notifications` Component:** Task assignment alerts.

### E. Admin Feature (`/admin`)
The overarching control panel for platform administrators.
*   **`dashboard` Component:** High-level platform statistics (total users, average scores, pending queue size).
*   **`users` & `user-detail` Components:** Datatables and detailed screens for user management.
*   **`agents`, `create-agent`, & `agent-detail` Components:** Administrative tools to register new Verifiers and track they pipelines.
*   **`declarations` Component:** A global datatable viewing all declarations across the platform.
*   **`assignments` Component:** The manual override view allowing Admins to break the automated scheduling and enforce manual assignment of any specific declaration to a given agent.

---

## 4. Reusable Shared UI Components
(Stored under `src/app/shared`)
The application relies heavily on standalone shared components built with robust design tokens:
*   **Navbar & Sidebar:** Dynamic, reactive menus listening to the Auth State.
*   **Alerts & Badges:** Dynamically colored indicators for object statuses (e.g., `VERIFIED` = green, `REJECTED` = red).
*   **Forms & Inputs:** Wrapped components to unify validation message displays and input highlights on errors.
*   **Charts/Graphs:** Reusable wrappers around Chart.js or D3.js ensuring clean presentation of metrics.
