package com.aiagentos.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiagentos.core.profile.ProfileRegistry;
import com.aiagentos.core.provider.Prompt;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Tag("integration")
@EnabledIfSystemProperty(named = "aiagentos.smoke.enabled", matches = "true")
class ProviderSmokeIT {
  @Autowired private ProfileRegistry profileRegistry;

  @Autowired private ProviderService providerService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void callsConfiguredModelAndPersistsSuccessfulAudit() {
    String sessionId = "provider-smoke-" + UUID.randomUUID();
    int callsBefore =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM llm_calls WHERE session_id = ?", Integer.class, sessionId);

    var response =
        providerService.chat(
            sessionId,
            profileRegistry.get("provider-smoke"),
            new Prompt("Reply with the word ready.", List.of()));

    assertThat(response.getResult()).isNotNull();
    assertThat(response.getResult().getOutput().getText()).isNotBlank();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM llm_calls WHERE session_id = ? AND success = 1",
                Integer.class,
                sessionId))
        .isEqualTo(callsBefore + 1);
  }
}
