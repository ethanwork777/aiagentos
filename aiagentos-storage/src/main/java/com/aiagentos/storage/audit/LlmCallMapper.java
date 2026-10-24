package com.aiagentos.storage.audit;

import com.aiagentos.core.audit.LlmCall;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
interface LlmCallMapper {
  @Insert(
      """
      INSERT INTO llm_calls (
        call_id, session_id, provider, model, prompt_tokens, completion_tokens,
        success, error_message, elapsed_ms, status
      ) VALUES (
        #{callId}, #{sessionId}, #{provider}, #{model}, #{promptTokens}, #{completionTokens},
        #{success}, #{errorMessage}, #{elapsedMs}, #{status}
      )
      """)
  void insert(LlmCallRow call);

  @Select(
      """
      SELECT call_id AS callId, session_id AS sessionId, provider, model,
             prompt_tokens AS promptTokens, completion_tokens AS completionTokens,
             success, error_message AS errorMessage, elapsed_ms AS elapsedMs
      FROM llm_calls
      WHERE call_id = #{callId}
      """)
  LlmCall findByCallId(@Param("callId") String callId);
}
