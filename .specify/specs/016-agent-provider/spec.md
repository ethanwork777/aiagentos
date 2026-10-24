# 第 16 节：AIagentos Agent Provider

## Goal

在现有 AIagentos 多模块工程中建立一次 LLM 调用的统一入口，按 Profile 选择显式注册的
Spring AI `ChatModel`，转发工具 schema 但不执行工具，并为成功和失败调用写入 SQLite 审计。

## Requirements

1. YAML Profile 支持课程列出的字段：name、description、identity、provider
   (name/model/temperature)、tools、skills、mcp_servers、channels、
   notify_channels、schedules、bootstrap、settings。
2. ProfileLoader 从 `.aiagentos/profiles/` 安全解析 YAML，并校验 provider 名存在；
   单个坏文件记录错误并跳过，不阻止其他 Profile 加载。
3. ProfileRegistry 可按 Profile 名称查找。
4. ProviderService 接受 session ID、Profile、Prompt，根据 provider 名显式选择 ChatModel；
   未知 provider 清晰失败。
5. Provider 将工具描述转换为 Spring AI schema、关闭框架自动执行、只调用一次，并原样
   返回模型响应。
6. 每次模型调用成功或失败均记录 provider、model、token 用量、耗时和状态；
   失败审计后重新抛出原始调用异常。
7. SQLite schema 手工维护，存储遵循 AIagentos 现有 MyBatis 模式；不启用 ORM 自动建表。
8. 默认单测使用 mock，不需要网络或真实密钥；真实模型冒烟测试为显式 opt-in。

## Out of scope

Fallback/retry/hedged requests/circuit breaker、成本看板、ReAct loop、工具执行和真实
模型凭证。课程中的 OryxOS/JPA 名称分别按 AIagentos 项目命名和现有 MyBatis 架构适配。

## Acceptance

- ProfileLoader、ProviderService、ToolSchemaAdapter、LlmCallRepository 测试覆盖课程关键
  回归点。
- `llm_calls` 手工 schema 包含成功/失败状态与错误信息；测试在真实 SQLite schema 上存取。
- 默认 `mvn test` 不依赖外部模型；`mvn verify` 完成可用的静态门禁和构建。
