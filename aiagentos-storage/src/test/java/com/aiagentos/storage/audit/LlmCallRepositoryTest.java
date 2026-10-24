package com.aiagentos.storage.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiagentos.core.audit.LlmCall;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Optional;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;

class LlmCallRepositoryTest {
  @Test
  void repositoryPersistsSuccessAndFailureColumnsUsingManualSchema(@TempDir Path directory)
      throws Exception {
    Path database = directory.resolve("llm-calls.db");
    UnpooledDataSource dataSource =
        new UnpooledDataSource(
            "org.sqlite.JDBC", "jdbc:sqlite:" + database.toAbsolutePath(), null, null);
    try (Connection connection = dataSource.getConnection();
        var schema =
            new InputStreamReader(
                getClass().getResourceAsStream("/schema.sql"), StandardCharsets.UTF_8)) {
      new ScriptRunner(connection).runScript(schema);
    }

    Configuration configuration =
        new Configuration(new Environment("test", new JdbcTransactionFactory(), dataSource));
    configuration.addMapper(LlmCallMapper.class);
    var sessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    try (var session = sessionFactory.openSession(true)) {
      MyBatisLlmCallRepository repository =
          new MyBatisLlmCallRepository(session.getMapper(LlmCallMapper.class));
      LlmCall successfulCall =
          new LlmCall("call-success", "session-1", "deepseek", "chat-v4", 9, 4, true, null, 127);
      LlmCall failedCall =
          new LlmCall(
              "call-failure",
              "session-2",
              "kimi",
              "kimi-latest",
              null,
              null,
              false,
              "connect timeout",
              52);

      repository.save(successfulCall);
      repository.save(failedCall);

      assertThat(repository.findByCallId("call-success")).contains(successfulCall);
      Optional<LlmCall> storedFailure = repository.findByCallId("call-failure");
      assertThat(storedFailure).contains(failedCall);
    }
  }

  @Test
  void migratorAddsAuditColumnsToExistingDatabase(@TempDir Path directory) throws Exception {
    Path database = directory.resolve("existing.db");
    UnpooledDataSource dataSource =
        new UnpooledDataSource(
            "org.sqlite.JDBC", "jdbc:sqlite:" + database.toAbsolutePath(), null, null);
    try (Connection connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.execute(
          """
          CREATE TABLE llm_calls (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            call_id TEXT NOT NULL UNIQUE,
            provider TEXT NOT NULL,
            model TEXT NOT NULL,
            prompt_tokens INTEGER,
            completion_tokens INTEGER,
            status TEXT NOT NULL,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
          )
          """);
      statement.execute(
          "INSERT INTO llm_calls (call_id, provider, model, status) "
              + "VALUES ('legacy-call', 'deepseek', 'chat-v4', 'SUCCESS')");
    }

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    LlmCallSchemaMigrator migrator = new LlmCallSchemaMigrator();
    migrator.migrate(jdbcTemplate);
    migrator.migrate(jdbcTemplate);

    assertThat(
            jdbcTemplate.query(
                "PRAGMA table_info(llm_calls)", (resultSet, row) -> resultSet.getString("name")))
        .contains("session_id", "success", "error_message", "elapsed_ms");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT success FROM llm_calls WHERE call_id = 'legacy-call'", Integer.class))
        .isEqualTo(1);
  }
}
