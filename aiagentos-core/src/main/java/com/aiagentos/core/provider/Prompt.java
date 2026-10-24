package com.aiagentos.core.provider;

import java.util.List;

public record Prompt(String content, List<ToolSpec> availableTools) {
  public Prompt {
    if (content == null || content.isBlank()) {
      throw new IllegalArgumentException("Prompt content is required");
    }
    availableTools = List.copyOf(availableTools);
  }
}
