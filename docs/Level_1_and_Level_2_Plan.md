# Medical Camp Feedback Portal
## Two-Level Feature Plan (Java)

This plan splits the project in `Medical_Camp_Feedback_Portal_Project_Description.md` into two levels:

- **Level 1:** Every essential feature, plus every "smart" feature that runs on a normal laptop CPU. Nothing needs a GPU, a paid API, or a cloud server.
- **Level 2:** Features that need a GPU, paid external services, public hosting, or more time than the six-week plan allows. This level is only a high-level draft for now.

---

## 1. Tech Stack (Level 1)

### 1.1 Backend (Java)

| Layer | Choice | Why |
|---|---|---|
| Language | Java 21 (LTS) | Your preferred language |
| Framework | Spring Boot 4 (REST API) | Web server, security, and database access in one package |
| Security | Spring Security + BCrypt, session cookie + CSRF token | Login, roles, and server-side permission checks. The frontend is served from the same address, so a session cookie is simpler and safer than JWT. |
| Database | PostgreSQL (H2 file mode, in PostgreSQL compatibility mode, during development) | Relational data with easy aggregate queries |
| ORM | Spring Data JPA (Hibernate) | Less SQL boilerplate |
| API docs | springdoc-openapi (Swagger UI) | Lets you test the API in a browser and documents it for the report |
| Live updates | Server-Sent Events (`SseEmitter`) | Live dashboard counts and notification badge |
| QR codes | ZXing (`com.google.zxing`) | Generates QR images fully offline |
| Exports | Apache POI (Excel), OpenCSV (CSV), OpenPDF (PDF) | Report downloads and printable posters |
| Scheduled jobs | Spring `@Scheduled` | Overdue flags, data-retention cleanup |
| Email *(optional)* | Spring Mail (SMTP) | Assignment and overdue emails, only when the laptop has internet |
| Build | Maven + `frontend-maven-plugin` | One command builds both the backend and the frontend |

### 1.2 Frontend

| Layer | Choice | Why |
|---|---|---|
| Framework | React 19 + TypeScript, built with Vite | The standard choice for interactive single-page apps. It has the most learning material, and TypeScript catches mistakes early. |
| UI components | Mantine | A full, accessible, mobile-friendly component set (forms, modals, date pickers, notifications, dark mode). It gives a polished "standard" look without designing everything yourself. |
| Charts | Mantine Charts (Recharts) | Dashboard charts that match the components |
| Tables | TanStack Table | Sorting, filtering, and paging for the complaint and feedback lists |
| Drag and drop | dnd-kit | Kanban board for moving complaints between statuses |
| Server data | TanStack Query | Caching, loading states, and automatic refresh |
| Forms | React Hook Form + Zod | Fast forms with validation rules that are easy to read |
| Routing | React Router | Page navigation |
| Multilingual | react-i18next (`en.json`, `hi.json`) | English/Hindi switch that changes instantly, with no page reload |

### 1.3 How the Pieces Fit
- **Development:** run Vite on port 5173 and Spring Boot on port 8080. Vite forwards `/api` calls to Spring Boot.
- **Running the project:** Maven builds React into Spring Boot's `static/` folder, which produces **one JAR file on one port (8080)**.
- The QR code, hotspot, and tunnel setup in Section 5 therefore works the same way. Node.js is needed only to build the frontend, not to run the project.

---

## 2. Level 1: Core and Low-Compute Features

### 2.1 Authentication and Role-Based Access
- Login and logout for staff roles: **Super Admin, Organizer, Medical Staff, Analyst**.
- Attendees never need an account. They reach the form only through a camp QR code or link.
- Passwords are stored as BCrypt hashes.
- Permissions are enforced on the server through URL rules and `@PreAuthorize`, not only by hiding buttons.
- The admin can create, disable, and reset users and change their roles.
- Medical staff are linked to a department and see only that department's feedback.
- Analysts see only aggregated, anonymized data.

### 2.2 Camp Management
- Organizers can create, edit, and archive camps with a name, dates, location, organizer, and status (**Upcoming / Active / Closed**).
- Organizers can add the departments and services available at each camp.
- A **feedback window** setting lets organizers open the form before the camp and close it a set number of days after the camp ends.
- Each camp gets a random **public token**, such as `/f/7Kq2xP`, instead of a sequential ID, so people cannot guess the links of other camps.

### 2.3 QR Code and Link Generation
- ZXing generates a QR PNG for each camp's feedback URL.
- The **printable poster page** shows the camp name, the QR code, a short typed link, and instructions in English and Hindi.
- The base URL is configurable in `application.properties` (`app.public-base-url`), so the QR codes keep working when the project moves from LAN to hosting (see Section 5).
- *(Optional)* A **Wi-Fi QR code** on the same poster helps attendees join the camp hotspot (see Section 5.1).

### 2.4 Feedback Form (Attendee)
- The form is mobile-first, with large touch targets and simple wording.
- It has an **English/Hindi toggle**.
- Attendees rate the following on a 1–5 scale (stars or emoji faces):
  - Registration and guidance
  - Waiting time
  - Staff behavior and communication
  - Cleanliness and facilities
  - Information about services
  - Overall experience
- Attendees can select the service or department they visited.
- The form includes a free-text comment box.
- **Anonymous by default.** An optional contact field (phone or email) appears only if the attendee ticks "I want follow-up" and gives consent.
- No clinical fields are included: no diagnosis, prescriptions, or medical history.
- Validation runs on both the client and the server.
- **Duplicate prevention:** each form load gets a one-time submission token, and a light per-device cookie and rate limit stop repeated submissions.
- **Staff-assisted mode:** a logged-in volunteer can fill in the form on behalf of an attendee. The response is flagged as "assisted".

### 2.5 Complaint Raising and Tracking by Reference Number
- The form has a "Report a problem" option. Attendees choose a category and describe the issue.
- The system creates a complaint with a **reference number** (for example, `MC-2026-00123`) and shows it on the thank-you screen.
- A public **"Check my complaint"** page lets attendees enter the reference number to see the status. It shows no personal details.

### 2.6 Complaint Management (Action and Resolution Center)
- Staff can set the category, priority (**Low / Medium / High**), assignee, and due date for each complaint.
- Complaints follow this status workflow: **Open → In Progress → Resolved → Closed**. Only valid transitions are allowed.
- Every complaint has an **action history** with timestamped entries for who did what and the notes they added.
- **Overdue flag:** a nightly `@Scheduled` job, or a query-time check, marks complaints that are past their due date and still not resolved.
- **Repeat complaint detection:** the system highlights cases where the same camp and category reach N or more complaints. This uses a simple `GROUP BY` count.
- **Two views:** a sortable and filterable table, and a **Kanban board** where staff drag a complaint card to change its status.
- **Live in-app notifications:** a bell badge and toast messages update instantly through Server-Sent Events when an item is assigned or becomes overdue. Email is an optional extra (Section 1.1). SMS is Level 2.

### 2.7 Resolution and Follow-Up
- The assignee records the action taken and the resolution notes.
- Attendees who opted in can open the "Check my complaint" page and answer **"Was your issue addressed? Yes/No + comment"**.
- Dashboards show "closed" and "confirmed resolved by attendee" as separate counts.

### 2.8 Dashboards (Three Pages)
All dashboard numbers come from SQL aggregate queries and are drawn with Mantine Charts. The headline numbers update live as new feedback arrives. Clicking a chart bar opens the matching list of feedback or complaints.

**Executive Overview**
- Total responses
- Average satisfaction
- Active camps
- Resolution percentage
- Monthly trend
- Camp-wise comparison

**Feedback Intelligence**
- Rating distribution
- Satisfaction per category
- Complaint categories
- Waiting-time and facility trends
- Keyword themes (see Section 2.9)

**Action and Resolution Center**
- Open, in-progress, resolved, and overdue counts
- Counts by priority
- Workload per assignee
- Average resolution time
- Repeat complaints
- Follow-up results

These rules apply to every dashboard page:
- Users can filter by date range, location, camp, and service.
- The response count (**n**) appears next to every average.
- Results with fewer than a configurable minimum (for example, 10 responses) are labeled **"low sample"**.

### 2.9 Lightweight Text Insights (Rule-Based, No ML)
- **Keyword theme tagging:** an admin-editable keyword list maps words to themes. For example, "queue", "wait", "late", "line" (and their Hindi equivalents) all map to **Waiting Time**.
- **Lexicon-based sentiment:** the system counts positive and negative words from small English and Hindi word lists and labels each comment Positive, Negative, or Neutral.
- Both results are shown as **"auto-suggested"**, and staff can correct them.

### 2.10 Explainable Recommendations (Rule Engine)
- An admin-configured rule looks like this: *IF Waiting Time average < 3.0 AND at least 5 comments are tagged Waiting Time THEN suggest "Review registration staffing / patient flow"*.
- Each suggestion shows its **evidence**: the average, the number of comments, and links to sample comments.
- Recommendations are worded as "suggested for review", never as conclusions.

### 2.11 Benchmarking
- Users can compare a camp with the **same organizer's previous camps** and with **configured targets**, such as a waiting-time target of 3.5 or higher.
- The comparison is hidden or flagged when either side is below the minimum sample size.

### 2.12 Community Accessibility Score
- The score is a weighted average of four components: registration convenience, clarity of information, waiting time, and facility accessibility.
- The admin sets the weights.
- The score always appears with its component breakdown and its formula.

### 2.13 Reports and Exports
- Each camp has a summary report covering responses, satisfaction, top concerns, unresolved issues, and actions taken.
- Reports can be exported to **PDF, Excel, and CSV**.
- Exports leave out contact details and identifiable comments unless an admin exports them on purpose. Each such export is logged.

### 2.14 Admin Configuration
- Admins can manage the following without changing code:
  - Feedback categories and keyword lists
  - Targets, score weights, and recommendation rules
  - The minimum sample size and the default overdue period
  - The data retention period
- **Audit log** of logins, role changes, status changes, and exports.

### 2.15 Privacy Safeguards
- A consent notice on the form explains how the feedback is used and how long it is kept.
- Contact details are visible only to Organizers and Admins.
- A retention job deletes contact details from closed complaints after the configured period.
- No identifying data appears on dashboards or in default exports.

### 2.16 Level 1 Data Entities
`User` (with a `Role` value), `Department`, `Camp`, `CampService`, `Feedback`, `FeedbackRating`, `Complaint`, `ComplaintAction` (history), `FollowUp`, `Notification`, `ThemeKeyword`, `RecommendationRule`, `Setting`, `AuditLog`

### 2.17 Mapping to the Six-Week Plan

| Week | Level 1 work |
|---|---|
| 1 | Entities and ER diagram, roles, form questions, wireframes, Spring Boot + React (Vite) project setup, API design |
| 2 | Login and roles (session + CSRF), app layout and navigation in Mantine, user management, camp list and detail pages |
| 3 | Camp CRUD, QR and poster, feedback form (English/Hindi), validation, duplicate prevention, reference numbers |
| 4 | Complaint workflow, action history, overdue job, three dashboards, exports |
| 5 | Keyword themes, lexicon sentiment, rule engine, benchmarking, accessibility score, follow-up |
| 6 | Permission tests, mobile and accessibility review, privacy checks, sample data, demo, documentation |

---

## 3. Level 2: High-Level Draft (Later)

| Area | Idea | Why it is Level 2 |
|---|---|---|
| AI/NLP text analysis | ML-based multilingual sentiment, theme clustering, and similar-complaint auto-linking | Running a small pretrained model on the CPU (ONNX Runtime/DJL) is possible, but training or fine-tuning one needs a GPU, and it does not fit the six weeks. The lexicon version in Level 1 sits behind the same interface, so it can be swapped out later. |
| Voice feedback | Speech-to-text in Hindi and regional languages | The browser's speech feature needs HTTPS and internet. Offline engines (Vosk) work on the CPU but add a lot of scope. |
| Offline-first PWA | Attendees or volunteers fill in forms with no network; the app syncs later and removes duplicates | Service workers only run over HTTPS, which the plain-HTTP hotspot setup does not have. Sync conflict handling also adds work. |
| External notifications | SMS, WhatsApp, or email for assignments, overdue items, and attendee follow-up | Third-party gateways, cost, consent handling |
| Public hosting | Cloud deployment with a domain, HTTPS, backups, and monitoring | Needs a server and ongoing upkeep |
| Multi-organization | Several NGOs and hospitals on one platform with isolated data | Tenant isolation and more complex security |
| Advanced analytics | Location maps and heatmaps, long-term trends, forecasting of attendance and complaints | Heavier data processing |
| Integrations | REST APIs for approved organizational reporting systems | External agreements and security reviews |
| More languages | Additional regional languages beyond Hindi and English | Translation effort and font/IME support |

Level 1 keeps the code ready for Level 2:
- The base URL is configurable.
- The text-insight logic sits behind one Java interface (`TextAnalyzer`). The lexicon version can later be swapped for an ML version.
- Notifications go through one `NotificationService`. In-app delivery can later be extended to SMS or email.

---

## 4. Does the QR Feature Need Hosting?

**Short answer: no.** The QR code does not need hosting. **The link inside the QR code does.**

- A QR code is just a URL turned into an image. ZXing creates it on your laptop with no internet.
- When an attendee scans it, their phone opens that URL. The scan works only if the **phone can reach the computer running the app**.
- So the real question is: *how does the attendee's phone reach the laptop?* Section 5 lists three ways to do that while keeping the project on your local machine.

---

## 5. Ways to Run the Project Without Hosting

### 5.1 Option A: Local Network or Laptop Hotspot (fully offline, recommended for Level 1)
1. Run the Spring Boot app on your laptop. By default it listens on all network interfaces at port 8080.
2. Turn on the **Windows Mobile Hotspot**, or connect the laptop and phones to the same Wi-Fi router.
   - On Windows Mobile Hotspot, the laptop's address is almost always **`192.168.137.1`**, so this stays the same every time.
3. Set `app.public-base-url=http://192.168.137.1:8080`. The QR code then points to `http://192.168.137.1:8080/f/7Kq2xP`.
4. Attendees join the hotspot and scan the QR code. The form opens and the feedback is saved straight into the laptop's database.
5. Print **two QR codes** on the poster:
   - First, a Wi-Fi QR code (`WIFI:T:WPA;S:CampWiFi;P:password;;`). Phone cameras join the network directly from it.
   - Second, the feedback form QR code.

**Pros**
- No internet, hosting, or cost.
- All data stays on your machine.
- Works in rural camps with no signal.
- Works well for demos.

**Cons and things to handle**
- The laptop must stay on at the camp.
- Attendees must join the hotspot. Windows hotspots allow about 8 devices at a time, which is fine because people take turns. A cheap Wi-Fi router allows more.
- Allow Java/port 8080 through **Windows Firewall** for Private networks, and mark the hotspot network as Private.
- Some phones warn that the Wi-Fi "has no internet". Attendees should tap "Stay connected".
- Plain HTTP works for forms. Features like voice input need HTTPS, which is a Level 2 concern.
- Attendees cannot check their complaint status from home unless the app is reachable from outside. In that case, staff can check the status for them at the next camp.

### 5.2 Option B: Local Machine + Tunnel (the app is local, but has a public HTTPS link)
- Run the app locally and start a tunnel. For example, with **Cloudflare Tunnel**: `cloudflared tunnel --url http://localhost:8080`. **ngrok** works the same way.
- You get a public HTTPS URL. Attendees can scan with **their own mobile data**, with no hotspot needed.
- **Pros:** no server to rent, HTTPS is included, and attendees can check complaint status from home.
- **Cons:**
  - The laptop must be on **and connected to the internet**.
  - Free quick-tunnel URLs **change on every restart**, which means the QR codes must be regenerated. A fixed URL needs a named tunnel with your own domain or a reserved free domain from the tunnel provider.
  - Because the app is now publicly reachable, security matters more. That is one more reason to use random camp tokens and server-side permission checks.

### 5.3 Option C: No QR at All (kiosk or assisted mode)
- Keep a tablet or laptop at the exit with the form already open. Attendees or volunteers fill it in there.
- This needs no network at all and is a good fallback for Options A and B.

### 5.4 Comparison

| | A: LAN/Hotspot | B: Tunnel | Hosting (Level 2) |
|---|---|---|---|
| Needs internet | No | Yes (laptop) | Yes |
| Cost | Free | Free (URL may change) | Free tier or paid |
| Attendee uses own mobile data | No (joins hotspot) | Yes | Yes |
| Status check from home | No | Yes, while the laptop is on | Yes |
| Laptop must stay on | Yes | Yes | No |
| Effort to set up | Low | Low | Medium |

**Recommendation:**
1. Build Level 1 entirely on your local machine and demo it with **Option A** (hotspot).
2. Keep **Option B** ready if you need attendees to use their own mobile data.
3. Because the QR base URL is a single setting, moving to real hosting later means changing that setting and regenerating the posters. No code changes are needed.
