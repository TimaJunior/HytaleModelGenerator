/*
 * Copyright 2026 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.cats.config;

import com.hypixel.hytale.logger.HytaleLogger;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public abstract class Config {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final Path CONFIG_DIR = Paths.get("config", "cats");

  private final String fileName;
  private final Properties properties = new Properties();
  private boolean loaded = false;

  protected Config(String fileName) {
    this.fileName = fileName;
  }

  protected static boolean parseBoolean(Properties props, String key, boolean defaultValue) {
    String value = props.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value.trim());
  }

  protected static int parseInt(Properties props, String key, int defaultValue) {
    String value = props.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      LOGGER.at(Level.WARNING).log(
          "Invalid integer for key '%s': '%s', using default %d", key, value, defaultValue);
      return defaultValue;
    }
  }

  protected static float parseFloat(Properties props, String key, float defaultValue) {
    String value = props.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Float.parseFloat(value.trim());
    } catch (NumberFormatException e) {
      LOGGER.at(Level.WARNING).log(
          "Invalid float for key '%s': '%s', using default %f", key, value, defaultValue);
      return defaultValue;
    }
  }

  protected static String parseString(Properties props, String key, String defaultValue) {
    String value = props.getProperty(key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    return value.trim();
  }

  public Path getConfigPath() {
    return CONFIG_DIR.resolve(fileName);
  }

  protected void init() {
    try {
      Files.createDirectories(CONFIG_DIR);
      Path configPath = getConfigPath();
      if (!Files.exists(configPath)) {
        LOGGER.at(Level.INFO).log("Creating default config at %s", configPath);
        writeDefaults(configPath);
      }
      load(configPath);
      loaded = true;
      LOGGER.at(Level.INFO).log("Config loaded: %s", configPath);
    } catch (IOException e) {
      LOGGER.at(Level.SEVERE).log("Failed to initialize config: %s", fileName, e);
      applyDefaults();
      loaded = true;
    }
  }

  public void reload() {
    try {
      Path configPath = getConfigPath();
      if (Files.exists(configPath)) {
        load(configPath);
        LOGGER.at(Level.INFO).log("Config reloaded: %s", configPath);
      }
    } catch (IOException e) {
      LOGGER.at(Level.SEVERE).log("Failed to reload config: %s", fileName, e);
    }
  }

  public boolean isLoaded() {
    return loaded;
  }

  protected abstract LinkedHashMap<String, String> getDefaults();

  protected abstract LinkedHashMap<String, String> getComments();

  protected abstract String getHeader();

  protected abstract void applyProperties(Properties props);

  protected abstract void applyDefaults();

  private void load(Path configPath) throws IOException {
    properties.clear();
    try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    applyProperties(properties);
  }

  private void writeDefaults(Path configPath) throws IOException {
    LinkedHashMap<String, String> defaults = getDefaults();
    LinkedHashMap<String, String> comments = getComments();

    try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
      writer.write("# " + getHeader());
      writer.newLine();
      writer.write("# ");
      writer.newLine();
      writer.write("# Auto-generated configuration file for BOs Cats Companion.");
      writer.newLine();
      writer.write("# Edit values below. Lines starting with # are comments.");
      writer.newLine();
      writer.write("# To restore defaults, delete this file and restart the server.");
      writer.newLine();
      writer.newLine();

      for (Map.Entry<String, String> entry : defaults.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();

        String comment = comments.get(key);
        if (comment != null) {
          for (String commentLine : comment.split("\n")) {
            writer.write("# " + commentLine);
            writer.newLine();
          }
        }

        writer.write(key + "=" + value);
        writer.newLine();
        writer.newLine();
      }
    }
  }
}
