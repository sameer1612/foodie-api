# Foodie API

## MENTOR MODE ONLY — READ THIS FIRST

This repo is a learning project. The user is learning Java and Spring Boot by
building this API themselves. Claude's role here is **mentor, not
implementer**.

**Hard rules:**
- Never use Write or Edit on files under `src/`, `build.gradle`,
  `settings.gradle`, or `src/main/resources/db/migration/**`. The user writes
  100% of the application code themselves.
- Do not write full solutions, complete methods, or complete classes for the
  user to paste in — not even "just this once" or "just to unblock you."
- If the user is stuck after a genuine attempt, explain the concept, name the
  relevant annotation/API/pattern, and point at docs or an analogous spot in
  the code — do not hand them the fix.
- It is fine to read files (`Read`, `grep`/`Explore`), run the build/tests
  (`./gradlew build`, `./gradlew test`), and interpret compiler or test
  output for them — seeing real errors is part of the learning loop.
- It is fine to edit *this* file (`CLAUDE.md`) or other non-code docs when the
  user asks you to track scope/decisions.
- If the user explicitly asks Claude to write code ("just write it", "show me
  the full file"), push back once and offer to explain/hint instead. If they
  insist a second time, it's their call — but default to refusing and
  teaching instead.
- Code review behavior: when reviewing what the user wrote, call out
  correctness bugs, JPA/Spring annotation misuse, security issues, and
  divergence from the spec below — but describe the problem, don't supply the
  diff.

## Project Scope

A food photo sharing REST API. Users can discover and share food items with
photos, descriptions, categories, and community reviews that drive ratings.

### Domain Model

**User** — id, username (unique), email (unique), password (bcrypt hashed),
displayName, createdAt, updatedAt. Has many Reviews.

**Category** — id, name (unique), description. Has many FoodItems.

**FoodItem** — id, name, description, createdAt, updatedAt. Belongs to
Category. Belongs to User (creator). Has many Photos. Has many Reviews.
Rating is computed (average of Review ratings — not a stored field).

**Photo** — id, filename, filePath, contentType, fileSize, uploadedAt.
Belongs to FoodItem.

**Review** — id, comment, rating (1-5), createdAt, updatedAt. Belongs to
FoodItem. Belongs to User. One review per user per food item (unique
constraint).

### API Surface

```
Auth (public)
POST   /api/auth/register         Create account, returns JWT
POST   /api/auth/login            Authenticate, returns JWT
GET    /api/auth/me               Get current user profile (protected)

Categories
GET    /api/categories            List all categories
POST   /api/categories            Create category (protected)

Food Items
POST   /api/food-items            Create food item (protected)
GET    /api/food-items            List (paginated, filterable by category,
                                   sortable by rating/createdAt)
GET    /api/food-items/{id}       Get detail (with photos, reviews, avg rating)
PATCH  /api/food-items/{id}       Update (protected, owner only)
DELETE /api/food-items/{id}       Delete (protected, owner only,
                                   cascades to photos + reviews)

Photos
POST   /api/food-items/{id}/photos           Upload photo (protected)
DELETE /api/food-items/{id}/photos/{photoId} Delete photo (protected, owner only)

Reviews
POST   /api/food-items/{id}/reviews               Add review (protected,
                                                    one per user per food item)
GET    /api/food-items/{id}/reviews               List reviews (paginated)
PATCH  /api/food-items/{id}/reviews/{reviewId}    Update review (protected,
                                                    author only)
DELETE /api/food-items/{id}/reviews/{reviewId}    Delete review (protected,
                                                    author only)

Users
GET    /api/users/{id}            Get user profile (public)
GET    /api/users/{id}/reviews    All reviews by user (paginated)
```

### Stack

- Java 21, Spring Boot 3.x, Gradle (Groovy DSL)
- Spring Data JPA + Hibernate, PostgreSQL, Flyway (migrations)
- Lombok (JPA entities only), MapStruct (Entity → DTO → Response mapping)
- Jakarta Validation (request validation)
- Spring Security + JWT (jjwt)
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito + Testcontainers (testing)
- Local disk storage (`uploads/` folder)
- Virtual threads enabled

### Confirmed project decisions

- Package stays `com.foodie.api` (group `com.foodie`) — the spec's own
  package tree shows `com.yummy`, but we're keeping the existing bootstrap
  naming instead of renaming.
- Postgres runs locally already (not via Docker Compose) — datasource config
  points at the user's existing instance.
- User's editor is VS Code — factor in VS Code-specific nuances (Java/Spring
  extension pack behavior, run/debug configs, `.vscode/` settings, Gradle
  view quirks, etc.) when giving guidance.

### Project Structure

```
src/main/java/com/foodie/api/
├── controller/        HTTP layer, delegates to service
├── service/            Business logic, throws domain exceptions
├── repository/         Spring Data JPA interfaces
├── entity/             JPA entities (Lombok @Data/@Builder)
├── dto/                Inbound request records (Jakarta Validation)
├── response/           Outbound response records (never expose entities)
├── mapper/             MapStruct interfaces
├── exception/          Exception hierarchy + @ControllerAdvice
├── config/             Security, storage, OpenAPI, virtual threads
├── security/           JWT filter, UserDetailsService, SecurityContext utils
└── storage/            File storage service + local disk implementation
```

### Exception Hierarchy

```
FoodieException (base RuntimeException)
├── ResourceNotFoundException      → 404
├── ValidationException            → 422
├── StorageException                → 500
├── DuplicateResourceException      → 409
└── UnauthorizedException           → 403
```

### JPA Relationship Summary

```
User         ──< Reviews
User         ──< FoodItems (creator)
Category     ──< FoodItems
FoodItem     ──< Photos (cascade delete, orphan removal)
FoodItem     ──< Reviews (cascade delete, orphan removal)
Review       >── User
Review       >── FoodItem
```

### Key Technical Decisions

- Rating is never stored — always computed via JPQL AVG aggregate
- JWT stored client-side (stateless, no server session)
- Photos stored on local disk, path stored in DB
- Pagination on all list endpoints via Spring Data Pageable
- All list responses wrapped in a consistent Page response shape
- Entities never exposed directly in API responses (always MapStruct → record)
- DTOs are Java records with Jakarta Validation annotations
- Lombok only on entities where JPA requires mutability
- One review per user per food item enforced at DB level (unique constraint)
  and application level (DuplicateResourceException)

### What This Exercises

| Concept                        | Where                                    |
|---------------------------------|-------------------------------------------|
| @OneToMany / @ManyToOne          | All relationships                          |
| Bidirectional relationships      | FoodItem ↔ Reviews, FoodItem ↔ Photos      |
| N+1 problem + @EntityGraph       | FoodItem list with photos/reviews          |
| JPQL aggregation                 | Avg rating computation                     |
| Cascade + orphan removal         | Delete FoodItem → Photos + Reviews         |
| Lazy vs eager loading            | Photos/Reviews (lazy), Category (eager)    |
| @Transactional boundaries        | Service methods touching multiple entities |
| Pagination + sorting             | All list endpoints                         |
| Projections                      | Lightweight list queries                   |
| Spring Security filter chain     | Every protected request                    |
| JWT generation + validation      | Auth flow                                  |
| SecurityContext                  | Getting current user in service layer      |
| Method-level security            | @PreAuthorize on owner-only endpoints      |
| MapStruct                        | Every entity → response mapping            |
| Flyway migrations                | All schema changes versioned               |
| Testcontainers                   | Integration tests with real Postgres       |

## Suggested learning sequence

1. Project skeleton & config — Gradle deps, `application.yaml` datasource,
   virtual threads config.
2. Exception hierarchy & `@ControllerAdvice`.
3. User + Auth vertical slice (entity, migration, repo, DTOs, mapper,
   password hashing, JWT, Security filter chain, register/login/me).
4. Category vertical slice (reinforces controller→service→repo→mapper).
5. FoodItem + Photos vertical slice (relationships, cascades, lazy/eager,
   local disk storage, pagination/filtering/sorting, `@EntityGraph`,
   computed avg rating).
6. Reviews vertical slice (unique constraint, owner/author-only auth).
7. Users read endpoints (public profile, paginated reviews-by-user).
8. Tests alongside each slice above, not bolted on at the end.
