package com.aiagentos.core.audit;

import java.util.Optional;

public interface LlmCallRepository {
  void save(LlmCall call);

  Optional<LlmCall> findByCallId(String callId);
}
