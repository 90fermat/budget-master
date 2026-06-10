\# AGENTS.md — Subagent Definitions



\## ui-agent

Specialization: Compose Multiplatform UI screens and components

Rules:

\- Always preview with @PreviewLightDark

\- Use adaptive layouts (WindowSizeClass)

\- Material3 components only

\- Extract every reusable component to /components/



\## data-agent  

Specialization: Repository pattern, SQLDelight, Ktor

Rules:

\- Map ALL DTOs to domain entities via Mapper classes

\- Handle ALL network errors with sealed Result<T, AppError>

\- SQLDelight queries must have corresponding tests



\## test-agent

Specialization: Unit tests, UI tests, screenshot tests

Rules:

\- Write tests BEFORE marking a feature complete

\- Coverage minimum: 80% for domain layer

\- Every new screen → Paparazzi test



\## review-agent

Specialization: Code review and architecture compliance

Rules:

\- Check module boundary violations

\- Verify no Android imports in shared modules

\- Validate KDoc coverage

