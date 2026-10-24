package com.aiagentos.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aiagentos.core.audit.LlmCall;
import com.aiagentos.core.audit.LlmCallRepository;
import com.aiagentos.core.profile.Profile;
import com.aiagentos.core.provider.Prompt;
import com.aiagentos.core.provider.ToolSpec;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

class ProviderServiceTest {
  private final ChatModel deepseek = mock(ChatModel.class);
  private final ChatModel kimi = mock(ChatModel.class);
  private final LlmCallRepository audit = mock(LlmCallRepository.class);
  private final ProviderService service =
      new ProviderService(
          Map.of("deepseek", deepseek, "kimi", kimi), new ToolSchemaAdapter(), audit);

  @Test
  void chatRoutesToConfiguredProviderAndDisablesToolExecution() {
    ChatResponse response = responseWithUsage(12, 7);
    when(kimi.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(response);

    ChatResponse result =
        service.chat(
            "session-1",
            profile("kimi"),
            new Prompt(
                "Check the service status",
                List.of(new ToolSpec("http_get", "Fetch a URL", "{\"type\":\"object\"}"))));

    assertThat(result).isSameAs(response);
    verify(kimi).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    verify(deepseek, never()).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    ArgumentCaptor<LlmCall> auditCaptor = ArgumentCaptor.forClass(LlmCall.class);
    verify(audit).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue())
        .satisfies(
            call -> {
              assertThat(call.sessionId()).isEqualTo("session-1");
              assertThat(call.provider()).isEqualTo("kimi");
              assertThat(call.model()).isEqualTo("model-kimi");
              assertThat(call.promptTokens()).isEqualTo(12);
              assertThat(call.completionTokens()).isEqualTo(7);
              assertThat(call.success()).isTrue();
              assertThat(call.errorMessage()).isNull();
            });

    ArgumentCaptor<org.springframework.ai.chat.prompt.Prompt> promptCaptor =
        ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
    verify(kimi).call(promptCaptor.capture());
    ChatOptions options = promptCaptor.getValue().getOptions();
    assertThat(options.getModel()).isEqualTo("model-kimi");
    assertThat(options.getTemperature()).isEqualTo(0.4);
    assertThat(options).isInstanceOf(ToolCallingChatOptions.class);
    assertThat(((ToolCallingChatOptions) options).getInternalToolExecutionEnabled()).isFalse();
    assertThat(((ToolCallingChatOptions) options).getToolCallbacks())
        .singleElement()
        .satisfies(
            tool -> {
              assertThat(tool.getToolDefinition().name()).isEqualTo("http_get");
              assertThat(tool.getToolDefinition().inputSchema()).isEqualTo("{\"type\":\"object\"}");
            });
  }

  @Test
  void chatAuditsFailedModelCallAndRethrowsOriginalException() {
    RuntimeException failure = new RuntimeException("connect timeout");
    when(deepseek.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenThrow(failure);

    assertThatThrownBy(
            () -> service.chat("session-2", profile("deepseek"), new Prompt("Question", List.of())))
        .isSameAs(failure);

    ArgumentCaptor<LlmCall> auditCaptor = ArgumentCaptor.forClass(LlmCall.class);
    verify(audit).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue())
        .satisfies(
            call -> {
              assertThat(call.sessionId()).isEqualTo("session-2");
              assertThat(call.provider()).isEqualTo("deepseek");
              assertThat(call.model()).isEqualTo("model-deepseek");
              assertThat(call.success()).isFalse();
              assertThat(call.errorMessage()).isEqualTo("connect timeout");
            });
  }

  @Test
  void chatRejectsUnknownProviderBeforeCallingModel() {
    assertThatThrownBy(
            () -> service.chat("session-3", profile("unknown"), new Prompt("Question", List.of())))
        .isInstanceOf(ProviderNotFoundException.class)
        .hasMessage("Provider not found: unknown");

    verify(deepseek, never()).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    verify(kimi, never()).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    verify(audit, never()).save(any());
  }

  private static Profile profile(String provider) {
    return new Profile(
        "assistant",
        null,
        null,
        new Profile.Provider(provider, "model-" + provider, 0.4),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        null,
        Map.of());
  }

  private static ChatResponse responseWithUsage(int promptTokens, int completionTokens) {
    ChatResponse response = mock(ChatResponse.class);
    ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
    Usage usage = mock(Usage.class);
    when(response.getMetadata()).thenReturn(metadata);
    when(metadata.getUsage()).thenReturn(usage);
    when(usage.getPromptTokens()).thenReturn(promptTokens);
    when(usage.getCompletionTokens()).thenReturn(completionTokens);
    return response;
  }
}
