package com.aiagentos.provider;

import com.aiagentos.core.provider.ToolSpec;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public final class ToolSchemaAdapter {
  public List<ToolCallback> toSpringAiTools(List<ToolSpec> tools) {
    return tools.stream().map(ToolSchemaAdapter::toSpringAiTool).toList();
  }

  private static ToolCallback toSpringAiTool(ToolSpec tool) {
    ToolDefinition definition =
        ToolDefinition.builder()
            .name(tool.name())
            .description(Objects.requireNonNullElse(tool.description(), ""))
            .inputSchema(tool.inputSchema())
            .build();
    return new ToolCallback() {
      @Override
      public ToolDefinition getToolDefinition() {
        return definition;
      }

      @Override
      public String call(String input) {
        throw new UnsupportedOperationException(
            "Tool execution belongs to the agent tool executor");
      }
    };
  }
}
