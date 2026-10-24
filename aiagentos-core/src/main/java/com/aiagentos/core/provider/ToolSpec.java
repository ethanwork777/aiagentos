package com.aiagentos.core.provider;

public record ToolSpec(String name, String description, String inputSchema) {
  public ToolSpec {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Tool name is required");
    }
    if (inputSchema == null || inputSchema.isBlank()) {
      throw new IllegalArgumentException("Tool input schema is required");
    }
  }
}
