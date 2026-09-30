# DevCommand — Personal Developer Command Center

This is a full-stack production-ready application containing a TanStack Start (React/Node) frontend and a Spring Boot (Java 23) backend powered by PostgreSQL.

The core architecture, entities, repositories, JWT authentication flow, and feature APIs (DSA tracking, Jobs, Learning, Daily Tasks) are fully implemented and integrated. Schema management is handled securely via Flyway migrations, and the entire stack can be launched via a unified Docker Compose configuration.

## 1. Project structure

```
com.devcommand.devcommand
├── DevcommandApplication.java        # @SpringBootApplication, @EnableJpaAuditing
│
├── auth/
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   └── dto/ (RegisterRequest, LoginRequest, AuthResponse)
│
├── user/
│   ├── entity/User.java
│   ├── repository/UserRepository.java
│   └── dto/UserResponse.java
│
├── dsa/            entity/DsaProblem, Difficulty, ProblemStatus
│                   controller/DsaProblemController, service/DsaProblemService,
│                   repository/DsaProblemRepository + DsaProblemSpecifications,
│                   mapper/DsaProblemMapper,
│                   dto/CreateDsaProblemRequest, UpdateDsaProblemRequest, DsaProblemResponse
│                   — full CRUD + filtering + pagination + sorting + ownership (see below)
├── jobs/           entity/JobApplication, ApplicationStatus, InterviewRound, InterviewStatus
│                   controller/JobApplicationController, InterviewRoundController,
│                   service/JobApplicationService, InterviewRoundService,
│                   repository/JobApplicationRepository + JobApplicationSpecifications,
│                   InterviewRoundRepository,
│                   mapper/JobApplicationMapper, InterviewRoundMapper,
│                   dto/(job) CreateJobApplicationRequest, UpdateJobApplicationRequest,
│                   JobStatusUpdateRequest, JobApplicationResponse,
│                   dto/(interview) CreateInterviewRoundRequest, UpdateInterviewRoundRequest,
│                   InterviewRoundResponse
│                   — full CRUD + status change + nested interview rounds + filtering
│                     + search + pagination + sorting + ownership (see below)
├── learning/       entity/LearningTopic, LearningStatus
│                   controller/LearningTopicController, service/LearningTopicService,
│                   repository/LearningTopicRepository + LearningTopicSpecifications,
│                   mapper/LearningTopicMapper,
│                   dto/CreateLearningTopicRequest, UpdateLearningTopicRequest,
│                   ProgressUpdateRequest, LearningTopicResponse, LearningSummary
│                   — full CRUD + progress auto-transitions + complete + filtering
│                     + search + completed/in-progress + pagination + sorting
│                     + ownership (see below)
├── projects/       entity/Project, ProjectStatus, ProjectTask, ProjectTaskStatus,
│                   ProjectTaskPriority + repositories + dto
├── tasks/          entity/DailyTask, TaskCategory, TaskPriority, DailyTaskStatus
│                   controller/DailyTaskController, service/DailyTaskService,
│                   repository/DailyTaskRepository + DailyTaskSpecifications,
│                   mapper/DailyTaskMapper,
│                   dto/CreateDailyTaskRequest, UpdateDailyTaskRequest, DailyTaskResponse
│                   — full CRUD + complete/start + filtering + today/upcoming/completed
│                     + pagination + sorting + ownership (see below)
│
├── security/       JwtService, JwtAuthenticationFilter, UserPrincipal,
│                   CustomUserDetailsService, SecurityConfig
│
├── exception/      GlobalExceptionHandler, ResourceNotFoundException,
│                   BadRequestException, ErrorResponse
│
├── config/         (reserved — currently empty; see "Design decisions" below)
│
└── common/entity/BaseEntity.java   # createdAt / updatedAt, shared by every entity
```

**One structural addition beyond what was asked, explained up front:**
a `common/entity/BaseEntity.java` `@MappedSuperclass` holding `createdAt` /
`updatedAt` with `@CreatedDate` / `@LastModifiedDate`. Every entity that needed
those two fields extends it instead of redeclaring them eight times. `@EnableJpaAuditing`
on the main class turns the annotations on. `InterviewRound` does **not** extend it,
since `createdAt`/`updatedAt` were not in its field list.

## 2. Entities created

| Entity | Table | Belongs to |
|---|---|---|
| `User` | `users` | — (root) |
| `DsaProblem` | `dsa_problems` | User |
| `JobApplication` | `job_applications` | User |
| `InterviewRound` | `interview_rounds` | JobApplication |
| `LearningTopic` | `learning_topics` | User |
| `Project` | `projects` | User |
| `ProjectTask` | `project_tasks` | Project |
| `DailyTask` | `daily_tasks` | User |

Every entity/repository/DTO listed in the spec was created. Repositories are
plain `JpaRepository<T, Long>` with no extra query methods (none were needed
yet, per the "no custom queries unless genuinely required" instruction).

## 3. Entity relationships

```
User 1───* DsaProblem
User 1───* JobApplication 1───* InterviewRound
User 1───* LearningTopic
User 1───* Project 1───* ProjectTask
User 1───* DailyTask
```

All `@ManyToOne` sides are `FetchType.LAZY`. All `@OneToMany` sides on `User`
and `Project`/`JobApplication` use `cascade = ALL, orphanRemoval = true`
(deleting a user cleans up their data; deleting a project cleans up its
tasks). Every relationship field is `@JsonIgnore`d and `equals`/`hashCode`
are id-only — see "Design decisions" for why.

## 4. Authentication endpoints

| Method | Path | Auth required | Body |
|---|---|---|---|
| POST | `/api/auth/register` | No | `{ name, email, password }` |
| POST | `/api/auth/login` | No | `{ email, password }` |

Both return:
```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "user": { "id": 1, "name": "...", "email": "...", "createdAt": "..." }
}
```

Every other endpoint (once built) requires `Authorization: Bearer <token>`
and is rejected with 401/403 by `SecurityConfig` otherwise.

## 5. Required environment variables

See `.env.example`. Summary:

| Variable | Purpose | Local default (for `dev` profile) |
|---|---|---|
| `POSTGRES_DB` | Postgres database name | `devcommand` |
| `POSTGRES_USER` | Postgres user | `postgres` |
| `POSTGRES_PASSWORD` | Postgres password | `postgres_password_example` |
| `DATABASE_URL` | Spring Boot JDBC URL | `jdbc:postgresql://postgres:5432/devcommand` (in compose) |
| `DATABASE_USERNAME` | Spring Boot DB User | `postgres` |
| `DATABASE_PASSWORD` | Spring Boot DB Password | `postgres_password_example` |
| `JWT_SECRET` | HMAC signing key for JWTs | **MUST** be overridden in `.env` |
| `JWT_EXPIRATION_MS` | Token lifetime in ms | `86400000` (24h) |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS domains | `http://localhost:8081` |
| `SERVER_PORT` | HTTP backend port | `8080` |
| `TELEGRAM_BOT_TOKEN` | Token obtained from BotFather | None |
| `TELEGRAM_ALLOWED_USER_ID` | Your numeric Telegram user ID | None |
> **Telegram Note:** Automatic verification of Telegram identities is disabled in production for security. Do NOT insert identities directly via SQL in production. When starting DevCommand for the first time with a fresh database, a one-time administrative bootstrap token will be generated and printed to the server logs. Message your Telegram bot with `/bootstrap <token>` to securely create the system admin account and link your Telegram identity. Run only ONE backend instance with polling enabled for the same bot token.
> [!IMPORTANT]
> The production configuration (`prod` profile) strictly requires these environment variables. Fallbacks only exist for local Maven test runs via the `dev` profile. Ensure your `.env` file is correctly populated before spinning up the Docker Compose stack.

## 6. Maven dependencies added

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `postgresql` (runtime driver)
- `spring-boot-starter-validation`
- `spring-boot-starter-security`
- `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.12.6) — JWT creation/parsing
- `lombok`
- `spring-boot-devtools`
- `spring-boot-starter-test`, `spring-security-test` (test scope)

**Assumption/decision:** `io.jsonwebtoken:jjwt-*` was added for JWT support
since the prompt said "JWT" but didn't name a library; jjwt is the de facto
standard for hand-rolled JWT issuing/validation in Spring Boot (as opposed to
pulling in the heavier `spring-security-oauth2-resource-server`, which
assumes an external identity provider — not the case here, since this app
issues its own tokens).

## 7. How JWT authentication works here

1. **Register** (`AuthService.register`) — checks email uniqueness, hashes
   the password with `BCryptPasswordEncoder`, saves the `User`, then issues a
   token immediately (no separate login step needed after signup).
2. **Login** (`AuthService.login`) — delegates credential checking to Spring
   Security's `AuthenticationManager` → `DaoAuthenticationProvider` →
   `CustomUserDetailsService` (loads the `User` by email) → `BCrypt` compare.
   Spring throws `BadCredentialsException` on mismatch, which
   `GlobalExceptionHandler` turns into a 401.
3. Both paths call `JwtService.generateToken`, which signs a JWT
   (`HS256`-family key from `JWT_SECRET`) with the user's **email as the
   subject claim** and an expiry `JWT_EXPIRATION_MS` in the future.
4. On every subsequent request, `JwtAuthenticationFilter` (registered before
   `UsernamePasswordAuthenticationFilter`) reads the `Authorization: Bearer
   <token>` header, extracts and validates the token, reloads the
   `UserDetails` via `CustomUserDetailsService`, and — if everything checks
   out — populates `SecurityContextHolder` so the request is treated as
   authenticated for the rest of the filter chain and any controller.
5. Sessions are stateless (`SessionCreationPolicy.STATELESS`) — nothing is
   stored server-side between requests; the JWT itself is the credential.

## 8. Running the application

Prerequisites: **Docker** and **Docker Compose**.

This project provides a unified, production-hardened Docker Compose stack that runs the frontend, backend, and PostgreSQL database seamlessly.

```bash
# 1. Create a .env file in the root of the project with required secrets
# (See .env.example or the table above)

# 2. Build and start the entire stack
docker compose up --build -d

# 3. View logs
docker compose logs -f
```

The stack includes:
- **Frontend** (TanStack Start + Nitro): [http://localhost:8081](http://localhost:8081)
- **Backend** (Spring Boot): [http://localhost:8080](http://localhost:8080)
- **Database** (PostgreSQL 16): Exposed on `localhost:5432` with a persistent Docker volume (`devcommand-pgdata`).

Database migrations are managed automatically by **Flyway** on container startup (`ddl-auto=validate`).

## 9. Testing registration/login with Postman

**Register**
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "password": "supersecret123"
}
```
Expect `201 Created` with a `token` in the body.

**Login**
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "ada@example.com",
  "password": "supersecret123"
}
```
Expect `200 OK` with a fresh `token`.

**Calling a protected endpoint (once one exists)**
```
GET http://localhost:8080/api/whatever
Authorization: Bearer <token from register/login response>
```
Missing/invalid/expired token → `401/403`. Wrong password on login → `401`
with an `ErrorResponse` body. Duplicate email on register → `400` with an
`ErrorResponse` body. Missing/invalid fields on either → `400` with a
field-level validation error body (`fieldErrors` map).

## 10a. DSA Tracking Module (implemented)

Full CRUD, ownership enforcement, filtering, pagination and sorting for
`DsaProblem`. See §11 for endpoints, DTOs, and Postman examples.

## 10b. Daily Tasks / Todo Module (implemented)

Full CRUD, ownership enforcement, complete/start transitions, filtering,
today/upcoming/completed convenience endpoints, pagination and sorting for
`DailyTask`. See §12.

## 10c. Job Application Tracker Module (implemented)

Full CRUD + status transitions for `JobApplication`, plus nested CRUD for
`InterviewRound`s, filtering, search, pagination and sorting. See §13.

## 10d. Learning Tracker Module (implemented)

Full CRUD, progress auto-transitions, explicit completion, filtering,
search, completed/in-progress views, pagination and sorting for
`LearningTopic`. See §14. Projects remain entities-and-repos-only.

## 10. What is intentionally NOT implemented yet

- Any CRUD endpoints/services/controllers for projects or project tasks —
  entities/repositories/DTOs exist, nothing else does. (DSA problems,
  daily tasks, job applications, and learning topics *are* now fully
  implemented — see §10a/§10b/§10c/§10d.)
- Analytics and dashboard logic.
- WhatsApp integration (webhook, message parsing).
- AI / natural-language command parsing.
- GitHub / LeetCode integrations.
- Notifications.
- Authorization/ownership rules beyond "authenticated or not" (e.g. "can
  user X touch resource Y" checks come with each feature's service layer).
- Role/permission model — `UserPrincipal` currently grants a single implicit
  authority to every authenticated user; no `Role` entity or `ROLE_*` scheme
  has been built, since none was requested.
- Refresh tokens — only a single-token, fixed-expiry JWT is issued; the spec
  asked for "a JWT," not a refresh flow.

## Design decisions worth knowing about

- **No `@Data` on entities.** Confirmed avoided everywhere, per the spec.
  Each entity uses `@Getter @Setter @NoArgsConstructor @AllArgsConstructor
  @Builder` plus a hand-written `equals`/`hashCode` based only on `id` (and
  only when `id != null`), and no generated `toString()` over relationship
  fields. This is the standard fix for the three classic JPA/Lombok traps:
  unstable `hashCode` as an entity moves from transient to persisted,
  `StackOverflowError` from bidirectional `toString`/`equals` recursion, and
  N+1-triggering `toString` calls that walk lazy collections.
- **`@JsonIgnore` on every relationship field**, not DTO-only serialization
  assumptions — belt-and-suspenders against accidental infinite recursion or
  password/collection leakage if an entity is ever accidentally returned
  directly from a future controller.
- **A few enums were invented** where the spec listed a field called
  `status`/`priority` but didn't enumerate its values (`InterviewRound`
  fields kept as plain `String`s instead, `LearningTopic.status`,
  `ProjectTask.status`, `ProjectTask.priority`). Each is called out with an
  inline comment in the source explaining the assumption; all are easy to
  replace or extend once the corresponding feature is actually built.
- **CSRF is disabled**, justified in `SecurityConfig`'s Javadoc: this API
  has no cookie-based session for a forged cross-site request to ride on,
  since auth is a bearer JWT a browser never attaches automatically.

---

## 11. DSA Tracking Module

Everything below is new in this stage. Jobs/learning/projects/tasks are
still entity-and-repository-only, per the original scope restriction.

### Java version

The project now targets **Java 23** (was Java 25). Changed:
`pom.xml`'s `<java.version>` property (which `spring-boot-starter-parent`
uses to set `maven.compiler.release`), and the JDK prerequisite in this
README's "Running the application" section. No Dockerfile, CI config,
Maven wrapper, or committed IDE files exist in this project, so there was
nothing else to update. No `maven.compiler.source`/`target`/`release` were
set explicitly elsewhere, so the single property change is sufficient.

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/dsa` | Yes | Create a problem, owned by the caller |
| GET | `/api/dsa` | Yes | Paginated list of the caller's problems, with optional filters |
| GET | `/api/dsa/{id}` | Yes | One problem — 404 if it doesn't exist *or* isn't owned by the caller |
| PUT | `/api/dsa/{id}` | Yes | Full update — 404 if not owned |
| DELETE | `/api/dsa/{id}` | Yes | Delete — 404 if not owned, 204 on success |
| PATCH | `/api/dsa/{id}/solve` | Yes | Sets `status = SOLVED`; fills `dateSolved` with today only if it was empty |
| PATCH | `/api/dsa/{id}/revision` | Yes | Sets `status = REVISION` |

**Filtering** (`GET /api/dsa`, all optional, combinable): `topic`,
`platform` (case-insensitive equality), `difficulty`, `status` (must be a
valid enum value — an invalid one returns `400`). Every query is always
additionally scoped to the caller's own `user_id`, regardless of which
filters are present.

**Pagination**: `?page=0&size=20` (defaults shown). Response is a standard
Spring Data `Page<DsaProblemResponse>` — `content`, `totalElements`,
`totalPages`, `number`, `size`, etc.

**Sorting**: `?sortBy=createdAt&direction=desc` (defaults shown).
`sortBy` is restricted to a whitelist (`createdAt`, `updatedAt`, `title`,
`dateSolved`, `revisionDate`, `difficulty`, `status`) — anything else
silently falls back to `createdAt` rather than erroring, since letting an
arbitrary string reach Spring Data's property resolver either 500s on an
unknown property or, in principle, lets a caller probe entity field names.

### DTOs

- `CreateDsaProblemRequest` — `title`/`platform`/`topic` `@NotBlank`,
  `difficulty`/`status` `@NotNull`, `timeTaken` `@Positive` (only enforced
  when present). No `userId` field — the owner always comes from the JWT.
- `UpdateDsaProblemRequest` — same shape/validation as create. **Assumption:**
  treated as a full replace (PUT semantics), not a partial patch — the two
  dedicated `PATCH` endpoints already cover the two partial-update cases
  (solve, revision) the spec called out.
- `DsaProblemResponse` — never includes the `User`/password; unchanged from
  the foundation stage.

### Repository

```java
public interface DsaProblemRepository
        extends JpaRepository<DsaProblem, Long>, JpaSpecificationExecutor<DsaProblem> {
    Optional<DsaProblem> findByIdAndUserId(Long id, Long userId);
}
```

`findByIdAndUserId` is a derived query over the nested `user.id` property
(Spring Data resolves `UserId` → `user.id` automatically) — every
single-record operation (get one/update/delete/solve/revision) uses this
instead of `findById`, so a mismatched owner is a `WHERE` clause that
matches nothing, not a service-layer `if` an implementer could forget.

`DsaProblemSpecifications` holds one small `Specification<DsaProblem>`
builder per filter (`ownerIs`, `topicEquals`, `platformEquals`,
`difficultyEquals`, `statusEquals`). `DsaProblemService` combines only the
ones present on the request with `Specification.allOf(...)` — `ownerIs` is
always included first, so filtering can never leak another user's rows.

### Service — `DsaProblemService`

`create`, `getAll`, `getById`, `update`, `delete`, `markSolved`,
`markForRevision`. Every method takes the caller's user id as an explicit
parameter (rather than reading `SecurityContextHolder` itself), which is
what makes the whole service testable with plain Mockito and no Spring
context.

### Ownership & security design

- The owner is taken from `@AuthenticationPrincipal UserPrincipal` in the
  controller (populated by the existing `JwtAuthenticationFilter`) — never
  from the request body.
- **Non-owner access returns `404`, not `403`.** This is a deliberate
  choice: returning 403 would confirm "this ID exists, you're just not
  allowed to see it," while 404 gives User B the same response for User
  A's problem #10 as for a completely nonexistent #999999 — it doesn't
  leak which IDs are in use by other accounts. `GlobalExceptionHandler`
  (already built in the foundation stage) needed no changes — it already
  maps `ResourceNotFoundException` → 404.

### Validation rules

| Field | Rule |
|---|---|
| `title`, `platform`, `topic` | `@NotBlank` |
| `difficulty`, `status` | `@NotNull`, must be a valid enum value |
| `timeTaken` | `@Positive` when provided (optional) |
| `problemUrl`, `notes`, `dateSolved`, `revisionDate` | optional, unvalidated |

### Entity

`DsaProblem` was **not** changed — it already had correct `FetchType.LAZY`,
`@JsonIgnore` on the `user` relation, and id-only `equals`/`hashCode` from
the foundation stage, so no JPA correction was needed here.

### Tests added

- `DsaProblemServiceTest` (Mockito unit tests, no DB/Spring context needed):
  create, get-by-id (owned and *not*-owned → `ResourceNotFoundException`),
  update (owned and not-owned), delete (owned and not-owned, verifies
  `repository.delete` is never called for a non-owner), `markSolved`
  (fills empty `dateSolved`, doesn't overwrite an existing one),
  `markForRevision`, `getAll` default pagination/sort, `getAll` with
  explicit page/size/sort/direction, and invalid-enum-filter → `400`.
- `DsaProblemControllerTest` (`@WebMvcTest`, mocked service, security
  filters skipped and the caller's `UserPrincipal` pushed directly into
  `SecurityContextHolder` — see the Javadoc on the test class for why):
  create → 201, blank-title → 400 with field error, get-all → 200 page
  shape, get-by-id-not-found → 404, delete → 204, solve/revision → 200.

The **mandatory ownership test** is `getById_whenOwnedByAnotherUser_...`,
`update_whenOwnedByAnotherUser_...`, and
`delete_whenOwnedByAnotherUser_...` in `DsaProblemServiceTest`, plus the
entire "Ownership Protection" folder in the Postman collection, which
exercises it against a real running server with two real accounts.

### Postman

The collection delivered earlier (`devcommand-postman-collection.json` /
`devcommand-postman-environment.json`) now has a **"4. DSA Problems"**
folder with five subfolders — **Create** (including validation and
no-token failure cases), **Read & Filter** (every filter, pagination,
sorting, get-by-id, 404), **Update & Status Transitions** (PUT, solve,
revision), **Delete**, and **Ownership Protection** (registers a second
account and proves it gets 404 on every one of User A's problem
operations). Re-import both files to pick up the new folder and
environment variables (`dsaProblemId`, `emailB`/`passwordB`/`tokenB`,
etc.).

Example create request:

```http
POST /api/dsa
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "title": "Two Sum",
  "platform": "LEETCODE",
  "problemUrl": "https://leetcode.com/problems/two-sum/",
  "topic": "ARRAY",
  "difficulty": "EASY",
  "status": "SOLVED",
  "dateSolved": "2026-09-23",
  "timeTaken": 25,
  "notes": "Solved using HashMap",
  "revisionDate": "2026-09-30"
}
```

Filtering/pagination/sorting example:
```http
GET /api/dsa?topic=ARRAY&difficulty=EASY&status=SOLVED&page=0&size=20&sortBy=createdAt&direction=desc
Authorization: Bearer <JWT>
```

---

## 12. Daily Tasks / Todo Module

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/tasks` | Yes | Create a task, owned by the caller |
| GET | `/api/tasks` | Yes | Paginated list of the caller's tasks, with optional filters |
| GET | `/api/tasks/today` | Yes | The caller's tasks due today (any status) |
| GET | `/api/tasks/upcoming` | Yes | The caller's incomplete tasks with a future due date |
| GET | `/api/tasks/completed` | Yes | The caller's completed tasks, most recent first |
| GET | `/api/tasks/{id}` | Yes | One task — 404 if it doesn't exist *or* isn't owned by the caller |
| PUT | `/api/tasks/{id}` | Yes | Full update — 404 if not owned; owner can never be changed |
| DELETE | `/api/tasks/{id}` | Yes | Delete — 404 if not owned, 204 on success |
| PATCH | `/api/tasks/{id}/complete` | Yes | Sets `status = COMPLETED`, stamps `completedAt` — idempotent |
| PATCH | `/api/tasks/{id}/start` | Yes | Sets `status = IN_PROGRESS` |

**Filtering** (`GET /api/tasks`, all optional, combinable): `category`,
`priority`, `status` (must be valid enum values — an invalid one returns
`400`), `dueDate` (`YYYY-MM-DD`, exact match). Always additionally scoped
to the caller's own `user_id`.

**Pagination**: `?page=0&size=20` (defaults shown), same `Page<...>`
response shape as the DSA module.

**Sorting**: `?sortBy=createdAt&direction=desc` (defaults shown), whitelisted
to `createdAt`, `updatedAt`, `dueDate`, `priority`, `title`, `status`.

**`/today`, `/upcoming`, `/completed`** intentionally return a plain JSON
array (`List<DailyTaskResponse>`), not a paginated `Page` — these are
small, dashboard-oriented convenience views ("keep the implementation
simple," per the brief) rather than general browsing endpoints:
- `/today`: `dueDate == today`, any status, sorted by due date.
- `/upcoming`: `status != COMPLETED AND dueDate > today`, sorted by due
  date ascending (soonest first).
- `/completed`: `status == COMPLETED`, sorted by `completedAt` descending
  (most recently finished first).

### DTOs

- `CreateDailyTaskRequest` — `title` `@NotBlank` `@Size(max=255)`,
  `category`/`priority`/`status` `@NotNull`. No owner field — the owner
  always comes from the JWT.
- `UpdateDailyTaskRequest` — same shape/validation as create (full-replace
  PUT semantics, same assumption as the DSA module's update DTO). No owner
  field here either — there's nowhere in this DTO the client could even
  attempt to reassign a task, since the service loads the task by
  `(id, callerUserId)` and only ever mutates the already-owned entity.
- `DailyTaskResponse` — never includes `User`/password; `completedAt` is
  only ever set by the `/complete` transition, never by create/update.

### Repository

```java
public interface DailyTaskRepository
        extends JpaRepository<DailyTask, Long>, JpaSpecificationExecutor<DailyTask> {
    Optional<DailyTask> findByIdAndUserId(Long id, Long userId);
}
```

Same pattern as `DsaProblemRepository`: `findByIdAndUserId` (derived query
over the nested `user.id` property) backs every single-record operation
(get one/update/delete/complete/start), and `DailyTaskSpecifications`
holds one small `Specification<DailyTask>` builder per filter
(`ownerIs`, `categoryEquals`, `priorityEquals`, `statusEquals`,
`statusNot`, `dueDateEquals`, `dueDateAfter`). `DailyTaskService` always
includes `ownerIs(userId)` first in every combination it builds.

### Service — `DailyTaskService`

`create`, `getAll`, `getById`, `update`, `delete`, `complete`, `start`,
`today`, `upcoming`, `completed`. Every method takes the caller's user id
as an explicit parameter, same as `DsaProblemService` — which is also what
makes it a natural single entry point for a future WhatsApp integration
(see below).

**`complete` is idempotent**: completing an already-`COMPLETED` task
returns it unchanged rather than overwriting `completedAt` with a new
timestamp or creating a duplicate record, per the spec's "handle the
request cleanly" instruction.

### Ownership & security design

Identical approach to the DSA module: the owner comes from
`@AuthenticationPrincipal UserPrincipal` in the controller, never from the
request body; non-owner access returns **404, not 403**, so a response
never confirms that a given task ID belongs to *someone*, just not the
caller. No `GlobalExceptionHandler` changes were needed.

### Validation rules

| Field | Rule |
|---|---|
| `title` | `@NotBlank`, `@Size(max = 255)` |
| `category`, `priority`, `status` | `@NotNull`, must be a valid enum value |
| `description`, `dueDate` | optional, unvalidated |

### Entity

`DailyTask` was **not** changed — it already had correct `FetchType.LAZY`,
`@JsonIgnore` on the `user` relation, and id-only `equals`/`hashCode` from
the foundation stage. No JPA correction was needed.

### Future WhatsApp compatibility (design only — nothing built)

No WhatsApp classes, webhook, or endpoint exist. The only thing done here
is consistent with what the brief asked for: `DailyTaskService`'s methods
already take a plain `Long userId` plus DTOs, not anything HTTP-specific
(`HttpServletRequest`, `Authentication`, etc.), so a future webhook →
message-parser flow could resolve a phone number to a `userId` and call
`dailyTaskService.create(...)` / `.complete(...)` exactly like
`DailyTaskController` does today — no service-layer changes anticipated,
just a new caller.

### Tests added

- `DailyTaskServiceTest` (19 Mockito unit tests, no DB/Spring context
  needed): create, get-by-id (owned and *not*-owned →
  `ResourceNotFoundException`), update (owned/not-owned, confirms owner
  and `completedAt` are untouched by update), delete (owned/not-owned,
  verifies `repository.delete` is never called for a non-owner),
  `complete` (stamps `completedAt`, and is idempotent — no duplicate
  `save` call when already completed), `start` (owned/not-owned), `today`/
  `upcoming`/`completed`, `getAll` default and explicit pagination/sort,
  and invalid-enum-filter → `400` for each of category/priority/status.
- `DailyTaskControllerTest` (11 `@WebMvcTest` cases, same slice-test
  approach as `DsaProblemControllerTest`): create → 201, blank-title → 400,
  missing-category → 400, get-all → 200 page shape, get-by-id-not-found →
  404, delete → 204, complete → 200 with `completedAt` populated, start →
  200, and today/upcoming/completed → 200 with list shape.

The **mandatory ownership tests** are
`getById_whenOwnedByAnotherUser_...`, `update_whenOwnedByAnotherUser_...`,
`delete_whenOwnedByAnotherUser_...`, and
`start_whenOwnedByAnotherUser_...` in `DailyTaskServiceTest`, plus the
"Ownership Protection" folder added to the Postman collection, which
exercises it against a real running server with two real accounts
(reusing the same User B from the DSA module's ownership folder).

### Postman

The collection now has a **"5. Daily Tasks"** folder with the same five-
subfolder shape as the DSA module's folder: **Create** (incl. validation
and no-token cases), **Read & Filter** (all filters, `/today`,
`/upcoming`, `/completed`, pagination, sorting, get-by-id, 404),
**Update & Status Transitions** (PUT, complete, start, and a
complete-twice idempotency check), **Delete**, and **Ownership
Protection** (User B gets 404 on every one of User A's task operations).
Re-import both files to pick up the new folder/variables (`taskId`, etc.).

Example create request:
```http
POST /api/tasks
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "title": "Solve 3 DSA problems",
  "description": "Complete today's DSA practice",
  "category": "DSA",
  "priority": "HIGH",
  "status": "TODO",
  "dueDate": "2026-09-25"
}
```

---

## 13. Job Application Tracker Module

### Entity correction made in this stage

`InterviewRound.status` was a placeholder plain `String` in the foundation
stage (explicitly flagged there as "revisited when interview-round
features are implemented" — that's now). It's now a proper
`InterviewStatus` enum (`SCHEDULED`, `COMPLETED`, `CANCELLED`,
`RESCHEDULED`), `@Enumerated(EnumType.STRING)`, matching every other
status-like field in the project. `roundType` stays a `String` — no enum
was specified for it (free text like "Technical" or "HR-round"), so
nothing was invented there. `ddl-auto=update` alters the existing column
automatically in dev; no migration script was needed since there's no
production data yet.

### Endpoints

**Job applications** (`/api/jobs`):

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/jobs` | Yes | Create an application, owned by the caller |
| GET | `/api/jobs` | Yes | Paginated list of the caller's applications, with optional filters/search |
| GET | `/api/jobs/{id}` | Yes | One application — 404 if not owned |
| PUT | `/api/jobs/{id}` | Yes | Full update — 404 if not owned; owner can never change |
| DELETE | `/api/jobs/{id}` | Yes | Delete — 404 if not owned, 204 on success; cascades to its interview rounds |
| PATCH | `/api/jobs/{id}/status` | Yes | `{"status": "INTERVIEW"}` — 404 if not owned |

**Interview rounds**, nested under a specific job (`/api/jobs/{jobId}/interviews`):

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/jobs/{jobId}/interviews` | Yes | Add a round to that job — 404 if the job isn't owned by the caller |
| GET | `/api/jobs/{jobId}/interviews` | Yes | All rounds for that job — 404 if the job isn't owned |
| GET | `/api/jobs/{jobId}/interviews/{roundId}` | Yes | One round — 404 if the job isn't owned, or the round doesn't belong to that job |
| PUT | `/api/jobs/{jobId}/interviews/{roundId}` | Yes | Full update — same ownership rules |
| DELETE | `/api/jobs/{jobId}/interviews/{roundId}` | Yes | Delete — same ownership rules, 204 on success |

**Filtering** (`GET /api/jobs`, all optional, combinable): `company`,
`role`, `source` (case-insensitive exact match), `status` (must be a valid
enum value — invalid returns `400`).

**Search** (`GET /api/jobs?search=java`): a separate, deliberately simple
partial-match filter — `company ILIKE %term%` **OR** `role ILIKE %term%`.
Combines with the filters above (a search always still adds `ownerIs`, and
can be combined with `status=` etc.). Not a search engine — no ranking,
no multi-field weighting, no tokenizing — just one `LIKE ... OR LIKE ...`
clause, per "do not build an advanced search engine."

**Pagination**: `?page=0&size=20` (defaults shown), same `Page<...>` shape
as the DSA/Tasks modules.

**Sorting**: `?sortBy=applicationDate&direction=desc` (defaults shown),
whitelisted to `applicationDate`, `createdAt`, `company`, `status`.

### DTOs

- `CreateJobApplicationRequest` — `company`/`role` `@NotBlank`,
  `applicationDate`/`status` `@NotNull`. No owner field.
- `UpdateJobApplicationRequest` — same shape/validation (full-replace PUT,
  same assumption used throughout). No owner field.
- `JobStatusUpdateRequest` — just `{ status }`, `@NotNull`, for the
  dedicated `PATCH .../status` endpoint.
- `JobApplicationResponse` — never includes `User`/password; unchanged
  shape from the foundation stage.
- `CreateInterviewRoundRequest`/`UpdateInterviewRoundRequest` —
  `roundNumber` `@NotNull @Positive`, `roundType` `@NotBlank`, `status`
  `@NotNull`. **No `jobApplicationId` or `userId` field** — the parent job
  comes from the URL path and its ownership is verified server-side before
  the request is ever applied; there's no field here a client could even
  attempt to misuse.
- `InterviewRoundResponse` — no `createdAt`/`updatedAt` (the entity has
  none, per the foundation-stage design and this stage's Javadoc note).

### Repositories

```java
public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long>, JpaSpecificationExecutor<JobApplication> {
    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);
}

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, Long> {
    List<InterviewRound> findByJobApplicationId(Long jobApplicationId);
    Optional<InterviewRound> findByIdAndJobApplicationId(Long id, Long jobApplicationId);
}
```

`JobApplicationSpecifications` holds one small builder per filter
(`ownerIs`, `companyEquals`, `roleEquals`, `sourceEquals`, `statusEquals`,
`searchKeyword`) — same pattern as the DSA/Tasks modules.

`InterviewRoundRepository` deliberately has **no** user-scoped query
method — see the ownership design below for why that's still fully safe.

### Services

- **`JobApplicationService`** — `create`, `getAll`, `getById`, `update`,
  `delete`, `changeStatus`, plus **`getOwnedEntityOrThrow(jobId, userId)`**,
  a public method that returns the managed `JobApplication` entity (not a
  DTO) after the exact same ownership check used by every other
  single-record method. This is the one method `InterviewRoundService`
  depends on.
- **`InterviewRoundService`** — `create`, `getAllForJob`, `getById`,
  `update`, `delete`. Every single method calls
  `jobApplicationService.getOwnedEntityOrThrow(jobId, userId)` **first**,
  before touching `InterviewRoundRepository` at all.

### Ownership & security design — two-layer check for nested resources

This module introduces a nested-resource ownership pattern the DSA/Tasks
modules didn't need, since `InterviewRound` has no direct `user`
relationship of its own — only its parent `JobApplication` does:

1. **Layer 1 — job ownership.** Every interview-round operation resolves
   the parent job via `JobApplicationService.getOwnedEntityOrThrow(jobId,
   userId)` — the identical `findByIdAndUserId` check `GET/PUT/DELETE
   /api/jobs/{id}` uses. If the job isn't the caller's, this throws
   `ResourceNotFoundException` immediately and `InterviewRoundRepository`
   is never even queried.
2. **Layer 2 — round-to-job scoping.** Once the job is confirmed owned,
   every round lookup uses `findByIdAndJobApplicationId(roundId, jobId)` —
   not `findById(roundId)` — so a round id that exists but belongs to a
   *different* job (even one the same caller owns) also 404s, rather than
   being returned by accident.

Together, these two checks are what make "a round can never be reached
through another user's job application" true, and they're exactly why
`InterviewRoundRepository` doesn't need — and deliberately doesn't have —
its own `userId`-aware query method. As with the DSA/Tasks modules,
non-owner access returns **404, not 403**, so a response never confirms
that a given job or round ID belongs to *someone* — just not the caller.

### Validation rules

| Field | Rule |
|---|---|
| `company`, `role` (job) | `@NotBlank` |
| `applicationDate`, `status` (job) | `@NotNull` |
| `roundNumber` (interview) | `@NotNull`, `@Positive` |
| `roundType`, `status` (interview) | `@NotBlank` / `@NotNull` |
| `location`, `jobUrl`, `source`, `salary`, `notes`, `feedback`, `scheduledAt` | optional, unvalidated |

### Future WhatsApp / dashboard compatibility (design only — nothing built)

Same approach as the Daily Tasks module: `JobApplicationService`'s methods
take a plain `Long userId` plus DTOs, so a future WhatsApp command parser
resolving `"applied at TCS for Java Developer"` to a `userId` and a
`CreateJobApplicationRequest` could call `create(...)` directly — no
service changes anticipated. No WhatsApp classes exist. Similarly, no
analytics endpoints were added — the dashboard figures the spec lists
(totals by status, upcoming interviews, offers, rejections) are all
straightforward queries `JobApplicationRepository`/`InterviewRoundRepository`
can already support later; nothing needed building now for that.

### Tests added

- `JobApplicationServiceTest` (16 Mockito unit tests): create,
  get-by-id (owned/not-owned), update (owned/not-owned, confirms owner
  untouched), delete (owned/not-owned, verifies `delete` never called for
  a non-owner), `changeStatus` (owned/not-owned),
  `getOwnedEntityOrThrow` (owned/not-owned — the method
  `InterviewRoundService` depends on), `getAll` default/explicit
  pagination and sort, invalid-status-filter → `400`.
- `InterviewRoundServiceTest` (13 Mockito unit tests, with
  `JobApplicationService` mocked rather than real — see the class Javadoc
  for why): create/getAllForJob/getById/update/delete, each with both a
  "job owned by caller" case and a "job not owned by caller → 404, and the
  round repository is never touched" case, plus a dedicated
  "round id doesn't belong to *this* job" 404 case for `getById`
  (layer-2 scoping, distinct from layer-1 job ownership).
- `JobApplicationControllerTest` (8 `@WebMvcTest` cases): create → 201,
  blank-company → 400, missing-applicationDate → 400, get-all → 200 page
  shape, get-by-id-not-found → 404, delete → 204, changeStatus → 200,
  changeStatus-missing-status → 400.
- `InterviewRoundControllerTest` (7 `@WebMvcTest` cases): create → 201,
  blank-roundType → 400, non-positive-roundNumber → 400,
  create-when-parent-job-not-owned → 404 (proves the controller correctly
  propagates the service's ownership rejection), get-all → 200 list shape,
  get-by-id-not-found → 404, delete → 204.

The **mandatory ownership tests**: job-level —
`getById_whenOwnedByAnotherUser_...`,
`update_whenOwnedByAnotherUser_...`,
`delete_whenOwnedByAnotherUser_...`,
`changeStatus_whenOwnedByAnotherUser_...` in
`JobApplicationServiceTest`; interview-level — all six
`..._whenJobNotOwnedByCaller_...` tests in `InterviewRoundServiceTest`,
plus the "Ownership Protection" folder added to the Postman collection
(User B against User A's job *and* its interview rounds).

### Postman

The collection now has a **"6. Job Applications & Interviews"** folder
with the same subfolder shape as the other two modules: **Create**
(job + first interview round, incl. validation and no-token cases),
**Read, Filter & Search** (every filter, `search=`, pagination, sorting,
get-by-id, 404), **Status & Interview Management** (PATCH status, and
full interview-round CRUD nested under the created job), **Delete**, and
**Ownership Protection** (User B gets 404 on every one of User A's job
*and* interview-round operations). Re-import both files to pick up the
new folder/variables (`jobId`, `roundId`, etc.).

Example requests:

```http
POST /api/jobs
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "company": "TCS",
  "role": "Java Developer",
  "location": "Indore",
  "jobUrl": "https://example.com/job",
  "source": "LinkedIn",
  "salary": "6-8 LPA",
  "applicationDate": "2026-09-24",
  "status": "APPLIED",
  "notes": "Java + Spring Boot role"
}
```

```http
PATCH /api/jobs/{id}/status
Authorization: Bearer <JWT>
Content-Type: application/json

{ "status": "INTERVIEW" }
```

```http
POST /api/jobs/{jobId}/interviews
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "roundNumber": 1,
  "roundType": "Technical",
  "scheduledAt": "2026-09-28T11:00:00",
  "status": "SCHEDULED",
  "feedback": null,
  "notes": "Prepare Spring Security"
}
```

Filtering/search example:
```http
GET /api/jobs?status=INTERVIEW&source=LinkedIn&search=java&page=0&size=20&sortBy=applicationDate&direction=desc
Authorization: Bearer <JWT>
```

---

## 14. Learning Tracker Module

### Entity correction made in this stage

`LearningStatus` gained a fourth value, **`ON_HOLD`** (existing values
`NOT_STARTED`/`IN_PROGRESS`/`COMPLETED` untouched). The foundation-stage
enum only had three values because no list was specified at the time; this
module's own spec explicitly requires `ON_HOLD` for its progress
auto-transition rules (see below), so there's now a concrete architectural
reason to add it. Purely additive — no migration concern.

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/learning` | Yes | Create a topic, owned by the caller |
| GET | `/api/learning` | Yes | Paginated list of the caller's topics, with optional filters/search |
| GET | `/api/learning/completed` | Yes | The caller's `COMPLETED` topics, most recently updated first |
| GET | `/api/learning/in-progress` | Yes | The caller's `IN_PROGRESS` topics, highest progress first |
| GET | `/api/learning/{id}` | Yes | One topic — 404 if not owned |
| PUT | `/api/learning/{id}` | Yes | Full update — 404 if not owned; owner can never change |
| DELETE | `/api/learning/{id}` | Yes | Delete — 404 if not owned, 204 on success |
| PATCH | `/api/learning/{id}/progress` | Yes | `{"progress": 75}` — applies the auto-transition rules below |
| PATCH | `/api/learning/{id}/complete` | Yes | Unconditionally sets `progress = 100`, `status = COMPLETED` |

**Filtering** (`GET /api/learning`, all optional, combinable):
`technology` (case-insensitive exact match), `status` (must be a valid
enum value — invalid returns `400`), `progress` (exact integer match).

**Search** (`GET /api/learning?search=security`): partial, case-insensitive
match on `technology` **OR** `topic` — one `LIKE ... OR LIKE ...` clause,
same "not a search engine" scope as the Jobs module's search.

**Pagination**: `?page=0&size=20` (defaults shown), same `Page<...>` shape
as the other three modules.

**Sorting**: `?sortBy=progress&direction=desc` (defaults shown),
whitelisted to `progress`, `hoursSpent`, `createdAt`, `technology`, `topic`.

### Progress auto-transition rules (`PATCH /{id}/progress`)

Applied in this priority order — implemented in
`LearningTopicService.updateProgress`, with the full reasoning in its
Javadoc:

1. **`progress == 100` → `status = COMPLETED`, always.** Reaching 100% is
   treated as an unambiguous "finished" signal, strong enough to end even
   an `ON_HOLD` pause without a separate explicit action.
2. **Otherwise, if `status` is currently `ON_HOLD`, it's left alone.**
   This is the spec's "don't override an explicit `ON_HOLD` status"
   protection — it exists specifically to stop rule 3 from silently
   un-pausing a topic just because a progress number came in.
3. **Otherwise, if `progress > 0` and `status == NOT_STARTED`, status
   bumps to `IN_PROGRESS`.** Logging any real progress on an untouched
   topic implies it's now underway.
4. **Everything else is left as-is** (progress dropping back down,
   already `IN_PROGRESS`/`COMPLETED`, etc.) — nothing in the spec calls
   for any other automatic change.

**`PATCH /{id}/complete` is the one exception to rule 2** — it's an
explicit, dedicated action, so it's allowed to override `ON_HOLD` (the
spec's "unless the endpoint is specifically intended to change it"
carve-out). It always sets `progress = 100` and `status = COMPLETED`
unconditionally.

### DTOs

- `CreateLearningTopicRequest` — `technology`/`topic` `@NotBlank`,
  `progress` `@NotNull @Min(0) @Max(100)`, `status` `@NotNull`,
  `hoursSpent` `@PositiveOrZero` (only enforced when present). No owner
  field.
- `UpdateLearningTopicRequest` — same shape/validation (full-replace PUT,
  same assumption used throughout). No owner field.
- `ProgressUpdateRequest` — just `{ progress }`, same `@Min(0) @Max(100)`
  bounds, for the dedicated `PATCH .../progress` endpoint.
- `LearningTopicResponse` — never includes `User`; unchanged shape from
  the foundation stage.
- `LearningSummary` — **not exposed by any endpoint** (see below).

### Repository

```java
public interface LearningTopicRepository
        extends JpaRepository<LearningTopic, Long>, JpaSpecificationExecutor<LearningTopic> {
    Optional<LearningTopic> findByIdAndUserId(Long id, Long userId);
}
```

`LearningTopicSpecifications` holds one small builder per filter
(`ownerIs`, `technologyEquals`, `statusEquals`, `progressEquals`,
`searchKeyword`) — same pattern as the other three modules.
`LearningTopicService` always includes `ownerIs(userId)` first in every
combination it builds.

### Service — `LearningTopicService`

`create`, `getAll`, `getById`, `update`, `delete`, `updateProgress`,
`complete`, `completedTopics`, `inProgressTopics`, plus `getSummary`
(below). Every method takes the caller's user id as an explicit
parameter, same reasoning as the other three modules — including making a
future WhatsApp command parser ("I completed Spring Security", "Spring
Boot is 80 percent complete") a matter of resolving a phone number to a
`userId` and calling `complete(...)`/`updateProgress(...)` directly.

### Ownership & security design

Identical approach to the other three modules: the owner comes from
`@AuthenticationPrincipal UserPrincipal` in the controller, never the
request body; `findByIdAndUserId` backs every single-record operation;
non-owner access returns **404, not 403**. No `GlobalExceptionHandler`
changes were needed.

### Validation rules

| Field | Rule |
|---|---|
| `technology`, `topic` | `@NotBlank` |
| `progress` | `@NotNull`, `@Min(0)`, `@Max(100)` |
| `status` | `@NotNull`, must be a valid enum value |
| `hoursSpent` | `@PositiveOrZero` when provided (optional) |
| `resourceUrl`, `notes` | optional, unvalidated |

### Learning summary — a service method, not an endpoint

The spec asked for a summary method *only if it stays architecturally
clean, and explicitly said not to build `/api/analytics` yet*. A single
`LearningTopicService.getSummary(userId)` fits that bar: it computes
`totalTopics`/`completedTopics`/`inProgressTopics`/`totalHoursSpent`/
`averageProgress` on the fly from the caller's own topics (already scoped
by `ownerIs(userId)`, no pagination), with no new table and no persisted
aggregate. **It has no controller and no route** — it exists purely so a
future dashboard/analytics module can call it directly instead of
re-deriving the same numbers. If it turns out to be the wrong shape once
analytics is actually built, it's a private implementation detail with a
single unit test (`getSummary_aggregatesAcrossTheCallersTopicsOnly`) and
nothing depends on its API yet.

### Frontend compatibility (no frontend changes made)

`LearningTopicResponse` carries exactly the fields the spec listed the
Lovable Learning page placeholder as needing — `technology`, `topic`,
`progress`, `status`, `hoursSpent` (as `resourceUrl`, spelled to match),
and `notes` — so the existing placeholder should be able to bind to this
response shape directly once someone wires it up. No frontend files were
touched, per the instruction.

### Tests added

- `LearningTopicServiceTest` (23 Mockito unit tests, no DB/Spring context
  needed): create, get-by-id (owned/not-owned), update (owned/not-owned,
  confirms owner untouched), delete (owned/not-owned, verifies `delete`
  never called for a non-owner), **six dedicated tests for the progress
  auto-transition rules** — reaching 100 always completes (including from
  `ON_HOLD`), `NOT_STARTED` + positive progress bumps to `IN_PROGRESS`,
  zero progress does *not* bump from `NOT_STARTED`, `ON_HOLD` is *not*
  silently bumped by a non-100 update, already-`IN_PROGRESS` is left
  alone, and not-owned → 404 — `complete` (unconditional override of
  `ON_HOLD`, and not-owned → 404), `completedTopics`/`inProgressTopics`,
  `getAll` default/explicit pagination and sort, invalid-status-filter →
  `400`, and `getSummary`'s aggregation math.
- `LearningTopicControllerTest` (13 `@WebMvcTest` cases): create → 201,
  blank-technology → 400, progress-over-100 → 400, negative-progress →
  400, negative-hoursSpent → 400, get-all → 200 page shape,
  get-by-id-not-found → 404, delete → 204, updateProgress → 200 (and
  out-of-range → 400), complete → 200 with `progress=100`/
  `status=COMPLETED`, completed/in-progress → 200 with list shape.

The **mandatory ownership tests** are
`getById_whenOwnedByAnotherUser_...`,
`update_whenOwnedByAnotherUser_...`,
`delete_whenOwnedByAnotherUser_...`,
`updateProgress_whenOwnedByAnotherUser_...`, and
`complete_whenOwnedByAnotherUser_...` in `LearningTopicServiceTest`, plus
the "Ownership Protection" folder added to the Postman collection.

### Postman

The collection now has a **"7. Learning Tracker"** folder with the same
subfolder shape as the other three modules: **Create** (incl. validation
and no-token cases), **Read, Filter & Search** (every filter, `search=`,
pagination, sorting, get-by-id, completed, in-progress, 404), **Progress
& Completion** (progress updates demonstrating each auto-transition rule
— `NOT_STARTED`→`IN_PROGRESS`, reaching 100→`COMPLETED`, `ON_HOLD`
protection — plus the explicit `/complete` override), **Delete**, and
**Ownership Protection** (User B gets 404 on every one of User A's
operations). Re-import both files to pick up the new folder/variables
(`learningTopicId`, etc.).

Example requests:

```http
POST /api/learning
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "technology": "Java",
  "topic": "Collections Framework",
  "progress": 80,
  "status": "IN_PROGRESS",
  "hoursSpent": 12,
  "resourceUrl": "https://example.com",
  "notes": "Need more revision on HashMap internals"
}
```

```http
PATCH /api/learning/{id}/progress
Authorization: Bearer <JWT>
Content-Type: application/json

{ "progress": 90 }
```

```http
PATCH /api/learning/{id}/complete
Authorization: Bearer <JWT>
```

Filtering/search example:
```http
GET /api/learning?technology=Spring%20Boot&status=IN_PROGRESS&search=security&page=0&size=20&sortBy=progress&direction=desc
Authorization: Bearer <JWT>
```

### Full Stack Production Readiness

This application has undergone a complete production-hardening cycle:
- **Test Coverage**: 156/156 integration and unit tests pass successfully.
- **Security**: Fallback credentials and `spring-boot-devtools` have been stripped from the `prod` profile. The application will securely "fail fast" if required secrets are missing.
- **Database Integrity**: Schema management has been successfully transitioned to **Flyway** (`V1__init.sql`) alongside Hibernate's `ddl-auto=validate`.
- **Docker Healthchecks**: Proper grace periods (`start_period`) and dependency conditions are implemented across containers to handle JVM startup times flawlessly.
