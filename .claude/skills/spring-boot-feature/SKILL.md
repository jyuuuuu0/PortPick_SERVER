---
name: spring-boot-feature
description: >
  PortPick Spring Boot layered-architecture implementation patterns. Provides coding conventions and
  concrete code examples for Entity, Repository, DTO, Service, and Controller.
  Used by the feature-developer agent when implementing new features.
---

# PortPick Spring Boot Implementation Patterns

## 1. Package Structure

```
com.example.PortPick_SERVER/
├── config/          — SecurityConfig, WebResourceConfig, etc.
├── controller/      — @RestController classes, ApiExceptionHandler
├── dto/             — request / response DTOs
├── filter/          — JwtAuthenticationFilter
├── handler/         — OAuth2 success/failure handlers
├── jwt/             — JwtProvider
├── model/           — JPA entities, enums
├── repository/      — JpaRepository interfaces
└── service/         — business logic
```

Place new files in the package that matches their responsibility.

---

## 2. Controller Pattern

```java
@RestController
@RequestMapping("/api/v1/likes")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/portfolios/{portfolioId}")
    public ResponseEntity<LikeResponse> toggleLike(
            Authentication authentication,
            @PathVariable Long portfolioId
    ) {
        return ResponseEntity.ok(likeService.toggleLike(authentication.getName(), portfolioId));
    }
}
```

**Rules:**
- `authentication.getName()` — retrieves the email (JWT principal = email)
- Use `@PathVariable`, `@RequestParam`, `@ModelAttribute`, or `@RequestBody` as appropriate
- Created successfully: `ResponseEntity.ok(...)` or `ResponseEntity.status(HttpStatus.CREATED).body(...)`
- Deleted successfully: `ResponseEntity.noContent().build()`
- Constructor injection; `@RequiredArgsConstructor` is acceptable

---

## 3. Service Pattern

```java
@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    public LikeService(LikeRepository likeRepository,
                       PortfolioRepository portfolioRepository,
                       UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LikeResponse toggleLike(String email, Long portfolioId) {
        User user = getUser(email);
        Portfolio portfolio = getPortfolio(portfolioId);

        return likeRepository.findByUserAndPortfolio(user, portfolio)
                .map(like -> {
                    likeRepository.delete(like);
                    return new LikeResponse(portfolioId, false);
                })
                .orElseGet(() -> {
                    likeRepository.save(new Like(user, portfolio));
                    return new LikeResponse(portfolioId, true);
                });
    }

    // Ownership validation pattern
    private void validateOwner(User user, Comment comment) {
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only modify or delete your own comment.");
        }
    }

    // User lookup helper (same pattern across all services)
    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
    }

    private Portfolio getPortfolio(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));
    }
}
```

**Rules:**
- Writes: `@Transactional`; reads: `@Transactional(readOnly = true)`
- Exceptions: `IllegalArgumentException` (bad input / not found), `IllegalStateException` (business rule violation), `AccessDeniedException` (ownership failure)
- Extract repeated logic into helper methods: `getUser()`, `get{Entity}()`, `validateOwner()`

---

## 4. Repository Pattern

```java
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndPortfolio(User user, Portfolio portfolio);

    boolean existsByUserAndPortfolio(User user, Portfolio portfolio);

    long countByPortfolio(Portfolio portfolio);

    void deleteAllByPortfolio(Portfolio portfolio);

    // Filtering query
    @Query("SELECT p FROM Portfolio p JOIN p.user u " +
           "WHERE (:jobRole IS NULL OR u.jobRole = :jobRole) " +
           "AND (:careerRange IS NULL OR u.careerRange = :careerRange) " +
           "ORDER BY p.createdAt DESC")
    List<Portfolio> findWithFilters(
            @Param("jobRole") JobRole jobRole,
            @Param("careerRange") CareerRange careerRange,
            Pageable pageable
    );
}
```

**Rules:**
- Follow Spring Data JPA naming conventions: `findBy`, `existsBy`, `countBy`, `deleteAllBy`
- Use JPQL `@Query` for complex queries; avoid native queries unless unavoidable (always use parameter binding)
- Use `Pageable` for paginated results

---

## 5. Entity Pattern (Like example)

```java
@Entity
@Getter
@NoArgsConstructor
@Table(name = "likes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "portfolio_id"}))
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Like(User user, Portfolio portfolio) {
        this.user = user;
        this.portfolio = portfolio;
    }
}
```

**Rules:**
- `@ManyToOne`: always `fetch = FetchType.LAZY`
- Duplicate prevention: `@UniqueConstraint`
- Use `@Builder` or a direct constructor (prefer direct constructor when fields are few)
- When using `@Builder`, also provide `@NoArgsConstructor` + full-args constructor or a complete-field constructor

---

## 6. Location-Based Comment Pattern

```java
@Entity
@Getter
@NoArgsConstructor
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 1000)
    private String content;

    // Nullable — allow comments without a location
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(nullable = false)
    private boolean resolved = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void resolve() {
        this.resolved = true;
    }
}
```

**Coordinate validation (in service):**
```java
private void validateCoordinates(Double latitude, Double longitude) {
    if (latitude == null && longitude == null) return; // location-less comment is allowed
    if (latitude == null || longitude == null) {
        throw new IllegalArgumentException("Both latitude and longitude are required.");
    }
    if (latitude < -90 || latitude > 90) {
        throw new IllegalArgumentException("Latitude must be between -90 and 90.");
    }
    if (longitude < -180 || longitude > 180) {
        throw new IllegalArgumentException("Longitude must be between -180 and 180.");
    }
}
```

---

## 7. My Page Aggregation Pattern

```java
@Transactional(readOnly = true)
public MyPageResponse getMyPage(String email) {
    User user = getUser(email);
    List<Portfolio> myPortfolios = portfolioRepository.findByUserOrderByCreatedAtDesc(user);
    long likeCount = likeRepository.countByUser(user);
    long commentCount = commentRepository.countByUser(user);

    return MyPageResponse.of(user, myPortfolios, likeCount, commentCount);
}
```

---

## 8. Compilation Check

After implementation, always verify compilation:
```bash
./gradlew compileJava
```
Fix any errors and re-verify. Never hand off code in a non-compilable state.
