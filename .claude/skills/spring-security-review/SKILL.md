---
name: spring-security-review
description: >
  PortPick Spring Security review guidelines. Covers JWT handling, OAuth2 flow, endpoint authorization,
  ownership validation, input validation, and information exposure.
  Used by the security-reviewer agent when reviewing implemented code.
---

# PortPick Security Review Guidelines

## 1. Security Architecture

**Authentication flow:**
1. Google OAuth2 → `CustomOAuth2UserService` → `OAuth2SuccessHandler` → JWT issued → set as cookie
2. Subsequent requests: `JwtAuthenticationFilter` → validate JWT → set `Authentication` object
3. In controllers: `authentication.getName()` = user's email

**JWT implementation:** JJWT 0.12.5, cookie name: `PORTPICK_ACCESS_TOKEN`

**Exception mapping (ApiExceptionHandler):**
- `IllegalArgumentException` → 400 Bad Request
- `IllegalStateException` → 401 Unauthorized
- `AccessDeniedException` → 403 Forbidden

---

## 2. Security Checklist

### 2-1. SecurityConfig Endpoint Rules

Verify that new endpoints are explicitly registered in SecurityConfig.

```java
// Correct: GET is public, writes require authentication
.requestMatchers(HttpMethod.GET, "/api/v1/portfolios/*/comments").permitAll()
.requestMatchers("/api/v1/portfolios/*/comments").authenticated()

// Wrong: relying only on anyRequest().authenticated()
```

**BLOCKER condition:** An endpoint that requires authentication is inadvertently covered by `permitAll()`.

### 2-2. Ownership Validation

Verify that all mutating APIs call an ownership check method.

```java
// Correct pattern
private void validateOwner(User user, Comment comment) {
    if (!comment.getUser().getId().equals(user.getId())) {
        throw new AccessDeniedException("...");
    }
}

// BLOCKER: delete without ownership check
public void deleteComment(String email, Long commentId) {
    Comment comment = getComment(commentId);
    // Missing validateOwner() — any authenticated user can delete any comment
    commentRepository.delete(comment);
}
```

**BLOCKER conditions:**
- DELETE, PUT, or PATCH endpoint missing `validateOwner()` or equivalent check
- Ownership validated only in the controller, not the service

### 2-3. Input Validation

```java
// Coordinate range validation (lat: -90 to 90, lng: -180 to 180)
if (latitude != null && (latitude < -90 || latitude > 90)) {
    throw new IllegalArgumentException("Latitude must be between -90 and 90.");
}

// Text length: code validation must match @Column(length = N)
// content @Column(length = 1000) → service must reject strings longer than 1000 chars
```

**INFO condition:** Validation absent or mismatched with DB column constraint.

### 2-4. Information Exposure

Check that response DTOs do not return sensitive data.

```java
// BLOCKER: returning a User entity directly as a response
// INFO: response unnecessarily includes internal userId or email
```

**BLOCKER condition:** Passwords or internal config values exposed in the response.  
**INFO condition:** Email or ID unnecessarily exposed to external consumers.

### 2-5. SQL Injection

```java
// BLOCKER: string concatenation in a native query
@Query(value = "SELECT * FROM portfolios WHERE title LIKE '%" + title + "%'", nativeQuery = true)

// Correct: parameter binding
@Query(value = "SELECT * FROM portfolios WHERE title LIKE %:title%", nativeQuery = true)
// Or use JPQL
@Query("SELECT p FROM Portfolio p WHERE p.title LIKE %:title%")
```

**BLOCKER condition:** User input is directly concatenated into a query string.

### 2-6. JWT Handling

Verify exception handling in `JwtAuthenticationFilter` and `JwtProvider`.

```java
// Check list
- Expired token  → returns 401
- Tampered token → returns 401
- JwtProvider.parseToken() is wrapped in try-catch
```

**INFO condition:** Missing exception handling that could cause a 500 response.

---

## 3. Fix Priority

| Level | Content | Action |
|-------|---------|--------|
| **BLOCKER** | Auth bypass, missing ownership check, SQL injection | Fix code directly; include fix in report |
| **INFO** | Missing input validation, unnecessary info exposure, best-practice suggestions | Record only; fixing is optional |

---

## 4. Report Format

```
## [BLOCKER] Missing ownership validation
- File: src/main/java/.../CommentService.java (deleteComment method)
- Problem: Comment deleted without calling validateOwner()
- Fix: Added validateOwner(user, comment) call → fixed directly

## [INFO] No coordinate range validation
- File: src/main/java/.../CommentService.java
- Problem: latitude/longitude range not validated
- Recommendation: Consider adding range check (-90 to 90, -180 to 180)
```
