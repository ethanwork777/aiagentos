package com.aiagentos.core.audit;

public record LlmCall(
    String callId,
    String sessionId,
    String provider,
    String model,
    Integer promptTokens,
    Integer completionTokens,
    boolean success,
    String errorMessage,
    long elapsedMs) {}
