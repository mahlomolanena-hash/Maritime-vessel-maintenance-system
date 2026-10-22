# Tidewatch: Maritime Vessel Maintenance Management System

A planned-maintenance system for a fleet: register vessels and their machinery, schedule preventive
maintenance by calendar days, running hours or operating hours, run the work order workflow, and keep a
permanent maintenance history.

Spring Boot 3 REST API (Java 17, JPA, JWT security) with a React 19 + Vite interface.

| | |
|---|---|
| Backend | Spring Boot 3.3, Spring Security (JWT + refresh-token rotation), Spring Data JPA, springdoc OpenAPI |
| Database | H2 in memory for development (seeded), MySQL 8 for production |
| Frontend | React 19, React Router 6, Vite |
| Tests | JUnit 5, Mockito, MockMvc integration tests |

## Run it

**Backend** (needs JDK 17+ and Maven; no database setup in dev):

```bash
cd backend
mvn spring-boot:run          # http://localhost:8080, Swagger UI at /swagger-ui.html
```

**Frontend** (needs Node 20.19+ or 22+):

```bash
cd frontend
cp .env.example .env
npm install
npm run dev                  # http://localhost:5173
```

The dev profile seeds two vessels, a main engine with a child pump, a generator, a purifier, schedules in every
due state (overdue, due soon, ok), spare parts and a few work orders.

| Role | Email | Password |
|---|---|---|
| Admin | admin@example.com | Admin123! |
| Superintendent | superintendent@example.com | Super123! |
| Engineer | engineer@example.com | Engineer123! |

To walk the whole workflow from the command line, run `bash docs/curl-examples.sh` with the backend up.

**Production profile (MySQL):** `docker compose up -d`, then start the backend with
`SPRING_PROFILES_ACTIVE=prod` and the `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET` environment
variables (see `application-prod.yml`). Always set your own `JWT_SECRET`.

## What it does

| Requirement | Where |
|---|---|
| System requirements analysis | Roles, workflow and rules below; `docs/DATABASE.md` design decisions |
| Database design | 13 tables with keys, constraints and indexes: [`docs/DATABASE.md`](docs/DATABASE.md) |
| Vessel and equipment management | Vessels, nested equipment (engine, then pump), tag numbers, criticality, hour meters |
| Preventive maintenance | Tasks plus schedules by days, running hours or operating hours; due status computed live |
| Maintenance work orders | Create, assign, start, hold, resume, complete, cancel with priority, assignee, dates, remarks |
| Maintenance history | A record is written when a work order completes; searchable by vessel, equipment, type, dates |
| User interface | Dashboard, due-maintenance watch list, vessel and equipment pages, work order workflow, history, spare parts |
| Testing and documentation | Unit and integration tests; [`docs/API.md`](docs/API.md), [`docs/DATABASE.md`](docs/DATABASE.md), Swagger UI |

## How maintenance flows through the system

1. **Register** a vessel and its equipment, then log each machine's hour-meter readings (meters only move forward).
2. **Define a task** for the equipment ("Change lube oil filter") and a **schedule** that repeats it every
   N days, running hours or operating hours.
3. The schedule's **due status** (`OK`, `DUE_SOON`, `OVERDUE`) is computed from today's date and the live hour
   meter. The dashboard and "Planned maintenance" list show the most urgent first, with a gauge of how much of
   the interval is used.
4. A superintendent turns a due schedule into a **preventive work order** in one click (or anyone raises a
   corrective one) and assigns it to an engineer.
5. The engineer **starts** the job, records the **spare parts** used (stock is drawn down), and **completes**
   it with remarks and time spent.
6. Completing writes the **maintenance record** (with hour meters and parts cost frozen at that moment),
   restarts the schedule's clock, and adds the closing entry to the work order's audit trail.

```
OPEN -> ASSIGNED -> IN_PROGRESS <-> ON_HOLD
                        |
                    COMPLETED            (any active state -> CANCELLED)
```

## Roles

| Role | Can do |
|---|---|
| Admin | Everything, plus create users and change roles |
| Superintendent | Manage vessels, equipment, tasks, schedules, spare parts; create, assign, edit and cancel work orders |
| Engineer | View everything; log hour readings; raise work orders; start, hold, complete the work assigned to them; record parts |

There is no public registration. An admin creates accounts.

## Tests

```bash
cd backend
mvn test
```

- `DueCalculatorTest`, `WorkOrderStatusTest`: the due-date logic and workflow rules, no Spring needed.
- `WorkOrderServiceTest`: permissions, workflow transitions, record creation, stock handling (Mockito).
- `ApiIntegrationTest`: boots the real application on the seeded H2 database and exercises login, role
  checks, validation errors, the due list and a full work order lifecycle through HTTP.

CI (`.github/workflows/ci.yml`) runs `mvn verify` for the backend and lint plus build for the frontend.

## Project layout

```
backend/src/main/java/com/example/maintenance/
  config/       security, CORS, OpenAPI, dev data seeder
  controller/   REST controllers (thin: role rules via @PreAuthorize)
  domain/       JPA entities, enums, DueCalculator (pure logic)
  dto/          request and response records
  exception/    domain exceptions and the global error handler
  repository/   Spring Data repositories
  security/     JWT filter and service
  service/      business rules and workflow
frontend/src/   api client, auth context, hooks, shared components, pages
docs/           database design, API reference, curl walkthrough
```

## Design decisions worth knowing

- **Status is derived, not stored.** "Overdue" can never be out of date because it is calculated from the date and
  the hour meter each time it is read.
- **One place for workflow rules.** `WorkOrderStatus.canTransitionTo` defines what is allowed; every change goes
  through one method that also writes the audit trail.
- **Concurrency is handled.** Optimistic locking on work orders and spare parts turns a lost update into a clear
  HTTP 409 instead of silent overwriting.
- **History does not rot.** Records snapshot hour meters and part prices at the time of service.
- **Roles are enforced on the server.** The UI hides what you cannot do, but the API refuses it regardless.

## Known limitations

- Hibernate generates the schema (`ddl-auto`); there are no Flyway migrations yet. Switch the prod profile to
  `validate` and add migrations before holding real data.
- The refresh-token lookup compares a presented token against stored hashes one by one. Fine for a small
  crew; index by a token identifier if the user base grows.
- The dashboard evaluates all active schedules in memory, which is fine for hundreds, not tens of thousands.
- No file attachments (photos, manuals) on work orders and no email or push notification for overdue work.
- Schedules count from the last completion; they do not yet support "fixed calendar" intervals (always on the 1st).
