---
name: aiagentos-init
description: >-
  初始化 AIagentOS（或同类 JDK 21 + Spring Boot 3.x 企业级应用）的工程地基：Maven 多模块骨架、
  Redis + Redisson、Spring Cloud + Nacos 服务注册、OpenFeign 远程调用及熔断限流、
  结构化日志、Actuator + Prometheus 监控、Spring MVC + 虚拟线程、springdoc OpenAPI、
  统一响应体与全局异常/错误码、Google 格式 + 阿里编码规约、安全检查，以及 CI 与 pre-commit。
  当用户要「初始化项目 / 搭工程骨架 / 起脚手架 / 配置 Redis、Redisson、Spring Cloud、Nacos、OpenFeign /
  加日志监控 / 加开发规范 / 加代码安全检查」时使用。
---

# AIagentOS 项目初始化 Skill

把"工程地基"一次性、标准化地装好——业务逻辑（五大核心能力）不在本 skill 范围内。

## 什么时候用

- 新建 AIagentOS 仓库、或给空仓库起工程骨架时
- 要给项目补齐日志 / 监控 / API 规范 / 开发规范 / 安全检查时
- 任何 JDK 21 + Spring Boot 3.x 的企业级应用，想一次到位地装好工程地基时

## 不做什么（边界）

- 不实现五大核心能力（Provider / ReAct / Memory / Tool / Web）——那是业务模块，走 Spec-Kit 的 user story 拆解
- 不硬编码任何密钥 / token / API key——一律用环境变量占位（`${ENV_VAR}`）
- 不替换已存在的业务代码；只新增基础设施与配置

## 前置约定（来自 AIagentOS constitution）

实施前确认这些硬约束，写进配置：
- JDK 21、Spring Boot 3.x、Maven 多模块；默认可由 boot 模块打包为 fat JAR
- HTTP 层用 Spring MVC + Java 21 虚拟线程（不引入响应式）
- Redis 作为缓存，使用 Redisson 作为 Redis 客户端/开发框架；连接信息从环境变量或外部配置读取
- 使用与 Spring Boot 版本兼容的 Spring Cloud Release Train 和 Spring Cloud Alibaba BOM；Nacos 用于服务注册发现
- 微服务间使用 Spring Cloud OpenFeign；所有远程调用配置连接/读取超时、熔断和限流，禁止无保护的无限等待
- OpenFeign 默认连接超时 500ms、读取超时 2000ms；按服务/调用场景可显式覆盖并说明原因
- 持久化 SQLite + mybatis；长期记忆走 MEMORY.md（本 skill 只配数据源，不建业务表）
- 审计表 `tool_invocations` / `llm_calls` 的建表脚本预留位（day one 落库）
- 代码必须过 Google 格式 + 阿里编码规约 + 安全扫描，才能合并

---

## 初始化步骤（按顺序执行，每步完成后 `git commit`）

### 0. 确认参数
向用户确认：`groupId`、根 `artifactId`、模块清单（默认 AIagentOS 9 模块）、端口（默认 8080）、JDK（默认 21），以及 Redis/Nacos 地址、命名空间和认证信息的外部配置来源。不得要求用户把凭证写入仓库。

### 1. Maven 多模块骨架
建父 `pom.xml`（packaging=pom，统一版本管理）+ 9 个子模块：
`aiagentos-core`、`aiagentos-provider`、`aiagentos-memory`、`aiagentos-tool`、`aiagentos-web`、
`aiagentos-storage`、`aiagentos-boot`、`aiagentos-cli`、`aiagentos-channel-cli`。
`aiagentos-boot` 为启动模块（含 `main`），打 fat JAR。

### 2. 基础依赖与版本管理（父 pom `dependencyManagement` / `pluginManagement`）
统一管理 Spring Boot BOM、与之兼容的 Spring Cloud Release Train BOM、兼容的 Spring Cloud Alibaba BOM、
Spring AI Alibaba BOM、Redisson、Redis、Nacos、OpenFeign、Resilience4j、SQLite JDBC、Picocli、
SnakeYAML、logstash-logback-encoder、springdoc 等版本。通过官方兼容矩阵选择一组彼此兼容的稳定版本，
禁止各自独立追最新版本；先锁定并验证依赖树再开发。

### 3. 日志（结构化）
`aiagentos-boot/src/main/resources/logback-spring.xml`：
- 开发环境：彩色 console pattern
- 生产环境（profile=prod）：JSON 输出（`LogstashEncoder`），带 `traceId`（MDC）
- 统一通过 SLF4J 打日志，禁止 `System.out`

### 4. 监控（Actuator + Micrometer + Prometheus）
依赖：`spring-boot-starter-actuator`、`micrometer-registry-prometheus`。
`application.yaml`：
```yaml
management:
  endpoints.web.exposure.include: health,info,prometheus,metrics
  endpoint.health.probes.enabled: true
  metrics.tags.application: aiagentos
```
暴露 `/actuator/health`、`/actuator/info`、`/actuator/prometheus`。

### 5. HTTP Server（Spring MVC + 虚拟线程）
依赖：`spring-boot-starter-web`。`application.yaml`：
```yaml
server.port: 8080
spring.threads.virtual.enabled: true   # JDK 21 虚拟线程
```

### 6. Redis 缓存（Redisson）
- 使用 Redis 作为缓存后端，通过 Redisson 的 Spring Boot starter 与 `RedissonClient` 访问 Redis；缓存业务逻辑应使用统一配置的 Redisson API，不在多个模块重复创建客户端。
- Redis 地址、用户名/密码、TLS 等连接设置通过环境变量或外部化配置注入，不提交明文凭证。
- 集中配置连接池、连接/命令超时、重试策略及缓存 TTL；设置合理的有界等待，避免 Redis 不可用时拖垮服务。
- 按应用需要配置 Spring Cache 与 Redisson 的集成；定义缓存 key、过期时间和失效策略，避免缓存永久驻留或把缓存当作唯一持久化数据。
- Redis 可选健康检查应纳入 Actuator；不得将凭证暴露在健康端点或日志中。

### 7. Spring Cloud、Nacos 与 OpenFeign
- 导入与 Spring Boot 版本匹配的 Spring Cloud BOM 和 Spring Cloud Alibaba BOM；通过 Spring Cloud Alibaba Nacos Discovery 接入 Nacos 服务注册发现。Nacos 地址、namespace、group 和认证凭证均外部化。
- 使用 Spring Cloud OpenFeign 声明服务间 HTTP API。Feign client 按服务边界定义，不把内部实体直接当作跨服务契约；启用必要的服务发现/负载均衡能力。
- 为所有 Feign client 设置默认连接超时 500ms、读取超时 2000ms，并允许有理由的逐客户端覆盖：
```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 500
            readTimeout: 2000
```
- 配置 Spring Cloud CircuitBreaker 的 Resilience4j 实现，并为远程调用设置熔断和限流策略（包括窗口/阈值、等待/刷新周期及并发行为）。确认限流策略实际包裹 Feign 调用，而非只声明依赖或配置未生效的实例。
- 为可恢复的失败定义明确的错误映射和 fallback；fallback 不得返回伪造的成功结果。记录熔断、限流、超时等可观测指标，避免记录敏感请求数据。
- 使用可控测试验证默认超时配置、熔断打开/恢复和限流拒绝行为；不依赖真实 Nacos/Redis 才能通过单元测试。集成验证环境应使用外部服务或 Testcontainers，并将凭证放在环境变量中。

### 8. API 规范（OpenAPI + 统一响应 / 错误码）
- 依赖：`springdoc-openapi-starter-webmvc-ui`（Swagger UI 在 `/swagger-ui.html`，spec 在 `/v3/api-docs`）
- 在 `aiagentos-web` 建：
  - `ApiResponse<T>`：统一响应体（`code` / `message` / `data` / `timestamp`）
  - `GlobalExceptionHandler`（`@RestControllerAdvice`）：异常 → 标准 JSON 错误（`errorCode` / `message` / `timestamp`），覆盖 400 / 404 / 500 / 503
  - REST 约定：资源名词复数、`/api/v1` 前缀、合理 HTTP 状态码

### 9. 开发规范（Google 格式 + 阿里编码规约，两层互补）

职责分开，互不冲突：
- **格式层 — Google**：Spotless + google-java-format，管缩进、import 顺序、空白、换行——`apply` 一键自动修。
- **编码规约层 — 阿里巴巴 Java 开发手册**：通过 **P3C（p3c-pmd ruleset）** 落地，管命名、并发、异常处理、集合、OOP、日志、SQL 等"怎么写才对"的规约。
- **兜底 — Checkstyle**（`google_checks.xml`）+ 根目录 `.editorconfig`。

**格式：Spotless + google-java-format**
```xml
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <version>${spotless.version}</version>
  <configuration>
    <java>
      <googleJavaFormat><style>GOOGLE</style></googleJavaFormat>
      <removeUnusedImports/>
      <importOrder/>
    </java>
  </configuration>
  <executions><execution><goals><goal>check</goal></goals></execution></executions>
</plugin>
```

**编码规约：阿里 P3C（挂在 PMD 上）**
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-pmd-plugin</artifactId>
  <version>${pmd.version}</version>
  <configuration>
    <rulesets>
      <ruleset>rulesets/java/ali-pmd.xml</ruleset>     <!-- 阿里 P3C 规约 -->
      <ruleset>rulesets/java/ali-concurrent.xml</ruleset>
      <ruleset>rulesets/java/ali-exception.xml</ruleset>
    </rulesets>
  </configuration>
  <dependencies>
    <dependency>
      <groupId>com.alibaba.p3c</groupId>
      <artifactId>p3c-pmd</artifactId>
      <version>${p3c.version}</version>
    </dependency>
  </dependencies>
  <executions><execution><goals><goal>check</goal></goals></execution></executions>
</plugin>
```

> 分工原则：**Google 管「长什么样」（格式），阿里管「怎么写才对」（规约）**，两者职责不同、可并存。
> 若个别风格规则冲突，以 google-java-format 为准（因为它能自动修，省争论）。开发者本地可装
> 「阿里巴巴 Java 编码规约」IDEA/VS Code 插件，写代码时即时提示。

### 10. 代码安全检查
父 pom 加四件套：
- **SpotBugs** + **Find Security Bugs**（findsecbugs 插件，覆盖 OWASP Top 10：SQL 注入、XSS、路径穿越、弱加密、XXE、不安全反序列化等）
- **PMD**（源码层规则）
- **OWASP Dependency-Check**（`dependency-check-maven`，扫第三方依赖已知 CVE，设 `failBuildOnCVSS`）

```xml
<plugin>
  <groupId>com.github.spotbugs</groupId>
  <artifactId>spotbugs-maven-plugin</artifactId>
  <version>${spotbugs.version}</version>
  <configuration>
    <effort>Max</effort><threshold>Low</threshold>
    <plugins><plugin>
      <groupId>com.h3xstream.findsecbugs</groupId>
      <artifactId>findsecbugs-plugin</artifactId>
      <version>${findsecbugs.version}</version>
    </plugin></plugins>
  </configuration>
</plugin>
```

### 11. CI + pre-commit
- **pre-commit**（本地）：提交前跑 `mvn spotless:check`（或 `spotless:apply`）+ 快速 SpotBugs
- **CI**（GitHub Actions）：`mvn verify` 串起 spotless:check → checkstyle → spotbugs → pmd → dependency-check；任一不过则红，禁止合并

### 12. 验证
- `mvn clean verify` 全绿
- `mvn -pl aiagentos-boot -am package` 成功，并可用 `java -jar aiagentos-boot/target/aiagentos-boot-0.1.0-SNAPSHOT.jar` 启动
- 访问 `/actuator/health`（UP）、`/actuator/prometheus`（有指标）、`/swagger-ui.html`（能打开）
- 验证 Redisson 能连接测试 Redis，缓存读写及过期策略符合配置；健康检查不泄漏凭证
- 验证服务可按配置向 Nacos 注册/发现；OpenFeign 默认连接/读取超时为 500ms/2000ms
- 验证远程调用的熔断、限流及 fallback；fallback 不得伪装成正常成功响应
- 故意写一行不规范代码 → `spotless:check` 报错；故意引一个有 CVE 的旧依赖 → depcheck 报警

---

## 检查清单（Definition of Done）

- [ ] 9 个 Maven 模块骨架建好，`mvn clean package` 出 fat JAR
- [ ] 结构化日志（prod 为 JSON，含 traceId），无 `System.out`
- [ ] `/actuator/health` `/info` `/prometheus` 可访问
- [ ] 虚拟线程开启（`spring.threads.virtual.enabled=true`）
- [ ] JDK 21；Spring Cloud / Spring Cloud Alibaba / Spring Boot 版本兼容且由 BOM 统一管理
- [ ] Redis 缓存通过 Redisson 接入；连接配置外部化并有明确 TTL/超时策略
- [ ] Nacos 服务注册发现已配置；微服务远程调用使用 OpenFeign
- [ ] OpenFeign 默认连接超时 500ms、读取超时 2000ms；远程调用具备已验证的熔断与限流
- [ ] springdoc：`/swagger-ui.html` 可打开，统一 `ApiResponse` + `GlobalExceptionHandler` 就位
- [ ] Spotless（Google 格式）+ 阿里 P3C（编码规约）+ Checkstyle + `.editorconfig` 全部生效
- [ ] SpotBugs + Find Security Bugs + PMD + OWASP Dependency-Check 接入 `mvn verify`
- [ ] pre-commit + CI 跑通，任一检查失败即阻断
- [ ] 敏感配置全用 `${ENV_VAR}` 占位，无明文密钥

---

## 与 constitution / Spec-Kit 的分工

- **本 skill**：把上面这套工程地基"装上"（一次性、可复用、跨模块）
- **constitution**：把硬约束"钉死"（JDK 21、Spring Cloud 兼容性、Google 规范、必须过安全扫描、Spring AI 只用一半…），让 AI 每次都遵守
- **CI + pre-commit**：把检查"强制执行"（机器把关，不靠人自觉）
- **Spec-Kit user story**：地基起好后，再按五大核心能力逐个开发

> 版本号、插件坐标、`google_checks.xml` 路径等以实施时官方文档为准；本 skill 给的是流程与配置骨架，不锁死具体版本。
