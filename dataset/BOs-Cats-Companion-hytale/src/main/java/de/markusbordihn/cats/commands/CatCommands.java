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

package de.markusbordihn.cats.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class CatCommands extends AbstractCommandCollection {
  public CatCommands() {
    super("cat", "Cat management commands");
    this.addAliases("cats");

    this.addSubCommand(new CatPounceCommand());
    this.addSubCommand(new CatBedCommand());
    this.addSubCommand(new CatDespawnCommand());
    this.addSubCommand(new CatFollowCommand());
    this.addSubCommand(new CatInfoCommand());
    this.addSubCommand(new CatListCommand());
    this.addSubCommand(new CatNameCommand());
    this.addSubCommand(new CatOwnerCommand());
    this.addSubCommand(new CatPlayCommand());
    this.addSubCommand(new CatReloadCommand());
    this.addSubCommand(new CatReleaseCommand());
    this.addSubCommand(new CatSearchCommand());
    this.addSubCommand(new CatSitCommand());
    this.addSubCommand(new CatSleepCommand());
    this.addSubCommand(new CatSpawnCommand());
    this.addSubCommand(new CatWaitCommand());
    this.addSubCommand(new CatWanderCommand());
  }
}
