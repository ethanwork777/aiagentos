package com.aiagentos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.aiagentos.core.audit.LlmCallRepository;
import com.aiagentos.core.profile.ProfileRegistry;
import com.aiagentos.provider.ProviderService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProviderRuntimeConfigurationTest {
  @Test
  void registersConfiguredChatModelBeanAndLoadsProfiles(@TempDir Path directory) throws Exception {
    Files.writeString(
        directory.resolve("assistant.yaml"),
        """
        name: assistant
        provider:
          name: deepseek
          model: model-v1
        """);

    new ApplicationContextRunner()
        .withUserConfiguration(ProviderRuntimeConfiguration.class)
        .withBean("deepseek", ChatModel.class, () -> mock(ChatModel.class))
        .withBean(LlmCallRepository.class, () -> mock(LlmCallRepository.class))
        .withPropertyValues(
            "aiagentos.providers=deepseek",
            "aiagentos.profiles.directory=" + directory.toAbsolutePath())
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context.getBean(ProviderService.class)).isNotNull();
              ProfileRegistry registry = context.getBean(ProfileRegistry.class);
              assertThat(registry.get("assistant").provider().name()).isEqualTo("deepseek");
            });
  }

  @Test
  void rejectsConfiguredProviderWithoutMatchingChatModelBean() {
    new ApplicationContextRunner()
        .withUserConfiguration(ProviderRuntimeConfiguration.class)
        .withBean(LlmCallRepository.class, () -> mock(LlmCallRepository.class))
        .withPropertyValues("aiagentos.providers=missing")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseMessage("Configured provider has no ChatModel bean named 'missing'");
            });
  }
}
