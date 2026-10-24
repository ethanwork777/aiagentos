package com.aiagentos.provider;

import com.aiagentos.core.audit.LlmCall;
import com.aiagentos.core.audit.LlmCallRepository;
import com.aiagentos.core.profile.Profile;
import com.aiagentos.core.provider.Prompt;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

public final class ProviderService {
  private final Map<String, ChatModel> providerModels;
  private final ToolSchemaAdapter toolSchemaAdapter;
  private final LlmCallRepository llmCallRepository;

  public ProviderService(
      Map<String, ChatModel> providerModels,
      ToolSchemaAdapter toolSchemaAdapter,
      LlmCallRepository llmCallRepository) {
    this.providerModels = Map.copyOf(providerModels);
    this.toolSchemaAdapter = toolSchemaAdapter;
    this.llmCallRepository = llmCallRepository;
  }

  public ChatResponse chat(String sessionId, Profile profile, Prompt prompt) {
    String providerName = profile.provider().name();
    ChatModel model = providerModels.get(providerName);
    if (model == null) {
      throw new ProviderNotFoundException(providerName);
    }

    String callId = UUID.randomUUID().toString();
    long startedAt = System.nanoTime();
    ChatResponse response;
    try {
      response = model.call(createRequest(profile, prompt));
    } catch (RuntimeException callFailure) {
      try {
        llmCallRepository.save(
            new LlmCall(
                callId,
                sessionId,
                providerName,
                profile.provider().model(),
                null,
                null,
                false,
                callFailure.getMessage(),
                elapsedMilliseconds(startedAt)));
      } catch (RuntimeException auditFailure) {
        callFailure.addSuppressed(auditFailure);
      }
      throw callFailure;
    }
    llmCallRepository.save(
        new LlmCall(
            callId,
            sessionId,
            providerName,
            profile.provider().model(),
            promptTokens(response),
            completionTokens(response),
            true,
            null,
            elapsedMilliseconds(startedAt)));
    return response;
  }

  private org.springframework.ai.chat.prompt.Prompt createRequest(Profile profile, Prompt prompt) {
    List<org.springframework.ai.tool.ToolCallback> tools =
        toolSchemaAdapter.toSpringAiTools(prompt.availableTools());
    ToolCallingChatOptions.Builder options =
        ToolCallingChatOptions.builder()
            .model(profile.provider().model())
            .toolCallbacks(tools)
            .internalToolExecutionEnabled(false);
    if (profile.provider().temperature() != null) {
      options.temperature(profile.provider().temperature());
    }
    ChatOptions chatOptions = options.build();
    return new org.springframework.ai.chat.prompt.Prompt(
        new UserMessage(prompt.content()), chatOptions);
  }

  private static Integer promptTokens(ChatResponse response) {
    return response.getMetadata() == null || response.getMetadata().getUsage() == null
        ? null
        : response.getMetadata().getUsage().getPromptTokens();
  }

  private static Integer completionTokens(ChatResponse response) {
    return response.getMetadata() == null || response.getMetadata().getUsage() == null
        ? null
        : response.getMetadata().getUsage().getCompletionTokens();
  }

  private static long elapsedMilliseconds(long startedAt) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
  }
}
