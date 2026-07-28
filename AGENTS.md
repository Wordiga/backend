# Wordiga Agent Guide

## Project

- Java 25, Spring Boot 4.0.6, Gradle, PostgreSQL 17.
- Keep the existing `controller -> service -> repository/domain` structure.
- Use the Gradle Wrapper (`./gradlew`) for build and verification commands.

## API Design Conventions

- Use the functional specification, the 2025-07-16 decisions, `v1 fix.svg`, and `v2 - in progress.svg` as product context. When they conflict, follow the latest discussion and then `v2 - in progress.svg`.
- Limit tourism content to Chungcheongnam-do.
- Use `lowerCamelCase` for JSON request and response DTO fields.
- Do not pass through every Korea Tourism Organization response field. Define API-specific DTOs containing the fields required by the UI and AI schedule generation.
- Use `yyyy-MM-dd` for dates, `HH:mm` for times, and ISO 8601 for date-times.
- Unless an endpoint specifies otherwise, sort list responses by `createdAt DESC, id DESC`.
- Keep consumer API specifications limited to API contracts.
- Track implementation status and remaining work in `docs/project-checklist.md`.
- Report API exclusion candidates, integration decisions, design rationale, source precedence, and other one-off findings to the user in chat. Do not commit them to project documents unless the user explicitly requests it.

## Working Principles

- Follow Ponytail's ladder: avoid unnecessary work, reuse existing code, prefer Java/Spring/platform features, then installed dependencies, and add only the minimum implementation required.
- Read and trace the affected flow before choosing the smallest solution.
- Do not add abstractions, dependencies, configuration, or behavior outside the requested scope.
- Never simplify away security, trust-boundary validation, error handling that prevents data loss, or data integrity.
- Preserve unrelated user changes. Do not read, modify, overwrite, or commit `.env`.

## Project Constraints

- Add constraints only after a concrete recurring problem is observed.
- Record the problem the constraint prevents and how compliance can be verified.
- Start with guidance here; add an automated check, Codex rule or hook, or CI gate only when guidance is insufficient.

## Verification

- Run the narrowest relevant test while iterating.
- Before handing off a code change, run `./gradlew test` unless the user explicitly limits verification or an environmental blocker is reported.

## Commit Messages

- Write the subject in the format `<type>: <concise Korean summary>`.
- Use an appropriate type such as `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, or `build`.
- Add a blank line after the subject and describe the concrete changes in a detailed bullet list.
- Keep each bullet focused on one implementation, correction, test, schema, or configuration change.
- Mention relevant migrations or SQL files explicitly.
- Do not use a subject-only commit when the change contains multiple meaningful details.

Example:

```text
test: 즉시/예약 펀딩/주문 분기 처리 테스트 추가 및 단위 테스트 보완

- Adaptor → Adapter 오타 수정
- Reserved 생성 테스트 추가
- Orchestrator 분기 처리 테스트 보완
- handle*Success/Failure 정상/예외 테스트 보완
- Order 상태 전이 테스트 보완
- V3(funding.type) sql 추가
```
