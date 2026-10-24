# 第 16 节任务清单

- [x] Add shared Profile, provider, prompt, tool-schema, and LLM-audit domain types.
- [x] Implement safe Profile YAML loading, environment placeholders, provider validation, and
      registry lookup; add ProfileLoaderTest.
- [x] Add managed Spring AI chat API dependency and implement schema adapter; add
      ToolSchemaAdapterTest.
- [x] Implement ProviderService routing, single-call behavior, usage collection, and success/
      failure audit; add ProviderServiceTest.
- [x] Update SQLite `llm_calls` schema and add MyBatis audit mapper/repository;
      add LlmCallRepositoryTest.
- [x] Wire Profile loading in Boot and provide a tagged, opt-in ProviderSmokeIT.
- [x] Update project usage documentation and run focused tests plus Maven verification.
