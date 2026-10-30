# Medical Camp Feedback Portal

Collects feedback from medical camp attendees and turns it into tracked action:
**Feedback → Analysis → Action → Resolution → Improvement**.

- Backend: Java 21, Spring Boot 4 (REST API, Spring Security, JPA, Flyway)
- Frontend: React 19, TypeScript, Vite, Mantine
- Database: H2 file database for development, PostgreSQL for demos and real camps
- Output: **one JAR file** that serves both the API and the web app on port 8080

Planning documents are in [`docs/`](docs/).

## Status

| Phase | Scope | State |
|---|---|---|
| 0 | Foundation: build, database migrations, API conventions, app shell | Done |
| 1 | Login, roles, user and department management | Done |
| 2 | Camps: services, dates, feedback window, status, organizer-only editing | Done |
| 3 | QR codes, printable bilingual poster with optional Wi-Fi QR, public camp link | Done |
| 4 | Bilingual mobile feedback form, duplicate prevention, staff-assisted mode, Feedback tab | Done |
| 5 | Reporting a problem, complaint reference numbers, public /track page | Done |
| 6–9 | Handling complaints, follow-up, dashboards | Planned |

## Requirements

- JDK 21
- Node.js 24 LTS (only for frontend development; the Maven build downloads its own copy)
- Maven is **not** needed: use the included wrapper (`mvnw` / `mvnw.cmd`)
- Optional: PostgreSQL 16 or Docker Desktop

## Run in development

Two terminals:

```bash
# 1. Backend on http://localhost:8080 (skips the frontend build)
cd backend
./mvnw spring-boot:run -Dskip.frontend=true

# 2. Frontend on http://localhost:5173 (reloads instantly; /api calls go to 8080)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

**First sign-in:** when the database is empty, the app creates a Super Admin `admin@medcamp.local`.
In development, the password is the default set in
`backend/src/main/resources/application-dev.properties` (or the `APP_ADMIN_PASSWORD` environment variable).
You must choose a new password at first sign-in.

The development database is stored in `backend/data/`. Delete that folder to start again from empty.

## Build and run the single JAR

```bash
cd backend
./mvnw package             # frontend tests + build, backend tests, then the JAR
java -jar target/medcamp-feedback.jar
```

Open http://localhost:8080. API documentation (development profile only): http://localhost:8080/swagger-ui.html

## Run at a camp (laptop hotspot, no internet)

1. Turn on **Windows Mobile Hotspot**. The laptop's address on it is `192.168.137.1`.
2. Allow Java through Windows Firewall for **Private** networks (Windows asks the first time).
3. Start the app with the hotspot address, so QR codes point to it:

   ```bash
   APP_PUBLIC_BASE_URL=http://192.168.137.1:8080 java -jar target/medcamp-feedback.jar
   ```

   On PowerShell: `$env:APP_PUBLIC_BASE_URL="http://192.168.137.1:8080"; java -jar target/medcamp-feedback.jar`

4. Phones that join the hotspot can open `http://192.168.137.1:8080`.

## Use PostgreSQL (prod profile)

```bash
DB_PASSWORD=choose-one docker compose up -d db        # or install PostgreSQL 16 yourself

SPRING_PROFILES_ACTIVE=prod DB_PASSWORD=choose-one APP_ADMIN_PASSWORD=first-admin-pw \
  java -jar backend/target/medcamp-feedback.jar
```

Settings can also go in a git-ignored `application-local.properties`; see
[`backend/application-local.properties.example`](backend/application-local.properties.example).

## Tests

```bash
cd backend && ./mvnw verify          # everything (backend + frontend)
cd frontend && npm test              # frontend only
cd frontend && npm run lint          # oxlint
```

## Project layout

```
backend/    Spring Boot API; builds the final JAR (React app included under /static)
frontend/   React + Vite + Mantine single-page app
docs/       Project description, Level 1/2 plan, phase plan
.github/    CI: builds and tests every push and pull request
```

## API conventions

- `/api/public/**`: no sign-in (attendee pages, later phases)
- `/api/**`: staff only; the role is checked on the server for every call
- Errors are `application/problem+json` with a stable `code` (for example `EMAIL_TAKEN`) and, for invalid input, `errors` per field
- Writes need the CSRF token: the `XSRF-TOKEN` cookie value sent back in the `X-XSRF-TOKEN` header
