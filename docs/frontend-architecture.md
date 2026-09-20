---

# 2. `docs/frontend-architecture.md`

```md
# SmartLink Frontend Architecture

## 1. Purpose

The SmartLink frontend is a supporting interface inside the existing Spring Boot application.

The backend remains the core of SmartLink.

The frontend must expose existing backend functionality without introducing unnecessary frontend complexity.

---

## 2. Technology

Use the existing Spring Boot application with:

- Thymeleaf
- HTML
- CSS
- minimal vanilla JavaScript where useful

Do not introduce:

- React
- Vue
- Angular
- Vite
- Redux
- SPA routing
- a separate frontend repository
- a frontend state-management framework

The frontend should remain lightweight.

---

## 3. Source of Truth

The backend is authoritative for all application behavior.

Frontend implementation must be derived from the actual backend source code.

Before implementing a feature, inspect the relevant backend:

- controllers
- DTOs
- services
- security configuration
- validation
- exception handling
- response structures
- authentication behavior
- sessions
- cookies
- link processing
- analytics
- reporting

Do not invent backend contracts.

Do not guess:

- endpoint paths
- request fields
- response fields
- DTO structures
- error codes
- cookie names
- session attributes
- authentication behavior
- link statuses

---

## 4. Backend Must Not Be Changed for Frontend Convenience

Frontend implementation must adapt to the existing backend.

Do not modify backend logic simply to make a frontend implementation easier.

If a genuine backend/frontend contract problem is discovered:

1. identify it
2. document it
3. determine whether it is actually necessary to change
4. do not silently alter backend behavior

---

## 5. Thymeleaf Structure

The frontend should live under the existing Spring Boot resource structure.

General organization:

```text
src/main/resources/
│
├── templates/
│   ├── fragments/
│   └── page templates
│
└── static/
    ├── css/
    ├── js/
    ├── images/
    └── favicon/
````

Exact filenames and subdirectories may be adjusted after inspecting the existing project.

Do not create unnecessary directory depth.

---

## 6. Thymeleaf Fragments

Use Thymeleaf fragments for genuinely reusable UI.

Potential shared concepts include:

* document head
* navbar
* footer
* page container
* common messages
* common form structures
* reusable UI elements

Do not create a fragment merely because two pieces of markup happen to look similar.

A fragment should exist when:

* it is reused
* it represents a coherent UI concept
* centralizing it improves consistency

---

## 7. CSS Organization

Use shared CSS for the common design system.

The design system should define reusable concepts such as:

* colors
* typography
* spacing
* buttons
* inputs
* cards
* navigation
* status indicators
* layout containers

Prefer CSS custom properties for design tokens.

Do not scatter repeated colors, spacing values, and component dimensions throughout individual page stylesheets.

Page-specific styles may be introduced when genuinely necessary.

---

## 8. JavaScript Philosophy

JavaScript should be minimal.

Use JavaScript when it meaningfully improves the user experience.

Appropriate examples:

* copy-to-clipboard
* password visibility
* OTP input behavior
* confirmation dialogs
* small interactive controls
* lightweight UI state
* analytics visualization

Do not move core business logic into JavaScript.

Do not recreate backend business rules in JavaScript.

Do not create a client-side application state machine unless there is a demonstrated need.

---

## 9. Server Rendering

Server rendering should be the default.

Prefer:

```text
Browser
   ↓
Spring Controller
   ↓
Backend service
   ↓
Model
   ↓
Thymeleaf template
   ↓
HTML
```

over building an API-driven SPA for ordinary page rendering.

Use client-side requests only when they provide a meaningful UX benefit.

---

## 10. Page Responsibility

Each page must have one primary responsibility.

A page may contain supporting UI required to complete that responsibility.

It should not combine unrelated features.

The planned responsibilities are:

### Home

Introduce SmartLink and direct users toward the application.

### Signup

Collect information required to begin account creation.

### Signup Verification

Verify the signup OTP and complete the account-creation flow.

### Login

Authenticate an existing user.

### Shorten URL

Create a shortened SmartLink.

### My Links

View and manage the user's created links.

### Link Analytics

Analyze one selected short link.

### Report Abuse

Allow reporting of a SmartLink/problematic link.

### Behind SmartLink

Explain the project, creator, architecture, technologies, and engineering decisions.

### Existing Track/Redirect Page

Perform the existing tracking and redirect responsibility.

---

## 11. Navigation Between Responsibilities

Pages may link to other pages when another responsibility is relevant.

For example:

```text
Shorten URL
    ↓
Link created
    ↓
My Links / Analytics
```

But navigation does not mean responsibilities should be merged.

For example:

```text
My Links
    ↓
Analytics
```

should navigate to the analytics responsibility rather than embedding the entire analytics experience into My Links.

---

## 12. Authentication

Authentication behavior must be derived from the actual Spring Security/backend implementation.

Do not create an independent frontend authentication architecture.

The frontend must respect the backend's:

* access-token mechanism
* refresh-token mechanism
* cookies
* sessions
* authentication endpoints
* authorization rules
* logout behavior

Do not store sensitive authentication information in browser storage unless the existing backend/security architecture
explicitly requires it.

Do not attempt to expose HttpOnly cookies to JavaScript.

---

## 13. Signup Flow

The signup process consists of the backend-defined multi-stage flow.

The frontend should represent those stages clearly.

General flow:

```text
Signup page
    ↓
Signup initiation
    ↓
Signup verification
    ↓
Authentication completed
    ↓
Authenticated application
```

The exact endpoint names, request fields, response structures, session behavior, and authentication details must be
discovered from the backend.

Do not hardcode them into architecture documentation.

---

## 14. Protected Pages

Pages requiring authentication should rely on the existing backend security model.

Do not attempt to duplicate authorization rules in frontend JavaScript.

Frontend behavior may improve the user experience, but the backend remains responsible for actual access control.

---

## 15. Error Handling

The frontend should consume the backend's existing error contract.

Do not invent a second error-code system.

Error handling should distinguish between:

* validation errors
* authentication errors
* authorization errors
* business/application errors
* unexpected server errors

The frontend should present useful information to the user without exposing internal implementation details.

Do not display:

* stack traces
* raw server exceptions
* internal implementation details

---

## 16. Loading States

Every operation that may take noticeable time should provide appropriate feedback.

Examples:

* form submission
* OTP verification
* link creation
* link deletion
* analytics loading

Loading states should prevent accidental duplicate submissions where appropriate.

---

## 17. Empty States

Pages that display collections or analytical information should define useful empty states.

Examples include:

* no links created
* no analytics available
* no report data where applicable

An empty state should explain the situation and, where appropriate, provide the next useful action.

---

## 18. Track/Redirect Page

An existing tracking/redirect page is part of the current application.

Inspect its implementation and backend contract before making changes.

This page is a special case and should not be forced into the general page architecture.

Do not rename, remove, or reinterpret values used by the existing tracking flow merely to make naming consistent with
the new frontend.

Preserve its functional contract.

Visual changes may be considered separately as long as they do not alter its behavior or contract.

---

## 19. Backend Contract Discovery

Before implementing any page that communicates with the backend:

1. Find the relevant controller.
2. Identify the actual request structure.
3. Identify the actual response structure.
4. Trace the service behavior.
5. Identify validation rules.
6. Identify possible application errors.
7. Identify authentication/authorization requirements.
8. Determine the appropriate Thymeleaf rendering or form-submission flow.
9. Implement according to the discovered behavior.

Never derive API behavior from assumptions.

---

## 20. Page Implementation Order

The frontend should be implemented incrementally.

Recommended order:

### Foundation

* common layouts
* Thymeleaf fragments
* global CSS/design system
* common UI
* navigation
* favicon/logo

### Authentication

* signup
* signup verification
* login
* logout/authentication integration

### Core product

* shorten URL
* generated-link result
* My Links
* link actions

### Analytics

* selected-link analytics
* analytics visualizations where useful

### Reporting

* public reporting flow

### Presentation

* Home
* Behind SmartLink

### Existing integration

* review/refine the existing tracking/redirect page without changing its backend contract

---

## 21. Page-by-Page Implementation

Do not implement the entire frontend in one operation.

Each page should be implemented as an isolated task.

For every page:

1. Inspect its backend contract.
2. Define its responsibility.
3. Define the required Thymeleaf template.
4. Identify reusable fragments.
5. Define required model data.
6. Define form submission/navigation behavior.
7. Define success state.
8. Define validation state.
9. Define error state.
10. Define loading/feedback behavior where applicable.
11. Implement.
12. Verify against the actual backend.

---

## 22. Avoid Frontend Over-Engineering

The frontend is not the core of SmartLink.

Do not introduce abstractions simply because they are common in large frontend applications.

Prefer:

```text
simple Thymeleaf template
+
reusable fragment
+
shared CSS
+
small JavaScript
```

over:

```text
large frontend framework
+
state management
+
client-side routing
+
complex component architecture
```

unless an actual requirement justifies it.

---

## 23. Consistency Rule

All pages must follow:

`frontend-design.md`

The visual system should remain consistent across:

* authentication
* link management
* analytics
* reporting
* informational pages

Individual pages may have different layouts because their responsibilities differ, but they should still clearly belong
to the same SmartLink application.

---

## 24. Documentation Rule

When a frontend architectural decision changes, update this document.

When a visual/design decision changes, update:

```text
frontend-design.md
```

The documentation should remain synchronized with the implementation.

---

## 25. Implementation Principle

The overall architecture should remain:

```text
Spring Boot backend
        ↓
Thymeleaf rendering
        ↓
HTML/CSS
        ↓
minimal JavaScript where useful
```

The backend provides the capabilities.

Thymeleaf exposes those capabilities.

CSS provides the visual system.

JavaScript provides targeted interaction improvements.

No layer should take responsibility away from the layer that already owns it.

````

# Where to put them

Because you're using the **existing SmartLink repository**, I'd place them at the root:

```text
smart-link/
│
├── .git/
├── .mvn/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       ├── templates/
│   │       └── static/
│   │
│   └── test/
│
├── docs/                         ← NEW
│   ├── frontend-design.md        ← NEW
│   └── frontend-architecture.md ← NEW
│
├── pom.xml
├── Dockerfile
├── README.md
└── ...
````