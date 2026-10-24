package com.aiagentos.storage.audit;

import java.util.HashSet;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;

final class LlmCallSchemaMigrator {
  void migrate(JdbcTemplate jdbcTemplate) {
    Set<String> columns =
        new HashSet<>(
            jdbcTemplate.query(
                "PRAGMA table_info(llm_calls)", (resultSet, row) -> resultSet.getString("name")));
    if (columns.isEmpty()) {
      throw new IllegalStateException("The llm_calls table was not initialized");
    }

    addColumn(jdbcTemplate, columns, "session_id", "TEXT NOT NULL DEFAULT ''");
    if (addColumn(jdbcTemplate, columns, "success", "INTEGER NOT NULL DEFAULT 0")) {
      jdbcTemplate.update(
          "UPDATE llm_calls SET success = CASE WHEN upper(status) = 'SUCCESS' THEN 1 ELSE 0 END");
    }
    addColumn(jdbcTemplate, columns, "error_message", "TEXT");
    addColumn(jdbcTemplate, columns, "elapsed_ms", "INTEGER NOT NULL DEFAULT 0");
  }

  private static boolean addColumn(
      JdbcTemplate jdbcTemplate, Set<String> columns, String name, String definition) {
    if (columns.contains(name)) {
      return false;
    }
    jdbcTemplate.execute("ALTER TABLE llm_calls ADD COLUMN " + name + " " + definition);
    columns.add(name);
    return true;
  }
}
