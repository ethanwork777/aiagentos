package com.aiagentos.storage.audit;

import com.aiagentos.core.audit.LlmCall;
import com.aiagentos.core.audit.LlmCallRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisLlmCallRepository implements LlmCallRepository {
  private final LlmCallMapper mapper;

  public MyBatisLlmCallRepository(LlmCallMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void save(LlmCall call) {
    mapper.insert(LlmCallRow.from(call));
  }

  @Override
  public Optional<LlmCall> findByCallId(String callId) {
    return Optional.ofNullable(mapper.findByCallId(callId));
  }
}
