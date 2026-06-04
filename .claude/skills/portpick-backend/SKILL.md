---
name: portpick-backend
description: >
  Orchestrator for the full PortPick backend development cycle — design, implementation, security review, and QA validation.
  Trigger this skill for any PortPick backend feature request: likes, comments, replies, mypage, portfolio filtering, etc.
  Also triggers on: "add API", "add endpoint", "implement feature", "backend development",
  "re-run", "redo", "update", "fix", "improve previous result".
---

# PortPick Backend Orchestrator

**Execution mode:** Hybrid
- Phase 1–2: Sequential sub-agents (design → implement)
- Phase 3: Parallel sub-agents (security review + QA simultaneously)

---

## Phase 0: Context Check

Check whether `_workspace/` exists:

- **Does not exist** → **Initial run**: start from Phase 1
- **Exists + new feature requested** → **New run**: rename `_workspace/` to `_workspace_prev/`, then start from Phase 1
- **Exists + partial fix requested** → **Partial re-run**: re-run only the relevant phase

---

## Phase 1: API Design

Spawn the api-architect agent to design the feature.

```
Agent(
  subagent_type: "general-purpose",
  model: "opus",
  prompt: """
    Read both files first:
    1. .claude/agents/api-architect.md  — role and output format
    2. .claude/skills/api-design/SKILL.md  — design guidelines

    Feature requirement: [pass the user's request verbatim]

    Analyze the requirement, design the API, and save the result to _workspace/01_api_design.md.
  """
)
```

Completion condition: `_workspace/01_api_design.md` exists

---

## Phase 2: Feature Implementation

After Phase 1 completes, spawn the feature-developer agent.

```
Agent(
  subagent_type: "general-purpose",
  model: "opus",
  prompt: """
    Read both files first:
    1. .claude/agents/feature-developer.md  — role and implementation principles
    2. .claude/skills/spring-boot-feature/SKILL.md  — coding patterns

    Design document: _workspace/01_api_design.md (read this before writing any code)

    Implement the Spring Boot code layer by layer following the design document.
    Save a summary of created files and the compile result to _workspace/02_feature_summary.md.
  """
)
```

Completion condition: `_workspace/02_feature_summary.md` exists

---

## Phase 3: Parallel Validation

After Phase 2 completes, spawn both agents simultaneously.

**Security Review (background):**
```
Agent(
  subagent_type: "general-purpose",
  model: "opus",
  run_in_background: true,
  prompt: """
    Read both files first:
    1. .claude/agents/security-reviewer.md
    2. .claude/skills/spring-security-review/SKILL.md

    Review the files listed in _workspace/02_feature_summary.md from a security perspective.
    Save the result to _workspace/03_security_review.md.
    If any BLOCKER is found, fix the code directly and include the fix in the report.
  """
)
```

**QA Validation (background):**
```
Agent(
  subagent_type: "general-purpose",
  model: "opus",
  run_in_background: true,
  prompt: """
    Read both files first:
    1. .claude/agents/qa-validator.md
    2. .claude/skills/portpick-qa/SKILL.md

    Cross-compare _workspace/01_api_design.md and _workspace/02_feature_summary.md
    to validate boundary consistency between design and implementation.
    Save the result to _workspace/04_qa_report.md.
  """
)
```

Completion condition: `_workspace/03_security_review.md` + `_workspace/04_qa_report.md` exist

---

## Phase 4: Summary

Read all `_workspace` files and report to the user:

1. **Implemented features**: list of created/modified files
2. **Security review**: BLOCKER fixes applied + INFO recommendations
3. **QA validation**: FAIL mismatches (if any → re-invoke feature-developer)
4. **Follow-up actions**: if QA FAILs exist, pass them to feature-developer for fixes

If QA FAILs are found:
```
Agent(
  subagent_type: "general-purpose",
  model: "opus",
  prompt: """
    Read .claude/agents/feature-developer.md.
    Fix all FAIL items listed in _workspace/04_qa_report.md.
    Update _workspace/02_feature_summary.md after fixing.
  """
)
```

---

## Error Handling

| Situation | Response |
|-----------|----------|
| Phase 1 fails (cannot design) | Ask the user to clarify requirements |
| Phase 2 compile failure | Re-spawn feature-developer with the error details |
| One Phase 3 agent fails | Continue with the other's result; mark the failure clearly |
| QA FAILs found | Re-spawn feature-developer with the FAIL list |

---

## Test Scenarios

**Happy path:** "Add a like feature"
→ Phase 0: no `_workspace/` → initial run
→ Phase 1: api-architect designs Like entity + endpoints
→ Phase 2: feature-developer implements Like.java, LikeRepository, LikeService, LikeController
→ Phase 3: security-reviewer + qa-validator run in parallel
→ Phase 4: summary report

**Error path:** Phase 2 compile failure
→ Pass the error to feature-developer
→ Re-implement and re-verify compilation
→ Proceed to Phase 3 on success
