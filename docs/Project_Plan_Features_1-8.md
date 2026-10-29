# Medical Camp Feedback Portal
## Project Plan: Level 1, Features 1–8

**Scope:**

| # | Feature | # | Feature |
|---|---|---|---|
| 1 | Login and roles | 5 | Raising a complaint |
| 2 | Camp management | 6 | Handling complaints |
| 3 | QR codes and links | 7 | Resolution and follow-up |
| 4 | Feedback form | 8 | Dashboards |

**Out of scope for this plan:** features 9–15 (text analysis, suggestions, camp comparison, accessibility score, reports and exports, admin settings, full privacy jobs).

The plan does leave room for them:
- the dashboards keep a space for the keyword themes from feature 9
- the database tables already include the fields those features will need

**Stack:** Spring Boot 4 (Java 21) REST API + React 19/TypeScript/Vite + Mantine, built into one JAR. The database is PostgreSQL, with H2 during development.

**Estimate:** about 25 working days (5 weeks) for one developer. Phases run in order because each one builds on the previous one. Testing happens inside each phase, not only at the end.

---

## Phase Overview

| Phase | Name | Features | Est. days | Depends on |
|---|---|---|---|---|
| 0 | Foundation and setup | n/a | 3 | n/a |
| 1 | Login and roles | 1 | 3 | 0 |
| 2 | Camp management | 2 | 2 | 1 |
| 3 | QR codes and links | 3 | 1.5 | 2 |
| 4 | Feedback form | 4 | 3.5 | 3 |
| 5 | Raising a complaint | 5 | 1.5 | 4 |
| 6 | Handling complaints | 6 | 4 | 5 |
| 7 | Resolution and follow-up | 7 | 1.5 | 6 |
| 8 | Dashboards | 8 | 3.5 | 4, 6, 7 |
| 9 | Stabilize and demo | 1–8 | 1.5 | all |
| | **Total** | | **~25** | |

```
Week 1: [Phase 0 ][Phase 1 ]
Week 2: [Ph 2][Ph 3][Phase 4 ...
Week 3: ...Ph 4][Ph 5][Phase 6 ...
Week 4: ...Phase 6 ][Ph 7][Phase 8 ...
Week 5: ...Phase 8 ][Phase 9]
```

---

## Phase 0: Foundation and Setup (3 days)

**Goal:** an empty app that builds into one JAR, runs on port 8080, and shows a React page served by Spring Boot.

**Design**
- Draw the ER diagram for these tables: User (with a role value), Department, Camp, CampService, Feedback, FeedbackRating, Complaint, ComplaintAction, FollowUp, Notification, Setting, AuditLog. Each phase adds its own tables as a new Flyway migration.
- Wireframes (paper or Figma) for:
  - the login page and the app layout
  - the camp list and camp form
  - the camp poster
  - the attendee form, the thank-you screen and the "check my complaint" page
  - the complaint table, the complaint board and the complaint details page
  - the three dashboards
- API conventions:
  - `/api/...` needs a login; `/api/public/...` does not
  - one standard error format (JSON)
  - lists support paging

**Backend**
- Spring Boot project with these modules: Web, Security, Data JPA, Validation, Mail (left switched off for now), and the database driver.
- **Flyway** for database changes, so every table change is versioned and can be repeated.
- Profiles: `dev` uses H2 and `prod` uses PostgreSQL.
- `app.public-base-url` setting.
- springdoc-openapi (Swagger UI).
- A global error handler and request validation.

**Frontend**
- A Vite + React + TypeScript project with Mantine, React Router, TanStack Query, react-i18next (`en`/`hi` files), React Hook Form and Zod.
- The `/api` proxy to port 8080.
- One shared API client that sends the CSRF token automatically.

**Build**
- `frontend-maven-plugin` builds React into `src/main/resources/static`.
- A fallback route sends page addresses like `/camps/12` to `index.html`, so refreshing the browser still works.

**GitHub and quality**
- A private GitHub repository set up as described in the "GitHub Workflow" section below:
  - `.gitignore`
  - the `main` branch protected
  - GitHub Actions running the build and tests
  - a project board with one milestone per phase
- A README covering requirements, how to run the project in dev, how to build it, and how to run it on the hotspot.
- A code formatter (Prettier), a linter (oxlint), and a basic test of each kind: JUnit and Vitest.

**Done when**
- `mvn package` produces one JAR.
- `java -jar app.jar` serves the React page at `http://localhost:8080`.
- Swagger UI opens.
- Flyway creates the database tables.
- The first push to GitHub passes the CI build.

---

## Phase 1: Login and Roles, Feature 1 (3 days)

**Goal:** staff can log in, and each role sees only what it is allowed to see.

**Backend**
- `users` and `departments` tables. Each user has one role: `SUPER_ADMIN`, `ORGANIZER`, `MEDICAL_STAFF` or `ANALYST`.
- Spring Security with a session cookie, CSRF protection and BCrypt hashing.
- Login and logout endpoints, plus `GET /api/me`, which returns the current user and role.
- URL rules plus `@PreAuthorize` on each service. `/api/public/**` stays open.
- User management for admins: create, edit, disable, reset password, change role, and link medical staff to a department.
- A first admin account created on startup from settings or environment variables.
- Login limits: lock an account briefly after repeated failed attempts.

**Frontend**
- A login page.
- The app layout:
  - side menu filtered by role
  - top bar with user menu, language switch and a placeholder for notifications
- Protected routes and a "no access" page.
- A users page with a table and a create/edit dialog.

**Tests**
- Every endpoint is tested with each role (MockMvc), so that "staff cannot call admin endpoints" is checked on the server.

**Done when**
- All four roles log in and see different menus.
- Calling a forbidden endpoint directly returns 403.

---

## Phase 2: Camp Management, Feature 2 (2 days)

**Goal:** organizers manage camps and their services.

**Backend**
- `Camp` table:
  - name, start and end date, location, organizer, status (`UPCOMING`/`ACTIVE`/`CLOSED`/`ARCHIVED`)
  - feedback window start and end
  - `publicToken`: random, unique, about 8 characters
- `CampService` table, linking a camp to its departments and services.
- Create, edit and archive endpoints. Organizers can edit only their own camps; admins can edit all.
- List endpoints with filters for status, date and location.
- Rule: the feedback form is open only when the camp is Active and today's date is inside the feedback window.

**Frontend**
- A camp list (table with filters).
- A create/edit form with date pickers and a list of services.
- A camp details page with tabs: Overview, Services, QR (Phase 3), Feedback (Phase 4), Complaints (Phase 6).

**Done when**
- An organizer can create a camp with services, open its feedback window and archive it.
- Another organizer cannot edit that camp.

---

## Phase 3: QR Codes and Links, Feature 3 (1.5 days)

**Goal:** every camp has a printable QR poster that opens its form.

**Backend**
- `GET /api/camps/{id}/qr.png` uses ZXing to create a QR code for `{public-base-url}/f/{publicToken}`.
- `GET /api/camps/{id}/poster.pdf` uses OpenPDF to create an A4 poster with:
  - the camp name
  - the QR code and the short typed link
  - English and Hindi instructions
  - an optional Wi-Fi QR code (`WIFI:T:WPA;S:<ssid>;P:<pwd>;;`), with the hotspot name and password stored as settings
- Regenerating the token, which invalidates the old QR code.

**Frontend**
- The QR tab:
  - QR preview and a copy-link button
  - download PNG and download poster buttons
  - a regenerate button with a confirmation step

**Hotspot test (important)**
- Run the JAR, turn on the Windows Mobile Hotspot, and set `app.public-base-url=http://192.168.137.1:8080`.
- Allow port 8080 through Windows Firewall for Private networks.
- Scan the code with two different phones, one Android and one iPhone if possible.

**Done when**
- A phone on the hotspot scans the poster and reaches `/f/{token}`, even if the page is still a placeholder.

---

## Phase 4: Feedback Form, Feature 4 (3.5 days)

**Goal:** attendees submit feedback on their phones, quickly and safely.

**Backend**
- `Feedback` table:
  - camp, service, comment, anonymous flag
  - contact details (optional) and consent flag
  - assisted flag and the staff member who helped
  - language, submission time, `submissionToken` (unique)
- `FeedbackRating` table: feedback, category, score from 1 to 5.
- The fixed categories are stored as seed data for now; feature 14 will make them editable:
  - registration
  - waiting time
  - staff behaviour
  - cleanliness
  - information
  - overall experience
- Public endpoints:
  - `GET /api/public/camps/{token}` returns the camp name, its services and whether the form is open.
  - `GET /api/public/camps/{token}/form-token` returns a one-time submission token.
  - `POST /api/public/camps/{token}/feedback` validates the submission, rejects a reused token, and saves the feedback.
- Duplicate prevention:
  - the one-time token
  - a simple in-memory rate limit per IP address and camp
  - a cookie on the device that shows "you already submitted, submit again?" as a soft warning
- Staff-assisted submission: the same endpoint called with a logged-in volunteer session sets the assisted flag.
- Contact details are accepted only when consent is true.

**Frontend (mobile-first, separate public layout with no staff menu)**
- Route `/f/:token`.
- Language selection first: big English and हिन्दी buttons.
- A short step-by-step form:
  1. Choose a service
  2. Ratings (large star or emoji buttons)
  3. Comment
  4. A "Report a problem?" toggle (the full complaint part comes in Phase 5)
  5. Follow-up consent and contact details
- A consent notice, a progress indicator and large touch targets.
- The finished draft is kept in `localStorage`, so a refresh does not lose answers.
- Screens for "Form closed" and "Invalid link".
- An assisted mode entry point for logged-in staff.

**Tests**
- Validation edge cases, a reused token, a closed window and an invalid token.
- Basic accessibility: keyboard use, labels, contrast.

**Done when**
- Over the hotspot, a phone submits feedback in both languages.
- A duplicate submission is blocked.
- The feedback appears in the camp's Feedback tab (a simple list for now).

---

## Phase 5: Raising a Complaint, Feature 5 (1.5 days)

**Goal:** a problem reported in the form becomes a trackable complaint.

**Backend**
- `Complaint` table:
  - linked feedback and camp, category, description
  - `referenceNo`, priority, status (`OPEN` at first)
  - assignee, due date, timestamps
- When feedback is submitted with "Report a problem", save the feedback and the complaint together, so either both are saved or neither is.
- **Reference number:** use a random part, for example `MC-26-K7Q2XP`, not a running number like `00123`. This stops people guessing other references, which matters because the reference is the only key to the public status page.
- `GET /api/public/complaints/{ref}` returns only the status, the date it was last updated and a public note. It returns no names, contact details or internal notes.
- A rate limit on this lookup endpoint.

**Frontend**
- A complaint section in the form: category and description.
- The thank-you screen shows the reference number with a copy button and a "screenshot this" hint.
- The `/track` page: enter a reference and see a status timeline.

**Done when**
- A submitted complaint returns a reference number.
- The track page shows its status.
- A wrong reference shows "not found" without revealing anything.

---

## Phase 6: Handling Complaints, Feature 6 (4 days)

**Goal:** staff assign and work on complaints, with a full history and live alerts.

**Backend**
- Assign a complaint and set its category, priority and due date. A default due date is set from the priority.
- **Status rules** in one place, a service that only allows these moves:
  - Open → In Progress
  - In Progress → Resolved
  - Resolved → Closed
  - Resolved → In Progress (reopen)
- **`ComplaintAction` history:** every change and note is saved with who made it and when. The history can only be added to, never edited.
- **Who can do what:**
  - Organizers handle complaints for their own camps.
  - Medical staff see only their department's complaints and add notes only when assigned.
  - Admins can handle all complaints.
- **Overdue job:** a nightly `@Scheduled` job flags complaints that are past their due date and not resolved. Lists also check this when they load.
- **Repeat detection:** a query counts complaints by camp and category within a time window, and anything with N or more is marked "repeat". N is a setting.
- **Notifications:**
  - `Notification` table
  - `GET /api/notifications/stream` (Server-Sent Events) sends assignment and overdue events
  - endpoints to list notifications and mark them read
- List endpoints with filters for status, priority, assignee, camp, overdue and repeat.

**Frontend**
- **Complaint table** (TanStack Table): sorting, filters, overdue and repeat badges.
- **Kanban board** (dnd-kit): one column per status. Dragging a card calls the status change, and an invalid move snaps back with a message. The board also works on touch screens.
- **Complaint details page:** the complaint, its linked feedback, and assign, priority and due-date controls. A history timeline and a box for adding notes.
- **Notification bell:** live count, a dropdown list, and pop-up messages for new assignments.

**Tests**
- Status rule tests, including every invalid move.
- Permission tests for each role.
- The overdue job, run against a fixed clock.

**Done when**
- An organizer assigns a complaint and the assignee gets a live notification.
- Dragging a card changes the status and adds a history entry.
- Overdue and repeat complaints are clearly flagged.

---

## Phase 7: Resolution and Follow-Up, Feature 7 (1.5 days)

**Goal:** record what was done, and ask the attendee whether it helped.

**Backend**
- Resolving a complaint requires an "action taken" note and an optional public note. Resolution time is recorded.
- `FollowUp` table: complaint, addressed (yes/no), comment, time.
- `POST /api/public/complaints/{ref}/follow-up`:
  - allowed only when the complaint is Resolved and the attendee asked for follow-up
  - only one follow-up per complaint
- If the attendee answers "No", the complaint is flagged for review and the assignee is notified.

**Frontend**
- A resolve dialog in the complaint details page.
- On the `/track` page, a resolved complaint shows the public note and asks "Was your issue addressed?" with Yes/No and a comment.
- The complaint details page shows the follow-up result.

**Done when**
- The full path works from start to finish: submit → assign → resolve → attendee confirms.
- A "No" answer reopens the review and notifies the assignee.

---

## Phase 8: Dashboards, Feature 8 (3.5 days)

**Goal:** three role-aware dashboards built on the data from Phases 4–7.

**Backend**
- A `DashboardService` that answers each question with an SQL aggregate query (JPQL or native SQL). Only the numbers are returned, never raw rows.
- Shared filters: date range, location, camp, service.
- Every average comes back with its count (`{avg, n}`), and with `lowSample: true` when n is below the setting, by default 10.
- Results depend on the role:
  - Medical staff see their own department only.
  - Analysts see grouped numbers with no comments or contact details.
- Endpoints:
  - `GET /api/dashboard/overview`: total responses, average satisfaction, active camps, resolution %, monthly trend, comparison between camps
  - `GET /api/dashboard/feedback`: how the ratings are spread, average by category, complaint categories, waiting-time and facility trends, comparison between camps
  - `GET /api/dashboard/actions`: complaint counts by status, priority and overdue; workload per person; average resolution time; repeat complaints; follow-up yes/no rates
- The live stream also sends a "data changed" event, so open dashboards refresh.

**Frontend**
- Three pages:
  - **Executive Overview**
  - **Feedback Intelligence**, with an empty "Themes" card waiting for feature 9
  - **Action and Resolution Center**
- Each page has:
  - one shared filter bar, with the filters kept in the page address so a view can be shared
  - Mantine Charts with summary number cards
  - an "n = 34" label next to every average and a "low sample" badge
  - live refresh
- **Drill-down:** clicking a bar or segment opens the matching feedback or complaint list with the filters already applied.
- Loading, empty and error states on every card.

**Demo data**
- A seed script in the `dev` profile only. It creates about 5 camps and about 300 feedback entries with complaints spread across different statuses, so the charts have something to show.

**Done when**
- All three dashboards show correct numbers. Check them by hand with SQL on the demo data.
- Filters and drill-down work.
- The page is usable on a tablet.
- Each role sees only the data it is allowed to see.

---

## Phase 9: Stabilize and Demo (1.5 days)

- Run the full workflow from start to finish on the hotspot with real phones.
- Check permissions again: call each endpoint directly with each role.
- Mobile review of the attendee pages on a small Android phone.
- Fix the bugs found. Update the README with the run and hotspot instructions.
- Record a demo script and screenshots, and add them to the README.
- Merge everything into `main`, tag it **`v1.0.0-features-1-8`**, and create a GitHub Release with the built JAR attached.

**Checkpoint:** features 1–8 complete. Next come features 9–15 (Level 1, Part 2).

---

## Run Setup: Local, Not Docker

The app runs **directly on your machine** with `java -jar`. Docker is optional and used only for the database.

| What | How it runs | Why |
|---|---|---|
| App (backend + frontend) | `java -jar medcamp-feedback.jar` | One process on port 8080. Phones on the hotspot can reach it without extra network settings. It is the easiest to debug. |
| Database during development | H2 file database (built in, no install) | Zero setup in the first weeks |
| Database for the demo or a real camp | PostgreSQL, either **installed on Windows** or **run with Docker Compose** (`docker compose up -d db`) | Docker only saves you installing PostgreSQL; neither choice changes the app |
| Frontend during development | `npm run dev` (Vite on port 5173, forwarding to 8080) | Instant reload while you work |

**Requirements:**
- JDK 21, Maven, Node.js LTS (needed only to build), and Git
- Optional: PostgreSQL 16 or Docker Desktop

**Why the app is not in Docker for Level 1:**
- On Windows, Docker Desktop runs containers inside a virtual machine (WSL2). That adds memory use and one more network layer between the phone and the app.
- The plain JAR is simpler to run at a camp.
- A `Dockerfile` for the app can be added in Level 2, when the project moves to hosting.

---

## GitHub Workflow

**Repository**
- A **private** GitHub repository, for example `medcamp-feedback-portal`, with the following structure:

```
medcamp-feedback-portal/
├── backend/                 # Spring Boot (Maven); builds the final JAR
├── frontend/                # React + Vite + TypeScript
├── docs/                    # Project description, Level plan, this plan, ER diagram, wireframes, screenshots
├── docker-compose.yml       # Optional: PostgreSQL only
├── .github/workflows/ci.yml
├── .gitignore
└── README.md
```

**Keep these out of Git** (list them in `.gitignore`):
- `target/`, `node_modules/` and `dist/`
- the H2 database files (`*.mv.db`)
- `.env`
- `application-local.properties`
- IDE folders (`.idea/`, `.vscode/`)

Passwords, the admin seed password and SMTP credentials go in environment variables or the git-ignored local settings file. Never commit them. Commit an `application-local.properties.example` file with placeholder values instead.

**Branches and commits**
- `main` always builds and runs. Protect it so changes only arrive through pull requests that pass CI.
- One branch per phase or feature, for example `phase-1/auth-roles` or `phase-6/complaint-kanban`.
- Open a pull request into `main` at the end of each phase and fill in the "done when" checklist in the description.
- Use short commit messages in the form `type: summary`, for example `feat: add camp QR poster PDF` or `fix: block reused submission token`. Types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`.
- **The commit author is the developer only.** Commits have no `Co-Authored-By` lines or other AI or tool attribution.

**Tracking**
- One **milestone** per phase (Phase 0 – Phase 9), with GitHub Issues for the tasks listed in each phase.
- A **GitHub Projects** board with the columns To do, In progress, Review and Done.
- Tags at important checkpoints:
  - `v0.1.0` after Phase 1
  - `v0.4.0` after Phase 4
  - `v0.7.0` after Phase 7
  - `v1.0.0-features-1-8` after Phase 9

**CI (GitHub Actions, `ci.yml`)**
- Runs on every push and pull request:
  1. set up JDK 21 and Node.js
  2. `mvn -B verify`, which also builds the frontend and runs the JUnit and Vitest tests
  3. upload the JAR as a build artifact
- A pull request cannot be merged until CI passes.

---

## Risks and How to Handle Them

| Risk | Mitigation |
|---|---|
| Learning React and Spring Boot at the same time slows Phases 0–1 | Keep the Phase 0 setup minimal. Use the Mantine examples directly. Build one complete feature (Phase 2) before adding polish. |
| Phones cannot reach the laptop on the hotspot | Test in Phase 3, not at the end. Check the firewall and network profile. Keep the Cloudflare Tunnel as a backup. |
| Duplicate or spam submissions | One-time submission tokens and rate limits (Phases 4 and 5). |
| Dashboard numbers are wrong | Seed the demo data and check each chart by hand against SQL (Phase 8). |
| The dashboards phase runs over | Build the Executive Overview first. The Action Center is next most important. Feedback Intelligence can be simplified. |
| Session/CSRF problems between React and Spring | Set up the shared API client and CSRF handling in Phase 0 and test them with the login in Phase 1. |

---

## Progress Log

| Date | Phase | Notes |
|---|---|---|
| 2026-10-29 | 0 and 1 done | Spring Boot 4.1 (3.x is past free support). Roles are one value per user, not a separate table. Login uses Spring Security's form login with JSON responses and the SPA CSRF setup. Extra safety added: a disabled user's open sessions end on their next request, and role changes apply without signing in again. 34 backend tests and 7 frontend tests pass. |
