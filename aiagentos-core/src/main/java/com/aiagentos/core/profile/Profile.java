package com.aiagentos.core.profile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Profile(
    String name,
    String description,
    String identity,
    Provider provider,
    List<Object> tools,
    List<Object> skills,
    List<Object> mcpServers,
    List<Object> channels,
    List<Object> notifyChannels,
    List<Object> schedules,
    String bootstrap,
    Map<String, Object> settings) {

  public Profile {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Profile name is required");
    }
    Objects.requireNonNull(provider, "Profile provider is required");
    tools = List.copyOf(tools);
    skills = List.copyOf(skills);
    mcpServers = List.copyOf(mcpServers);
    channels = List.copyOf(channels);
    notifyChannels = List.copyOf(notifyChannels);
    schedules = List.copyOf(schedules);
    settings = Map.copyOf(settings);
  }

  public record Provider(String name, String model, Double temperature) {
    public Provider {
      if (name == null || name.isBlank()) {
        throw new IllegalArgumentException("Profile provider name is required");
      }
      if (model == null || model.isBlank()) {
        throw new IllegalArgumentException("Profile provider model is required");
      }
    }
  }
}
