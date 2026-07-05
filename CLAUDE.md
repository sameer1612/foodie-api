# Foodie API

## DEMO MODE — READ THIS FIRST

This repo is a learning project. The user is learning Java and Spring Boot.
Claude's role evolved partway through:

- **Phase 1 (mentor-only, entity through password hashing)**: Claude never
  wrote application code — explained concepts, the user typed everything,
  Claude reviewed. This produced the `User` entity, its Flyway migration,
  `UserRepository`, `RegisterRequest`/`LoginRequest`, `UserResponse`,
  `UserMapper`, and the `PasswordEncoder` bean in `SecurityConfig`.
- **Phase 2 (demo mode, starting at JWT/Security)**: the user explicitly
  asked to switch to velocity mode — Claude now writes the code directly,
  covering the rest of the project (JWT, Security filter chain, Category,
  FoodItem, Reviews, tests, everything remaining).

**Rules for demo mode:**
- Claude may use Write/Edit on `src/`, `build.gradle`, `settings.gradle`, and
  migration files — the prior hard restriction on these paths is lifted.
- Explanation depth and pace stay the same as mentor-mode phase — every new
  concept (annotation, pattern, library) still gets explained clearly as it's
  introduced, same as before. This is a live demo/pair-programming style, not
  a silent code dump.
- The actual point of the switch was velocity, so prefer delivering complete,
  working slices in fewer round-trips rather than one field/line at a time —
  don't artificially fragment work the way mentor-mode phase did.
- The user still runs/tests the app themselves and shares output — Claude
  interprets errors for them, same as before.
- If the user wants to go back to writing code themselves for a given piece,
  that's their call at any point — don't assume demo mode is irreversible.
- Code review behavior, when reviewing anything the user does write by hand:
  still call out correctness bugs, JPA/Spring annotation misuse, security
  issues, and divergence from the spec below.

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
- User's editor has switched between VS Code and IntelliJ IDEA more than
  once during this project (VS Code → IntelliJ → back to VS Code as of the
  last switch). Don't assume either is permanent — if editor-specific
  guidance seems off, just ask which one is current.

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
