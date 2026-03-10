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

import java.util.LinkedHashMap;
import java.util.Properties;

public class GeneralConfig extends Config {

  private static final int DEFAULT_CAT_LIMIT = 16;

  public static int CAT_LIMIT = DEFAULT_CAT_LIMIT;

  private static GeneralConfig instance;

  public GeneralConfig() {
    super("general.cfg");
  }

  public static void initialize() {
    if (instance == null) {
      instance = new GeneralConfig();
    }
    instance.init();
  }

  public static void reloadConfig() {
    if (instance != null) {
      instance.reload();
    }
  }

  public static GeneralConfig getInstance() {
    return instance;
  }

  @Override
  protected String getHeader() {
    return "Cats Companion - General Configuration";
  }

  @Override
  protected LinkedHashMap<String, String> getDefaults() {
    LinkedHashMap<String, String> defaults = new LinkedHashMap<>();
    defaults.put("cat.limit", String.valueOf(DEFAULT_CAT_LIMIT));
    return defaults;
  }

  @Override
  protected LinkedHashMap<String, String> getComments() {
    LinkedHashMap<String, String> comments = new LinkedHashMap<>();
    comments.put(
        "cat.limit",
        "Default maximum number of tamed cats per player.\nCan be overridden per-player via permissions (markusbordihn.cats.limit.<N>).\nRange: 1 - 64\nDefault: 16");
    return comments;
  }

  @Override
  protected void applyProperties(Properties props) {
    CAT_LIMIT = Math.max(1, Math.min(64, parseInt(props, "cat.limit", DEFAULT_CAT_LIMIT)));
  }

  @Override
  protected void applyDefaults() {
    CAT_LIMIT = DEFAULT_CAT_LIMIT;
  }
}
