package com.aiagentos.config;

import com.aiagentos.core.audit.LlmCallRepository;
import com.aiagentos.core.profile.ProfileLoader;
import com.aiagentos.core.profile.ProfileRegistry;
import com.aiagentos.provider.ProviderService;
import com.aiagentos.provider.ToolSchemaAdapter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderRuntimeConfiguration {
  @Bean
  Map<String, ChatModel> configuredProviderModels(
      @Value("${aiagentos.providers:}") List<String> providerNames,
      Map<String, ChatModel> chatModelBeans) {
    Map<String, ChatModel> configuredModels = new LinkedHashMap<>();
    for (String providerName : providerNames) {
      ChatModel model = chatModelBeans.get(providerName);
      if (model == null) {
        throw new IllegalStateException(
            "Configured provider has no ChatModel bean named '" + providerName + "'");
      }
      if (configuredModels.putIfAbsent(providerName, model) != null) {
        throw new IllegalStateException("Duplicate configured provider: " + providerName);
      }
    }
    return Map.copyOf(configuredModels);
  }

  @Bean
  ProfileRegistry profileRegistry(
      @Value("${aiagentos.profiles.directory:.aiagentos/profiles}") String profileDirectory,
      Map<String, ChatModel> configuredProviderModels)
      throws IOException {
    return new ProfileRegistry(
        new ProfileLoader()
            .loadDirectory(Path.of(profileDirectory), configuredProviderModels.keySet()));
  }

  @Bean
  ToolSchemaAdapter toolSchemaAdapter() {
    return new ToolSchemaAdapter();
  }

  @Bean
  ProviderService providerService(
      Map<String, ChatModel> configuredProviderModels,
      ToolSchemaAdapter toolSchemaAdapter,
      LlmCallRepository llmCallRepository) {
    return new ProviderService(configuredProviderModels, toolSchemaAdapter, llmCallRepository);
  }
}
