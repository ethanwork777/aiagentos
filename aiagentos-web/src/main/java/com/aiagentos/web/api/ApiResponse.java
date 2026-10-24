package com.aiagentos.web.api;

import java.time.Instant;

public record ApiResponse<T>(String code, String message, T data, Instant timestamp) {
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>("OK", "Success", data, Instant.now());
  }
}
