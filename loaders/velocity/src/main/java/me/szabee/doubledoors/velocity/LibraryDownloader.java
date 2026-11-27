package me.szabee.doubledoors.velocity;

import com.velocitypowered.api.plugin.PluginManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;

/**
 * Downloads required SQL dependencies on first start and injects them into the
 * plugin classpath via {@link PluginManager#addToClasspath}. Each library is
 * fetched from Maven Central, SHA-256 verified, and cached under
 * {@code plugins/doubledoors-velocity/libs/} with a versioned filename so
 * upgrades never reuse stale JARs.
 */
final class LibraryDownloader {

  private static final String MAVEN_CENTRAL =
    "https://repo1.maven.org/maven2";

  private static final String[][] LIBRARIES = {
    {
      "sqlite-jdbc",
      "org.xerial",
      "sqlite-jdbc",
      "${sqliteVersion}",
      "${sqliteSha256}",
    },
    {
      "mysql-connector-j",
      "com.mysql",
      "mysql-connector-j",
      "${mysqlConnectorVersion}",
      "${mysqlConnectorSha256}",
    },
    {
      "HikariCP",
      "com.zaxxer",
      "HikariCP",
      "${hikariVersion}",
      "${hikariSha256}",
    },
  };

  private final PluginManager pluginManager;
  private final Object plugin;
  private final Path libsDir;
  private final Logger logger;

  /**
   * Creates a library downloader for the given plugin.
   *
   * @param pluginManager the Velocity plugin manager
   * @param plugin the plugin instance used for classpath injection
   * @param dataDirectory the plugin data directory
   * @param logger the plugin logger
   */
  LibraryDownloader(
    PluginManager pluginManager,
    Object plugin,
    Path dataDirectory,
    Logger logger
  ) {
    this.pluginManager = pluginManager;
    this.plugin = plugin;
    this.libsDir = dataDirectory.resolve("libs");
    this.logger = logger;
  }

  /**
   * Downloads all required libraries and adds them to the classpath.
   *
   * @return {@code true} if every library was successfully loaded
   */
  boolean downloadAndInject() {
    try {
      Files.createDirectories(libsDir);
    } catch (IOException e) {
      logger.error(
        "Failed to create libs directory at {}: {}",
        libsDir,
        e.getMessage()
      );
      return false;
    }

    for (String[] lib : LIBRARIES) {
      String label = lib[0];
      String group = lib[1];
      String artifact = lib[2];
      String version = lib[3];
      String expectedSha256 = lib[4];

      Path jarPath = libsDir.resolve(label + "-" + version + ".jar");

      if (Files.exists(jarPath)) {
        if (!verifyChecksum(jarPath, expectedSha256)) {
          logger.warn(
            "Cached {} has bad checksum, re-downloading.",
            label
          );
          try {
            Files.deleteIfExists(jarPath);
          } catch (IOException ignored) {}
        } else {
          logger.info("Using cached {}.", label);
          if (!addToClasspath(label, jarPath)) {
            return false;
          }
          continue;
        }
      }

      Path tmpPath = jarPath.resolveSibling(jarPath.getFileName() + ".tmp");
      logger.info("Downloading {} {} …", artifact, version);
      if (!download(group, artifact, version, tmpPath)) {
        logger.error(
          "Failed to download {}. SQL functionality will be disabled.",
          label
        );
        return false;
      }

      if (!verifyChecksum(tmpPath, expectedSha256)) {
        logger.error(
          "Checksum mismatch for {} — discarding corrupted download.",
          label
        );
        try {
          Files.deleteIfExists(tmpPath);
        } catch (IOException ignored) {}
        return false;
      }

      try {
        Files.move(tmpPath, jarPath, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException e) {
        logger.error(
          "Failed to move verified JAR for {}: {}",
          label,
          e.getMessage()
        );
        try {
          Files.deleteIfExists(tmpPath);
        } catch (IOException ignored) {}
        return false;
      }

      logger.info("Downloaded {} successfully.", label);
      if (!addToClasspath(label, jarPath)) {
        return false;
      }
    }

    return true;
  }

  private boolean addToClasspath(String label, Path jarPath) {
    try {
      pluginManager.addToClasspath(plugin, jarPath);
      return true;
    } catch (Exception e) {
      logger.error(
        "Failed to add {} to classpath: {}",
        label,
        e.getMessage()
      );
      return false;
    }
  }

  private boolean download(
    String group,
    String artifact,
    String version,
    Path target
  ) {
    String url = mavenUrl(group, artifact, version);
    try {
      URLConnection conn = URI.create(url).toURL().openConnection();
      conn.setConnectTimeout(10_000);
      conn.setReadTimeout(60_000);
      try (InputStream in = conn.getInputStream()) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }
      return true;
    } catch (IOException e) {
      logger.warn(
        "Download failed for {}:{}:{} — {}",
        group,
        artifact,
        version,
        e.getMessage()
      );
      try {
        Files.deleteIfExists(target);
      } catch (IOException ignored) {}
      return false;
    }
  }

  private static boolean verifyChecksum(Path file, String expectedHex) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream in = Files.newInputStream(file)) {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
          digest.update(buf, 0, n);
        }
      }
      byte[] hash = digest.digest();
      StringBuilder hex = new StringBuilder(64);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString().equalsIgnoreCase(expectedHex);
    } catch (NoSuchAlgorithmException | IOException e) {
      return false;
    }
  }

  private static String mavenUrl(
    String group,
    String artifact,
    String version
  ) {
    return MAVEN_CENTRAL +
      "/" +
      group.replace('.', '/') +
      "/" +
      artifact +
      "/" +
      version +
      "/" +
      artifact +
      "-" +
      version +
      ".jar";
  }
}
