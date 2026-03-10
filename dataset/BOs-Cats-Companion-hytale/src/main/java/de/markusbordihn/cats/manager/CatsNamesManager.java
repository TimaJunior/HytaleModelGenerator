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

package de.markusbordihn.cats.manager;

import com.hypixel.hytale.logger.HytaleLogger;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class CatsNamesManager {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final String RESOURCE_PATH = "/Server/cat_names.txt";
  private static final Path CONFIG_FILE_PATH = Paths.get("config", "cats", "cat_names.txt");
  private static final Random RANDOM = new Random();
  private static final String VALID_NAME_PATTERN = "^[a-zA-Z0-9 '\\-.]+$";
  private static final int MAX_NAME_LENGTH = 32;
  private static final int MIN_NAME_LENGTH = 1;
  private static final List<String> catNames = new ArrayList<>();
  private static boolean initialized = false;

  public static void initialize() {
    if (initialized) {
      LOGGER.at(Level.WARNING).log("CatNamesManager already initialized");
      return;
    }

    try {
      Files.createDirectories(CONFIG_FILE_PATH.getParent());
      if (!Files.exists(CONFIG_FILE_PATH)) {
        LOGGER.at(Level.INFO).log("Creating default cat names config at %s", CONFIG_FILE_PATH);
        copyDefaultConfig();
      }
      loadNamesFromFile();
      initialized = true;
      LOGGER.at(Level.INFO).log("CatNamesManager initialized with %d names", catNames.size());
    } catch (IOException e) {
      LOGGER.at(Level.SEVERE).log("Failed to initialize CatNamesManager", e);
      loadDefaultNames();
      initialized = true;
    }
  }

  private static void copyDefaultConfig() throws IOException {
    try (InputStream inputStream = CatsNamesManager.class.getResourceAsStream(RESOURCE_PATH)) {
      if (inputStream == null) {
        LOGGER.at(Level.WARNING).log("Resource not found: %s, using fallback names", RESOURCE_PATH);
        createFallbackConfig();
        return;
      }

      try (BufferedReader reader =
              new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
          BufferedWriter writer =
              Files.newBufferedWriter(CONFIG_FILE_PATH, StandardCharsets.UTF_8)) {
        String line;
        while ((line = reader.readLine()) != null) {
          writer.write(line);
          writer.newLine();
        }
      }
      LOGGER.at(Level.INFO).log("Default cat names config created successfully");
    }
  }

  private static void createFallbackConfig() throws IOException {
    try (BufferedWriter writer =
        Files.newBufferedWriter(CONFIG_FILE_PATH, StandardCharsets.UTF_8)) {
      writer.write("# Cat Names Configuration");
      writer.newLine();
      writer.write("# Add one name per line. Empty lines and lines starting with # are ignored.");
      writer.newLine();
      writer.write("# These names will be randomly assigned when taming cats.");
      writer.newLine();
      writer.write("# ");
      writer.newLine();
      writer.write("# SECURITY RULES:");
      writer.newLine();
      writer.write(
          "# - Only letters (a-z, A-Z), numbers (0-9), spaces, hyphens (-), apostrophes ('), and dots (.) are allowed");
      writer.newLine();
      writer.write("# - Names must be between 1 and 32 characters long");
      writer.newLine();
      writer.write("# - Invalid names will be skipped and logged as warnings");
      writer.newLine();
      writer.newLine();

      for (String name : getDefaultCatNames()) {
        writer.write(name);
        writer.newLine();
      }
    }
  }

  private static void loadNamesFromFile() throws IOException {
    catNames.clear();
    int skippedCount = 0;

    for (String line : Files.readAllLines(CONFIG_FILE_PATH, StandardCharsets.UTF_8)) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }

      if (isValidCatName(trimmed)) {
        catNames.add(trimmed);
      } else {
        skippedCount++;
        LOGGER.at(Level.WARNING).log(
            "Skipped invalid cat name: '%s'",
            trimmed.length() > 50 ? trimmed.substring(0, 50) + "..." : trimmed);
      }
    }

    if (skippedCount > 0) {
      LOGGER.at(Level.WARNING).log("Skipped %d invalid cat names from config", skippedCount);
    }

    if (catNames.isEmpty()) {
      LOGGER.at(Level.WARNING).log("No valid cat names found in config, using defaults");
      loadDefaultNames();
    }
  }

  private static boolean isValidCatName(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }

    if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
      return false;
    }

    if (!name.matches(VALID_NAME_PATTERN)) {
      return false;
    }

    return !name.contains("\n") && !name.contains("\r") && !name.contains("\t");
  }

  private static void loadDefaultNames() {
    catNames.clear();
    catNames.addAll(getDefaultCatNames());
  }

  private static List<String> getDefaultCatNames() {
    return List.of(
        "Whiskers",
        "Shadow",
        "Luna",
        "Tiger",
        "Mittens",
        "Felix",
        "Smokey",
        "Oliver",
        "Bella",
        "Max",
        "Chloe",
        "Charlie",
        "Milo",
        "Lucy",
        "Leo",
        "Molly",
        "Jack");
  }

  public static String getRandomName() {
    if (!initialized) {
      LOGGER.at(Level.WARNING).log("CatNamesManager not initialized, calling initialize()");
      initialize();
    }

    if (catNames.isEmpty()) {
      LOGGER.at(Level.WARNING).log("No cat names available, returning default");
      return "Cat";
    }

    String name = catNames.get(RANDOM.nextInt(catNames.size()));
    if (!isValidCatName(name)) {
      LOGGER.at(Level.SEVERE).log(
          "Invalid name detected in loaded list: '%s', using fallback", name);
      return "Cat";
    }

    return name;
  }

  public static void reload() {
    try {
      loadNamesFromFile();
      LOGGER.at(Level.INFO).log("Cat names reloaded (%d names)", catNames.size());
    } catch (IOException e) {
      LOGGER.at(Level.SEVERE).log("Failed to reload cat names", e);
    }
  }

  public static int getNameCount() {
    return catNames.size();
  }

  public static Path getConfigPath() {
    return CONFIG_FILE_PATH;
  }
}
