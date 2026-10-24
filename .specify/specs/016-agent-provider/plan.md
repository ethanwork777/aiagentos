# 第 16 节实施计划

## Project decisions

- Use the root Maven `java.version` (currently 25) and Spring Boot 3.x versions.
- Import Spring AI BOM 1.1.2, which is verified resolvable; keep provider selection vendor-neutral
  because the lesson gives example provider names but no target vendor or credentials.
- Place shared contracts/Profile YAML parsing in `aiagentos-core`, provider orchestration and
  Spring AI adapters in `aiagentos-provider`, and MyBatis/SQLite audit persistence in
  `aiagentos-storage`.
- Wire a `Map<String, ChatModel>` explicitly by bean name. Concrete vendor starters and secrets
  remain application-provided; do not invent a vendor integration.

## Steps

1. Add shared Profile, Prompt, tool schema, and audit contracts; test safe YAML parsing, field
   mapping, provider validation, and per-file failure isolation.
2. Add Spring AI chat API dependency and implement schema conversion plus ProviderService.
   Test name-based routing, unknown-provider errors, tool auto-execution disabled, and audit on
   both successful and failed model calls.
3. Implement the audit repository with MyBatis and update the manually maintained SQLite schema.
   Test against a fresh SQLite database created from the shipped schema.
4. Wire Profile loading in the Boot module and document provider bean registration, profile
   format, unit test, and optional real-provider smoke-test procedure.
5. Run focused tests and Maven verification; retain environment limitations explicitly.

## External model check

The current AIagentos BOM manages Spring AI Alibaba components but does not manage the generic
Spring AI chat API. The Spring AI BOM and chat API 1.1.2 were resolved successfully. No vendor
starter is selected because the course does not specify a target provider.
