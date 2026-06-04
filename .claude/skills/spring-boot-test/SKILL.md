# spring-boot-test: PortPick Spring Boot 테스트 패턴 가이드

## 프로젝트 테스트 환경

### build.gradle 의존성 (이미 포함됨)
```groovy
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.springframework.security:spring-security-test'
```

### 테스트용 application.yml
`src/test/resources/application.yml` 또는 `@TestPropertySource`로 H2 인메모리 DB 설정:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: test
            client-secret: test
```

---

## 1. Service 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @InjectMocks
    private PortfolioService portfolioService;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private UserRepository userRepository;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .name("테스트유저")
                .build();
    }

    @Test
    @DisplayName("포트폴리오 생성 성공")
    void createPortfolio_success() {
        // given
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(mockUser));
        // when
        // then
    }

    @Test
    @DisplayName("존재하지 않는 포트폴리오 조회 시 예외 발생")
    void getPortfolio_notFound_throwsException() {
        // given
        given(portfolioRepository.findById(999L)).willReturn(Optional.empty());
        // when & then
        assertThatThrownBy(() -> portfolioService.getPortfolio(999L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("본인 소유가 아닌 포트폴리오 삭제 시 예외 발생")
    void deletePortfolio_notOwner_throwsException() {
        // given
        User otherUser = User.builder().id(2L).email("other@test.com").build();
        Portfolio portfolio = Portfolio.builder().id(1L).user(otherUser).build();
        given(portfolioRepository.findById(1L)).willReturn(Optional.of(portfolio));
        // when & then
        assertThatThrownBy(() -> portfolioService.deletePortfolio(1L, mockUser))
                .isInstanceOf(AccessDeniedException.class);
    }
}
```

---

## 2. Controller 슬라이스 테스트

### 기본 설정
```java
@WebMvcTest(PortfolioController.class)
@Import(SecurityTestConfig.class)   // 아래 SecurityTestConfig 참조
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    @Autowired
    private ObjectMapper objectMapper;
}
```

### JWT 쿠키 인증 헬퍼
PortPick은 HttpOnly 쿠키 기반 JWT를 사용하므로, 테스트에서 쿠키를 직접 심어야 한다.

```java
// src/test/java/.../support/JwtTestSupport.java
public class JwtTestSupport {
    public static MockHttpServletRequestBuilder withJwtCookie(
            MockHttpServletRequestBuilder builder, String token) {
        return builder.cookie(new Cookie("token", token));
    }
}
```

### SecurityTestConfig
```java
// src/test/java/.../config/SecurityTestConfig.java
@TestConfiguration
public class SecurityTestConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

### 인증 필요 엔드포인트 테스트 패턴
```java
@Test
@DisplayName("포트폴리오 생성 성공 - 201 반환")
void createPortfolio_success() throws Exception {
    // given
    PortfolioDetailResponse response = PortfolioDetailResponse.builder()...build();
    given(portfolioService.createPortfolio(any(), any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(
            multipart("/api/v1/portfolios")
                .file(new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]))
                .param("title", "테스트 포트폴리오")
                .cookie(new Cookie("token", "valid-token"))
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("테스트 포트폴리오"));
}

@Test
@DisplayName("인증 없는 요청 - 401 반환")
void createPortfolio_unauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/portfolios"))
            .andExpect(status().isUnauthorized());
}
```

---

## 3. Repository 테스트 (@DataJpaTest)

복잡한 커스텀 쿼리가 있을 때만 사용:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL"
})
class PortfolioRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("사용자별 포트폴리오 목록 조회")
    void findByUserId_returnsUserPortfolios() {
        // given
        User user = em.persist(User.builder().email("test@test.com").build());
        Portfolio p = em.persist(Portfolio.builder().user(user).title("포트폴리오1").build());
        em.flush();

        // when
        List<Portfolio> result = portfolioRepository.findByUserId(user.getId());

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("포트폴리오1");
    }
}
```

---

## 4. 공통 테스트 유틸

```java
// 현재 인증 사용자 주입 헬퍼 (Principal 기반 엔드포인트용)
public static Authentication mockAuthentication(String email) {
    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
            email, "", List.of());
    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
}
```

---

## 5. 테스트 파일 위치 규칙
```
src/test/java/com/example/PortPick_SERVER/
├── controller/
│   └── PortfolioControllerTest.java
├── service/
│   └── PortfolioServiceTest.java
├── repository/          ← 필요시만
│   └── PortfolioRepositoryTest.java
├── config/
│   └── SecurityTestConfig.java
└── support/
    └── JwtTestSupport.java
```
