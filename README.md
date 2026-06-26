# PortPick SERVER

포트폴리오 공유 플랫폼 **PortPick**의 백엔드 REST API 서버입니다.
구글 소셜 로그인으로 가입한 사용자가 포트폴리오를 등록·공유하고, 임베드 위치 기반으로 피드백 코멘트를 주고받을 수 있습니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.x (Spring Web MVC, Spring Data JPA, Spring Security) |
| Auth | OAuth2 Client (Google), JWT (jjwt 0.12.5) |
| Database | MySQL |
| Cache / Session | Redis |
| Build | Gradle |

---

## 주요 기능

- **소셜 로그인** — 구글 OAuth2 인증 후 JWT 발급, 쿠키 기반 인증 유지
- **회원가입 / 프로필** — 추가 정보 입력으로 가입 완료, 프로필 이미지 업로드
- **포트폴리오** — 등록/수정/삭제/상세 조회, 직무·경력 기반 필터링, 첨부 파일 또는 임베드 링크 지원
- **좋아요** — 포트폴리오 좋아요 등록/취소/카운트 (동시 요청 시 정합성 처리)
- **코멘트 피드백** — 포트폴리오 임베드 위치(좌표) 기반 코멘트와 답글, 완료(resolve) 처리
- **마이페이지** — 내가 등록/좋아요/코멘트한 포트폴리오 목록 조회

---

## 프로젝트 구조

```
src/main/java/com/example/PortPick_SERVER
├── config       # Security, OAuth2, Web 설정
├── controller   # REST 엔드포인트
├── dto          # 요청/응답 DTO
├── filter       # JWT 인증 필터
├── handler      # OAuth2 성공/실패 핸들러, 예외 핸들러
├── jwt          # JWT 발급/검증
├── model        # JPA 엔티티 (User, Portfolio, PortfolioLike, Comment, Reply, JobRole, CareerType, CareerRange)
├── repository   # Spring Data JPA 리포지토리
└── service      # 비즈니스 로직 (FileStorageService 등)
```

---

## 커밋 컨벤션

`type: 한국어 설명` 형식을 사용하며, 작은 기능 단위로 커밋합니다.

| Type | 설명 |
|------|------|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| refactor | 기능 변경 없는 코드 리팩토링 |
| style | 코드 스타일 변경 |
| design | CSS/UI 수정 |
| docs | 문서 수정 |
| test | 테스트 코드 추가/수정 |
| chore | 빌드, 설정 파일 수정 |
