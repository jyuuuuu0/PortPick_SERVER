---
name: portpick-qa
description: >
  PortPick integration consistency validation guidelines. Cross-compares the API design document
  against the implementation code to find boundary mismatches between layers.
  Used by the qa-validator agent when validating a completed implementation.
---

# PortPick QA Validation Guidelines

## 1. Core Principle

The goal is not "does the file exist?" — it is **"does the boundary match?"**

Open two files simultaneously and compare them:
- Controller method signature ↔ Service method signature
- Service repository call ↔ Repository interface declaration
- Design document DTO fields ↔ Actual DTO class fields

---

## 2. Validation Checklist

### 2-1. Design ↔ Implementation Alignment

Compare `_workspace/01_api_design.md` against the actual controllers:

| Check | How |
|-------|-----|
| Every design endpoint exists in a controller | Design URL vs `@GetMapping` / `@PostMapping` annotation |
| HTTP methods match | Design method vs annotation type |
| URL path parameters match | Design `{id}` vs `@PathVariable Long id` |
| Response HTTP status matches | Design 200/201/204 vs `ResponseEntity.ok()` / `.status(CREATED)` |

### 2-2. Controller ↔ Service Boundary

```
// Verify this pattern
Controller: return ResponseEntity.ok(commentService.createComment(email, portfolioId, request));
Service:    public CommentResponse createComment(String email, Long portfolioId, CommentCreateRequest request)

Checks:
- Method name matches: createComment ✓
- Parameter types match: String, Long, CommentCreateRequest ✓
- Return type matches: CommentResponse ✓
```

**FAIL example:**
```
Controller: commentService.createComment(email, request, portfolioId)  // wrong parameter order
Service:    createComment(String email, Long portfolioId, CommentCreateRequest request)
```

### 2-3. Service ↔ Repository Boundary

```
// Service calls
commentRepository.findByPortfolioOrderByCreatedAtDesc(portfolio)

// Verify in repository interface
List<Comment> findByPortfolioOrderByCreatedAtDesc(Portfolio portfolio);  // does this exist?
```

**FAIL example:** Service calls `findAllByPortfolioId(Long portfolioId)` but repository only declares `findByPortfolio(Portfolio portfolio)`.

### 2-4. DTO Field Consistency

Compare design document DTO spec against actual DTO class:

```
// Design document
CommentResponse: id(Long), content(String), latitude(Double), longitude(Double),
                 resolved(boolean), createdAt(LocalDateTime), authorName(String)

// Actual DTO
public record CommentResponse(Long id, String content, boolean resolved, LocalDateTime createdAt)
// → latitude, longitude, authorName are missing → FAIL
```

### 2-5. Business Rules Implementation

Cross-check every rule in the design's "Business Rules" section against the service code:

| Design rule | What to verify in service |
|-------------|--------------------------|
| One like per user | `existsByUserAndPortfolio()` check or toggle logic |
| Only portfolio owner can resolve a comment | `validateOwner()` call present |
| Replies can only be added to top-level comments (no nested replies) | `parent.getParent() == null` guard |

### 2-6. Entity Consistency

For every new entity:

```java
// Must verify
✓ @ManyToOne(fetch = FetchType.LAZY)  — not EAGER
✓ @Column(nullable = false)           — on required fields
✓ @CreationTimestamp                  — on the createdAt field
✓ @UniqueConstraint                   — where duplicates must be prevented (e.g., Like)
✓ cascade / orphanRemoval             — for parent-child deletion
```

**FAIL example:** `@ManyToOne(fetch = FetchType.EAGER)` — can cause N+1 query issues.

---

## 3. Report Format

```
## [PASS] Controller ↔ Service boundary: CommentController.createComment
- Method signature, parameter types, and return type all match.

## [FAIL] DTO field mismatch: CommentResponse
- File: src/main/java/.../dto/CommentResponse.java
- Design: includes latitude, longitude, authorName
- Implementation: latitude, longitude, authorName are missing
- Recommendation: add the missing fields to CommentResponse

## [FAIL] Repository method not declared
- File: src/main/java/.../repository/CommentRepository.java
- Service calls: findByPortfolioOrderByCreatedAtDesc(Portfolio portfolio)
- Repository: method not found
- Recommendation: declare the method in CommentRepository
```

---

## 4. Validation Priority

| Priority | Item | Reason |
|----------|------|--------|
| P1 | Controller ↔ Service type mismatch | Runtime error |
| P1 | Repository method not declared | Compile or runtime error |
| P2 | DTO field missing | Incomplete API response |
| P2 | Business rule not implemented | Functional defect |
| P3 | Entity FetchType.EAGER | Performance issue |
