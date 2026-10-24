package com.aiagentos.core.profile;

import java.util.Map;

public final class ProfileRegistry {
  private final Map<String, Profile> profiles;

  public ProfileRegistry(Map<String, Profile> profiles) {
    this.profiles = Map.copyOf(profiles);
  }

  public Profile get(String name) {
    Profile profile = profiles.get(name);
    if (profile == null) {
      throw new ProfileNotFoundException(name);
    }
    return profile;
  }

  public Map<String, Profile> profiles() {
    return profiles;
  }
}
