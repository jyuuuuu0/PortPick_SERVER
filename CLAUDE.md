## Harness: PortPick Backend

**Goal:** Coordinate an agent team to design, implement, and validate PortPick REST APIs (likes, comments, filtering, mypage, etc.)

**Skill Triggers:**
- `portpick-backend` — 백엔드 기능 추가/수정/구현 요청
- `portpick-test` — 테스트 코드 작성 요청
- `portpick-pr-review` — PR 코멘트 반영/답글 요청

Simple code questions can be answered directly.

---

## Commit Convention

모든 커밋은 아래 타입 + **한국어** 설명으로 작성. 작은 기능 단위로 커밋.

| Type | 설명 |
|------|------|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| refactor | 기능 변경 없는 코드 리팩토링 |
| style | 코드 스타일 변경 (공백, 세미콜론 등) |
| design | CSS/UI 수정 |
| docs | 문서 수정 |
| test | 테스트 코드 추가/수정 |
| chore | 빌드, 설정 파일 수정 |

형식: `type: 한국어 설명`
예시: `feat: 포트폴리오 좋아요 API 추가`, `test: PortfolioService 단위 테스트 추가`

---

## PR Convention

- 템플릿: `.github/pull_request_template.md` 기반으로 **한국어** 작성
- 생성 명령: `gh pr create --base main --title "..." --body "$(cat <<'EOF' ... EOF)"`
- 제목은 커밋 컨벤션과 동일한 형식 사용

---

**Changelog:**
| Date | Change | Target | Reason |
|------|--------|--------|--------|
| 2026-05-30 | Initial setup | All | - |
| 2026-06-04 | portpick-test 하네스 추가 | Test automation | 테스트 자동화 |
| 2026-06-04 | portpick-pr-review 스킬 추가 | PR workflow | 코멘트 반영 자동화 |
| 2026-06-04 | Commit/PR convention 추가 | All | 협업 표준화 |
