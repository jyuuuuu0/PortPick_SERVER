---
name: api-design
description: >
  PortPick REST API design guidelines. Provides endpoint naming conventions, JPA entity design patterns,
  and DTO contract definitions. Used by the api-architect agent when designing new features.
---

# PortPick API Design Guidelines

## 1. Project Context

**Stack:** Spring Boot 4.0.6, Java 21, JPA + MySQL, Redis, Spring Security + JWT (JJWT 0.12.5), Lombok

**Implemented domains:**
- `User` — Google OAuth login, profile (name, organization, `JobRole`, `CareerType`, `CareerRange`, profile image)
- `Portfolio` — title, description, embed link, file URL, original filename, author (User), created timestamp

**Domains to design (primary targets for this skill):**
- `Like` — portfolio likes (User ↔ Portfolio)
- `Comment` — location-based comments on a portfolio (GPS coordinates + content)
- `Reply` — replies to a comment
- Portfolio filtering (add queries to existing entities)
- My page (aggregation API)

---

## 2. URL Conventions

```
GET    /api/v1/{resource}           — list
POST   /api/v1/{resource}           — create
GET    /api/v1/{resource}/{id}      — single item
PUT    /api/v1/{resource}/{id}      — update
DELETE /api/v1/{resource}/{id}      — delete

Nested resources:
POST   /api/v1/portfolios/{id}/likes        — toggle like
GET    /api/v1/portfolios/{id}/comments     — list comments
POST   /api/v1/portfolios/{id}/comments     — create comment
DELETE /api/v1/comments/{id}               — delete comment
POST   /api/v1/comments/{id}/replies        — create reply
PATCH  /api/v1/comments/{id}/resolve        — mark comment as resolved
GET    /api/v1/users/me                    — my page
```

---

## 3. JPA Entity Pattern

```java
@Entity
@Getter
@NoArgsConstructor
@Table(name = "table_name")
public class EntityName {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Associations: always LAZY fetch
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Created timestamp
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Use @Builder for construction
    @Builder
    public EntityName(...) { ... }

    // Mutations as instance methods
    public void update(...) { ... }
}
```

**Entity design rules:**
- Represent M:N relationships with an intermediate entity (e.g., `Like`). Do not use `@ManyToMany`.
- Use `cascade` only when the parent–child relationship is clear (e.g., Portfolio → files, Comment → Reply).
- Use `orphanRemoval = true` when child records must be deleted with the parent.
- All `nullable = false` fields must be initialized in the constructor.

---

## 4. DTO Pattern

```java
// Request DTO — @Getter + @NoArgsConstructor (for @ModelAttribute binding)
@Getter
@NoArgsConstructor
public class XxxRequest {
    private String fieldName;
}

// Response DTO — record with static factory
public record XxxResponse(
    Long id,
    String fieldName
) {
    public static XxxResponse from(Entity entity) {
        return new XxxResponse(entity.getId(), entity.getFieldName());
    }
}
```

**DTO naming:**
- Create request: `{Entity}CreateRequest`
- Update request: `{Entity}UpdateRequest`
- Response: `{Entity}Response` or `{Entity}DetailResponse`
- List response: `{Entity}ListResponse` or `List<{Entity}Response>` returned directly

---

## 5. SecurityConfig Rules Pattern

```java
// Read-only (allow anonymous)
.requestMatchers(HttpMethod.GET, "/api/v1/portfolios/*/comments").permitAll()

// Write (require authentication)
.requestMatchers("/api/v1/portfolios/*/likes").authenticated()
.requestMatchers("/api/v1/comments/**").authenticated()
.requestMatchers("/api/v1/users/me").authenticated()
```

Always add explicit rules for new endpoints. Do not rely on the `anyRequest().authenticated()` catch-all — explicit rules are required for clarity.

---

## 6. Business Rule Design Guide

| Rule type | Where to enforce | Example |
|-----------|-----------------|---------|
| Ownership check | Service `validateOwner()` | Only the author can delete a comment |
| Duplicate prevention | `existsBy` in repository + check in service | Prevent duplicate likes |
| State transition | Service method | Resolve a comment (one-way only) |
| Cascading cleanup | JPA cascade or service | Delete likes and comments when a portfolio is deleted |

---

## 7. Portfolio Filtering Design

```java
// Repository — @Query with optional parameters
@Query("SELECT p FROM Portfolio p JOIN p.user u " +
       "WHERE (:jobRole IS NULL OR u.jobRole = :jobRole) " +
       "AND (:careerRange IS NULL OR u.careerRange = :careerRange)")
Page<Portfolio> findWithFilters(
    @Param("jobRole") JobRole jobRole,
    @Param("careerRange") CareerRange careerRange,
    Pageable pageable
);

// Controller
@GetMapping
public ResponseEntity<Page<PortfolioResponse>> getPortfolios(
    @RequestParam(required = false) JobRole jobRole,
    @RequestParam(required = false) CareerRange careerRange,
    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
)
```
