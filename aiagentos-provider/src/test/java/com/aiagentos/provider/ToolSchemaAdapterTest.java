package com.aiagentos.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aiagentos.core.provider.ToolSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class ToolSchemaAdapterTest {
  private final ToolSchemaAdapter adapter = new ToolSchemaAdapter();

  @Test
  void toSpringAiToolsPreservesSchemaAndDoesNotExecuteTheTool() {
    var tools =
        adapter.toSpringAiTools(
            List.of(new ToolSpec("http_get", "Fetch a URL", "{\"type\":\"object\"}")));

    assertThat(tools).hasSize(1);
    assertThat(tools.getFirst().getToolDefinition().name()).isEqualTo("http_get");
    assertThat(tools.getFirst().getToolDefinition().description()).isEqualTo("Fetch a URL");
    assertThat(tools.getFirst().getToolDefinition().inputSchema())
        .isEqualTo("{\"type\":\"object\"}");
    assertThatThrownBy(() -> tools.getFirst().call("{}"))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Tool execution belongs to the agent tool executor");
  }
}
