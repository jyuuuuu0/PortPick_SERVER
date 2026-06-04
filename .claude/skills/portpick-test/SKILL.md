# portpick-test: 테스트 작성 오케스트레이터

## 트리거 조건
- "테스트 코드 작성해줘 / 추가해줘 / 짜줘"
- "테스트도 해줘"
- portpick-backend 완료 후 테스트 요청

## 워크플로우

### Phase 1 — 대상 분석
1. 테스트할 파일 목록 확인 (Controller, Service)
2. `src/test/java` 기존 테스트 파일 유무 확인
3. 테스트 전략 결정:
   - Service → Mockito 단위 테스트
   - Controller → `@WebMvcTest` 슬라이스 테스트
   - Repository → `@DataJpaTest` (쿼리 메서드가 복잡할 때만)

### Phase 2 — 테스트 작성 (test-writer 에이전트)
`spring-boot-test` 스킬 패턴을 참조해 테스트 파일 작성.

**필수 테스트 케이스:**
- Happy path (정상 케이스)
- 인증 없는 요청 → 401
- 타 사용자 리소스 접근 → 403
- 존재하지 않는 리소스 → 404
- 유효하지 않은 요청값 → 400 (있는 경우)

### Phase 3 — 커밋
- 타입: `test`
- 단위: 테스트 파일 하나당 커밋 하나
- 메시지는 한국어
- 예시:
  ```
  test: PortfolioService 단위 테스트 추가
  test: PortfolioController 슬라이스 테스트 추가
  ```

## 주의사항
- 실제 DB 접근 금지 → H2 인메모리 또는 Mockito 사용
- JWT 쿠키 인증 테스트 방법은 `spring-boot-test` 스킬 참조
- 테스트 파일명: `{대상클래스명}Test.java`
- 테스트 메서드명: `given_when_then` 또는 한국어 설명 (`@DisplayName` 사용)
