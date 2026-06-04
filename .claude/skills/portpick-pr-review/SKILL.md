# portpick-pr-review: PR 코멘트 반영 자동화

## 트리거 조건
- "PR 코멘트 반영해줘"
- "코멘트 처리해줘"
- "리뷰 반영해줘"
- PR 생성 후 코멘트 확인 요청

---

## 워크플로우

### Step 1 — PR 번호 확인
```bash
# 현재 브랜치의 PR 번호 자동 감지
gh pr list --head $(git branch --show-current) --json number --jq '.[0].number'
```
감지 실패 시 사용자에게 PR 번호 질문.

### Step 2 — 코멘트 수집
```bash
# 리뷰 인라인 코멘트 (코드 줄에 달린 것)
gh api repos/{owner}/{repo}/pulls/{pr_number}/comments \
  --jq '[.[] | {id, body, path, line, user: .user.login, in_reply_to_id}]'

# 일반 코멘트 (Conversation 탭)
gh api repos/{owner}/{repo}/issues/{pr_number}/comments \
  --jq '[.[] | {id, body, user: .user.login}]'
```

repo 정보 가져오기:
```bash
gh repo view --json owner,name --jq '"\(.owner.login)/\(.name)"'
```

### Step 3 — 코멘트별 판단

각 코멘트에 대해 다음 기준으로 **반영 / 불반영** 판단:

**반영 기준:**
- 버그, 누락된 예외 처리, 잘못된 로직 지적
- 보안 취약점 (인증 누락, 소유권 검증 누락 등)
- 코드 컨벤션 / 네이밍 문제
- 성능 이슈 (N+1, 불필요한 쿼리 등)

**불반영 기준:**
- 개인 취향 수준의 스타일 차이 (이미 일관성 있는 코드인 경우)
- 프로젝트 아키텍처 방향과 맞지 않는 제안
- 이미 다른 방식으로 처리된 사항
- 현재 스코프를 벗어난 제안 (별도 이슈로 처리가 맞는 것)
- 이미 구현된 내용을 모르고 지적한 경우

### Step 4 — 반영 처리
변경 후 커밋:
```
fix: PR 코멘트 반영 — {변경 내용 한 줄 요약}
```

### Step 5 — 답글 작성

**반영한 경우:**
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/comments/{comment_id}/replies \
  --method POST \
  -f body="반영 완료 ✅
커밋: \`{commit_hash}\` — {변경 내용 한 줄 요약}"
```

**불반영한 경우:**
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/comments/{comment_id}/replies \
  --method POST \
  -f body="반영 안 함 ❌
이유: {구체적인 이유}"
```

일반 코멘트(Conversation 탭)에 달린 경우:
```bash
gh api repos/{owner}/{repo}/issues/{pr_number}/comments \
  --method POST \
  -f body="@{user} 반영 완료 ✅ 커밋: \`{hash}\` — {요약}"
```

---

## 처리 결과 요약 출력 형식

모든 코멘트 처리 완료 후:

```
## PR #{number} 코멘트 처리 결과

| # | 작성자 | 코멘트 요약 | 판단 | 조치 |
|---|--------|------------|------|------|
| 1 | @user  | ...        | 반영 | 커밋 abc1234 |
| 2 | @user  | ...        | 불반영 | 이유: ... |
```

---

## 주의사항
- 이미 답글이 달린 코멘트는 건너뜀 (중복 처리 방지)
- 본인(Claude)이 작성한 코멘트는 처리 대상 제외
- 코드 변경 없이 답글만 달리는 경우 커밋 생략
