---
name: aiagentos-lesson-dev
description: "Use when implementing a numbered AIagentos lesson from doc/class, especially chapters 16-31; read the lesson PDF, adapt its design to this repository, follow spec-plan-task-implement gates, and verify the chapter deliverables."
argument-hint: "Lesson number, for example: 16"
user-invocable: true
---

# AIagentos Lesson Development

## Input

`$ARGUMENTS` is the lesson number. The primary course source is `doc/class/第{N}节*.pdf`.
Use this repository's actual project identity and conventions: `aiagentos`, Java version and
dependency versions from the root `pom.xml`, modules and persistence patterns from the module
POMs, README, and existing source. Do not carry OryxOS branding, module names, or `.oryxos`
paths into new AIagentos code. The repository currently uses SQLite and MyBatis; do not
introduce JPA or another persistence framework unless the project itself has deliberately
migrated.

## Non-negotiable rules

- Machine-verifiable gates are mandatory: tests, build, static checks, and deliverable checks
  must pass before claiming completion.
- Read the lesson PDF and relevant existing code before implementation. If the PDF is not
  text-searchable, extract its text or use OCR; never treat binary PDF bytes as lesson content.
- Use course design for lesson requirements and existing AIagentos code/configuration for
  project architecture. If they conflict on a project-wide contract, stop and report the
  conflict rather than inventing a new contract.
- Keep public types, configuration keys, database columns, and external dependencies within
  the lesson deliverables. Verify third-party APIs and exact dependency coordinates against
  the project's BOM before adding dependencies.
- Do not hide exceptions, delete assertions, disable tests, or loosen thresholds to get a
  green build. Failed calls must be audited where the lesson requires audit, then rethrown.
- Never place credentials in code or files. Use environment-variable placeholders.
- Do not commit, push, switch branches, or run packaging scripts. Report Git status if this is
  a Git worktree; if not, continue without fabricating Git evidence.

## Lesson routing

| Lesson | Type | Handling |
|---|---|---|
| 16, 17, 18, 19, 20, 22, 24, 25, 26, 29, 30 | Code lesson | Run the full workflow below |
| 21, 23 | Review lesson | Do not implement code; explain that the lesson is review/spec material for 22/24 |
| 27, 28 | Integration lesson | Follow the PDF reconciliation checklist and add its named integration tests |
| 31 | Demo lesson | Follow the PDF demo/release checklist, not the ordinary feature workflow |
| Other | Unsupported | Report that only lessons 16-31 are supported |

If no matching PDF exists, do not infer the lesson from a filename or another lesson. Report
the missing source and stop.

## Workflow

### 0. Validate scope and collect evidence

1. Parse the lesson number and select the exact `doc/class/第{N}节*.pdf`.
2. Read the complete lesson, especially its requirements, "本节交付物", acceptance harness,
   and manual verification sections.
3. Read the root `README.md`, root and affected module POMs, relevant source/tests, and schema
   files. Read project architecture documents if present; do not assume
   `docs/TechnicalSolution.md` exists.
4. Verify prerequisite lesson deliverables exist. If a required earlier lesson is missing,
   stop and identify it rather than creating substitute APIs.
5. Inspect Git status when available. Preserve unrelated changes and do not assume a branch
   exists.

### 1. Specify and clarify

Create a concise, testable WHAT/WHY feature specification from the lesson. Include user
scenarios, requirements, non-goals, assumptions, exact deliverables, and course-provided
acceptance criteria. Keep the project name and module names AIagentos-specific. Record
project/course conflicts as explicit blockers instead of silently choosing one.

Resolve questions using the PDF and repository evidence. For an unresolved project contract
or missing required API, stop and report the exact decision needed; do not fill gaps with
invented behavior. If a `speckit-specify` or `speckit-clarify` skill is available, use it.

### 2. Plan

Produce a plan naming module locations, dependency direction, verified library coordinates,
tests, config/schema changes, and validation commands. Use the root POM's Java version and
managed versions. Keep secrets externalized, schema changes manual and explicit, and use
existing data access patterns. If a `speckit-plan` skill is available, use it.

### 3. Break down tasks and reconcile deliverables

Make a task list that maps every course deliverable to a code/config/schema/test task. Test
tasks must precede or accompany the related implementation. Identify additions not justified
by the course or existing project; remove them or report a blocker. In an interactive run,
present the reconciliation and wait for confirmation. In an explicitly requested
auto-execution run, continue with the minimal course-compliant scope and report that
assumption. If a `speckit-tasks` skill is available, use it.

### 4. Implement test-first

1. Verify required library artifacts and APIs against the current dependency management
   before adding them.
2. Add the course's key regression tests first and preserve assertion intent. A test must
   not be deleted, disabled, or weakened to make the build pass.
3. Implement only the named deliverables in their AIagentos modules. Before a write, verify
   third-party types and methods in resolved project dependencies.
4. Preserve public names and literal values specified in the course unless the repository has
   a conflicting established contract; report that conflict before changing an existing
   contract.
5. Do not add undocumented abstraction layers. Exceptions must be surfaced; required audit
   records must be written before the original failure is rethrown.
6. Keep test method identifiers in English; use `@DisplayName` for source-language case
   labels when useful.
7. Run focused module tests after each coherent implementation, then project-wide validation.
   If `speckit-analyze` / `speckit-implement` skills are available, use them.

### 5. Chapter acceptance

Report evidence for:

1. Build and test results, including any checks skipped only because of a documented
   environment limitation.
2. Presence and non-emptiness of each course-required test class and key regression case.
3. Presence of each code, config, and schema deliverable.
4. Regression tests for prerequisite lessons.
5. Course-specific invariants and manual validation still required.
6. A concise reviewer guide: files changed and why, highest-risk review points, and copyable
   verification commands.

Do not claim the chapter is complete when a hard gate fails. Do not commit or push.

## Lesson and architecture map

The lesson PDF is the source for lesson-specific behavior. Use this map only to find likely
AIagentos module boundaries; existing code and POMs take precedence.

| Lesson | Technical area | Typical AIagentos placement |
|---|---|---|
| 16 | Provider, Profile, LLM audit | Profile contracts in `aiagentos-core`; provider orchestration in `aiagentos-provider`; audit persistence in `aiagentos-storage` |
| 17 | Agent loop, prompt, tools, context | Agent/loop contracts in `aiagentos-core`; tool audit in `aiagentos-storage` |
| 18 | CLI and sessions | Commands in `aiagentos-cli`; CLI channel in `aiagentos-channel-cli`; session contract in `aiagentos-core`; persistence in `aiagentos-storage` |
| 19 | Notifications | Adapters and notification tools in `aiagentos-tool` |
| 20 | Tool and MCP | Shared tool contracts in `aiagentos-core`; registry, built-ins, and MCP in `aiagentos-tool` |
| 22 | Memory | `aiagentos-memory` |
| 24 | Sandbox | Sandbox package in `aiagentos-tool` |
| 25 | Scheduling and session identity | Scheduler contracts in `aiagentos-core` |
| 26 | Web service | Controllers and static admin in `aiagentos-web` |
| 29 | Skill/Profile runtime registration | Existing runtime classes in `aiagentos-core`; examples in `.aiagentos/` |
| 30 | Agent lifecycle and API | Lifecycle service in `aiagentos-core`; API controller in `aiagentos-web` |
| 27, 28, 31 | Integration/demo | Follow the exact PDF reconciliation or demo checklist; do not open a new ordinary feature |

## Required planning details for code lessons

When specifying a code lesson, derive WHAT/WHY from the PDF, not implementation class names.
Include:

- User scenarios and individually testable functional requirements.
- Explicit non-goals copied from the course.
- Acceptance tests and the manual-only checks.
- Earlier lesson dependencies and assumptions.

The plan must name each course deliverable and the module that owns it. Use a test strategy
that separates network-free unit tests from real integration smoke tests. Integration tests
must be tagged and opt-in so default CI never depends on an external API or credential.

## Cross-lesson invariants

Apply only the invariants relevant to the current lesson, and use existing AIagentos
implementations where available:

1. External I/O must use the existing sandbox enforcement when that capability exists. If
   the lesson precedes sandbox support, record the deferred integration point; do not invent
   a sandbox implementation early.
2. LLM and tool success/failure paths must use their established audit repositories when
   those contracts exist.
3. Search source and configuration for credential literals; only environment-variable
   placeholders are allowed.
4. Session IDs are managed by the established session component; do not create IDs in other
   modules.
5. Do not introduce Reactor, `CompletableFuture`, or custom thread pools unless the course
   and project explicitly require them.
6. Provider code must not execute tools automatically; execution belongs to the agent/tool
   loop.

## AIagentos module map

Use the closest existing project module and verify dependency direction in its POM. Typical
placements are:

| Concern | AIagentos module |
|---|---|
| Shared domain contracts and profile primitives | `aiagentos-core` |
| LLM provider integrations and request adapters | `aiagentos-provider` |
| SQLite schema and MyBatis persistence | `aiagentos-storage` |
| Tool implementations | `aiagentos-tool` |
| HTTP controllers and web conventions | `aiagentos-web` |
| Application wiring and runtime configuration | `aiagentos-boot` |
| Command-line interfaces | `aiagentos-cli`, `aiagentos-channel-cli` |
| Memory capabilities | `aiagentos-memory` |

## Chapter 16 checklist

For the Agent Provider lesson, preserve these course requirements while adapting names and
implementation to AIagentos:

- Profile model and YAML loading/indexing, including the provider name, model, temperature,
  and the remaining fields explicitly listed by the lesson.
- Provider connection configuration separated from per-Profile model parameters.
- Explicit `provider name -> ChatModel` routing; unknown names fail clearly.
- A single-call provider service accepting session ID, Profile, and prompt. The provider
  translates available tool schemas but never executes tools; framework auto-execution is off.
- `llm_calls` records successful and failed calls, including provider/model, usage, success,
  error message, and elapsed time as specified by the course. Use the repository's SQLite
  schema and MyBatis pattern, not JPA auto-DDL.
- Required test coverage: Profile loading, provider routing/audit/auto-execution, tool-schema
  translation, repository persistence against the hand-maintained SQL schema, and an opt-in
  real-provider smoke test tagged `integration`.
- No fallback, retries, hedged requests, circuit breaker, or cost dashboard unless the lesson
  explicitly adds them.

Keep `.aiagentos/` as the project-specific Profile configuration directory. Use environment
variables for API keys and do not require a real key for default unit tests.
