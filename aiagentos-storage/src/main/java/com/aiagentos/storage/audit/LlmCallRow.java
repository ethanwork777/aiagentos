package com.aiagentos.storage.audit;

import com.aiagentos.core.audit.LlmCall;

record LlmCallRow(
    String callId,
    String sessionId,
    String provider,
    String model,
    Integer promptTokens,
    Integer completionTokens,
    boolean success,
    String errorMessage,
    long elapsedMs,
    String status) {
  static LlmCallRow from(LlmCall call) {
    return new LlmCallRow(
        call.callId(),
        call.sessionId(),
        call.provider(),
        call.model(),
        call.promptTokens(),
        call.completionTokens(),
        call.success(),
        call.errorMessage(),
        call.elapsedMs(),
        call.success() ? "SUCCESS" : "FAILED");
  }
}
