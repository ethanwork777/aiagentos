# aiagentos

aiagentos is a Java 21/ Spring Boot 3 modular foundation for an AI agent platform.

## Requirements

- JDK 21(LTS)
- Maven 3.9+
- Optional: `pre-commit` to install the local Git hooks

## Build and run

```sh
mvn clean verify
mvn -pl aiagentos-boot -am package
java -jar aiagentos-boot/target/aiagentos-boot-0.1.0-SNAPSHOT.jar
```

The service listens on port `8080` by default. Override it with `SERVER_PORT`.

- Health: `/actuator/health`
- Prometheus: `/actuator/prometheus`
- OpenAPI UI: `/swagger-ui.html`
- OpenAPI document: `/v3/api-docs`

SQLite stores its local database in `aiagentos.db`. The initial schema reserves
`tool_invocations` and `llm_calls` for audit records.

## Agent providers

Profiles are loaded from `.aiagentos/profiles/` by default. Override the directory with
`AIAGENTOS_PROFILE_DIR`. List provider names under `aiagentos.providers`; each name must
match a Spring `ChatModel` bean name. Add the provider-specific Spring AI starter and
configure its credentials with environment variables in the consuming deployment. No
vendor-specific model starter or real credentials are enabled by default.

A profile selects the configured provider and its model:

```yaml
name: assistant
provider:
  name: local-model
  model: example-model
  temperature: 0.2
```

Run the default, network-free unit tests with `mvn test`. The real-provider smoke test is
opt-in with `-Daiagentos.smoke.enabled=true`; it requires a registered ChatModel bean and a
profile named `provider-smoke` in the configured profile directory.

## Modules

- `aiagentos-core`: shared domain primitives
- `aiagentos-provider`: model-provider integrations
- `aiagentos-memory`: memory capabilities
- `aiagentos-tool`: tool execution
- `aiagentos-web`: HTTP API conventions and cross-cutting web behavior
- `aiagentos-storage`: SQLite and MyBatis persistence
- `aiagentos-boot`: Spring Boot application and executable JAR
- `aiagentos-cli`: command-line foundation
- `aiagentos-channel-cli`: channel-specific CLI foundation

Business capabilities are intentionally left as module foundations.

## Quality and security checks

`mvn verify` runs Google Java formatting checks, Checkstyle, Alibaba P3C/PMD,
SpotBugs with Find Security Bugs, and OWASP Dependency-Check. Set `NVD_API_KEY`
in the environment to authenticate Dependency-Check against the NVD API and
avoid unauthenticated rate limits. CI accepts this as the `NVD_API_KEY` secret.

Install local hooks with `pre-commit install`.
