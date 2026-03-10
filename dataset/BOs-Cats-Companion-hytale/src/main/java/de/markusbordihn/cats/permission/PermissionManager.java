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

package de.markusbordihn.cats.permission;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.exceptions.NoPermissionException;
import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import de.markusbordihn.cats.config.GeneralConfig;
import javax.annotation.Nonnull;

public class PermissionManager {

  private static final int MAX_CAT_LIMIT = 32;

  private PermissionManager() {}

  public static void checkPermissionAlways(
      @Nonnull CommandContext context, @Nonnull String permission) throws NoPermissionException {
    if (!context.isPlayer()) {
      return;
    }

    if (context.sender() instanceof PermissionHolder permissionHolder) {
      if (!permissionHolder.hasPermission(permission, false)) {
        throw new NoPermissionException(permission);
      }
    } else {
      throw new NoPermissionException(permission);
    }
  }

  public static boolean hasAdminBypass(@Nonnull CommandContext context) {
    if (!context.isPlayer()) {
      return true;
    }

    if (context.sender() instanceof PermissionHolder permissionHolder) {
      return permissionHolder.hasPermission("markusbordihn.cats.admin.bypass");
    }

    return false;
  }

  public static int getCatLimit(@Nonnull PermissionHolder permissionHolder) {
    if (permissionHolder.hasPermission("markusbordihn.cats.limit.unlimited", false)) {
      return -1;
    }

    int maxLimit = 0;
    boolean hasAnyLimit = false;

    for (int i = 1; i <= MAX_CAT_LIMIT; i++) {
      if (permissionHolder.hasPermission("markusbordihn.cats.limit." + i, false)) {
        maxLimit = Math.max(maxLimit, i);
        hasAnyLimit = true;
      }
    }

    return hasAnyLimit ? maxLimit : GeneralConfig.CAT_LIMIT;
  }

  public static int getCatLimit(@Nonnull CommandContext context) {
    if (!context.isPlayer()) {
      return -1;
    }

    if (!(context.sender() instanceof PermissionHolder permissionHolder)) {
      return GeneralConfig.CAT_LIMIT;
    }

    return getCatLimit(permissionHolder);
  }
}
