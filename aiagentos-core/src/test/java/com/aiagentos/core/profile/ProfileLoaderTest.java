package com.aiagentos.core.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileLoaderTest {
  private final ProfileLoader loader = new ProfileLoader();

  @Test
  void loadDirectoryParsesProfileFieldsAndEnvironmentVariables(@TempDir Path directory)
      throws IOException {
    Path profileFile = directory.resolve("ops-agent.yaml");
    Files.writeString(
        profileFile,
        """
        name: ops-agent
        description: Operations assistant
        identity: Be concise
        provider:
          name: deepseek
          model: deepseek-chat
          temperature: 0.2
        tools:
          - http_get
        skills:
          - operations
        mcp_servers:
          - name: local
        channels:
          - cli
        notify_channels:
          - webhook
        schedules:
          - cron: "0 0 * * *"
        bootstrap: ${PATH}
        settings:
          locale: zh-CN
        """);

    Map<String, Profile> profiles = loader.loadDirectory(directory, Set.of("deepseek"));

    Profile profile = profiles.get("ops-agent");
    assertThat(profile).isNotNull();
    assertThat(profile.description()).isEqualTo("Operations assistant");
    assertThat(profile.identity()).isEqualTo("Be concise");
    assertThat(profile.provider().name()).isEqualTo("deepseek");
    assertThat(profile.provider().model()).isEqualTo("deepseek-chat");
    assertThat(profile.provider().temperature()).isEqualTo(0.2);
    assertThat(profile.tools()).containsExactly("http_get");
    assertThat(profile.skills()).containsExactly("operations");
    assertThat(profile.mcpServers()).containsExactly(Map.of("name", "local"));
    assertThat(profile.channels()).containsExactly("cli");
    assertThat(profile.notifyChannels()).containsExactly("webhook");
    assertThat(profile.schedules()).containsExactly(Map.of("cron", "0 0 * * *"));
    assertThat(profile.bootstrap()).isEqualTo(System.getenv("PATH"));
    assertThat(profile.settings()).containsEntry("locale", "zh-CN");
  }

  @Test
  void loadDirectorySkipsInvalidProfilesAndContinues(@TempDir Path directory) throws IOException {
    Files.writeString(
        directory.resolve("invalid-provider.yaml"),
        """
        name: invalid
        provider:
          name: missing
          model: model
        """);
    Files.writeString(directory.resolve("malformed.yaml"), "name: [unclosed");
    Files.writeString(
        directory.resolve("valid.yaml"),
        """
        name: valid
        provider:
          name: configured
          model: model
        """);

    Map<String, Profile> profiles = loader.loadDirectory(directory, Set.of("configured"));

    assertThat(profiles).containsOnlyKeys("valid");
  }

  @Test
  void loadDirectoryReturnsEmptyWhenDirectoryDoesNotExist(@TempDir Path directory)
      throws IOException {
    assertThat(loader.loadDirectory(directory.resolve("missing"), Set.of())).isEmpty();
  }
}
