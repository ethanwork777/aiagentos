package com.aiagentos.core.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public final class ProfileLoader {
  private static final Logger logger = LoggerFactory.getLogger(ProfileLoader.class);
  private static final Pattern ENVIRONMENT_VARIABLE =
      Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");

  public Map<String, Profile> loadDirectory(Path directory, Set<String> providerNames)
      throws IOException {
    if (Files.notExists(directory)) {
      return Map.of();
    }
    if (!Files.isDirectory(directory)) {
      throw new IOException("Profile path is not a directory: " + directory);
    }

    Map<String, Profile> profiles = new LinkedHashMap<>();
    try (var files = Files.list(directory)) {
      for (Path file :
          files.filter(Files::isRegularFile).filter(ProfileLoader::isYamlFile).sorted().toList()) {
        try {
          Profile profile = loadFile(file, providerNames);
          if (profiles.putIfAbsent(profile.name(), profile) != null) {
            logger.warn("Skipping duplicate Profile name in {}", file.getFileName());
          }
        } catch (IOException exception) {
          logger.warn(
              "Skipping Profile file {} because it could not be read ({})",
              file.getFileName(),
              exception.getClass().getSimpleName());
        } catch (YAMLException exception) {
          logger.warn(
              "Skipping invalid Profile file {} because its YAML is malformed ({})",
              file.getFileName(),
              exception.getClass().getSimpleName());
        } catch (IllegalArgumentException exception) {
          logger.warn(
              "Skipping invalid Profile file {}: {}", file.getFileName(), exception.getMessage());
        }
      }
    }
    return Map.copyOf(profiles);
  }

  private static Profile loadFile(Path file, Set<String> providerNames) throws IOException {
    String source = Files.readString(file);
    Object loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(source);
    if (!(loaded instanceof Map<?, ?> rawProfile)) {
      throw new IllegalArgumentException("Profile document must be a YAML mapping");
    }

    Map<String, Object> data = resolveEnvironmentVariables(stringKeyedMap(rawProfile));
    Map<String, Object> providerData = objectMap(data, "provider");
    Profile.Provider provider =
        new Profile.Provider(
            stringValue(providerData, "name"),
            stringValue(providerData, "model"),
            doubleValue(providerData.get("temperature")));
    if (!providerNames.contains(provider.name())) {
      throw new IllegalArgumentException("Profile provider is not configured");
    }

    return new Profile(
        stringValue(data, "name"),
        optionalString(data, "description"),
        optionalString(data, "identity"),
        provider,
        objectList(data, "tools"),
        objectList(data, "skills"),
        objectList(data, "mcp_servers"),
        objectList(data, "channels"),
        objectList(data, "notify_channels"),
        objectList(data, "schedules"),
        optionalString(data, "bootstrap"),
        optionalObjectMap(data, "settings"));
  }

  private static boolean isYamlFile(Path file) {
    String name = file.getFileName().toString().toLowerCase();
    return name.endsWith(".yaml") || name.endsWith(".yml");
  }

  private static Map<String, Object> stringKeyedMap(Map<?, ?> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : source.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new IllegalArgumentException("Profile mapping keys must be strings");
      }
      result.put(key, entry.getValue());
    }
    return result;
  }

  private static Map<String, Object> resolveEnvironmentVariables(Map<String, Object> source) {
    Map<String, Object> resolved = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : source.entrySet()) {
      resolved.put(entry.getKey(), resolveValue(entry.getValue()));
    }
    return resolved;
  }

  private static Object resolveValue(Object value) {
    if (value instanceof String text) {
      Matcher matcher = ENVIRONMENT_VARIABLE.matcher(text);
      StringBuilder resolved = new StringBuilder();
      while (matcher.find()) {
        String variable = matcher.group(1);
        String environmentValue = System.getenv(variable);
        if (environmentValue == null) {
          throw new IllegalArgumentException("Required environment variable is not set");
        }
        matcher.appendReplacement(resolved, Matcher.quoteReplacement(environmentValue));
      }
      matcher.appendTail(resolved);
      return resolved.toString();
    }
    if (value instanceof List<?> list) {
      return list.stream().map(ProfileLoader::resolveValue).toList();
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> resolved = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (!(entry.getKey() instanceof String key)) {
          throw new IllegalArgumentException("Profile mapping keys must be strings");
        }
        resolved.put(key, resolveValue(entry.getValue()));
      }
      return resolved;
    }
    return value;
  }

  private static Map<String, Object> objectMap(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("Profile field must be a mapping: " + key);
    }
    return stringKeyedMap(map);
  }

  private static Map<String, Object> optionalObjectMap(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value == null) {
      return Map.of();
    }
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("Profile field must be a mapping: " + key);
    }
    return stringKeyedMap(map);
  }

  private static List<Object> objectList(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value == null) {
      return List.of();
    }
    if (!(value instanceof List<?> list)) {
      throw new IllegalArgumentException("Profile field must be a list: " + key);
    }
    return new ArrayList<>(list);
  }

  private static String stringValue(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException("Profile field is required: " + key);
    }
    return text;
  }

  private static String optionalString(Map<String, Object> source, String key) {
    Object value = source.get(key);
    if (value == null) {
      return null;
    }
    if (!(value instanceof String text)) {
      throw new IllegalArgumentException("Profile field must be a string: " + key);
    }
    return text;
  }

  private static Double doubleValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    throw new IllegalArgumentException("Profile provider temperature must be numeric");
  }
}
