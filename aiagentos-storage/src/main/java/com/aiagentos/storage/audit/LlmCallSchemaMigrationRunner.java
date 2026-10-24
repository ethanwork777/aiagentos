package com.aiagentos.storage.audit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
final class LlmCallSchemaMigrationRunner implements ApplicationRunner {
  private final JdbcTemplate jdbcTemplate;

  LlmCallSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    new LlmCallSchemaMigrator().migrate(jdbcTemplate);
  }
}
