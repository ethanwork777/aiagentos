package com.aiagentos.web.api;

import java.time.Instant;

public record ApiErrorResponse(String errorCode, String message, Instant timestamp) {}
