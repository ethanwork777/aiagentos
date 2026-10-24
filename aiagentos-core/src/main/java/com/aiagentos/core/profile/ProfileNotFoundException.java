package com.aiagentos.core.profile;

public final class ProfileNotFoundException extends RuntimeException {
  public ProfileNotFoundException(String name) {
    super("Profile not found: " + name);
  }
}
