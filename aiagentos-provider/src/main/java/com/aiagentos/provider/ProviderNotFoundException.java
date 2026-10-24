package com.aiagentos.provider;

public final class ProviderNotFoundException extends RuntimeException {
  public ProviderNotFoundException(String name) {
    super("Provider not found: " + name);
  }
}
