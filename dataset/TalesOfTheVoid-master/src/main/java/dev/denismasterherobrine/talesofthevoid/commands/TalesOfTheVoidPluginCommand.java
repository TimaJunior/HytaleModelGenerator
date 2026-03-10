package dev.denismasterherobrine.talesofthevoid.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

/**
 * Main command for TalesOfTheVoid plugin.
 *
 * Usage:
 * - /tales help - Show available commands
 * - /tales info - Show plugin information
 * - /tales reload - Reload plugin configuration
 * - /tales ui - Open the plugin dashboard
 */
public class TalesOfTheVoidPluginCommand extends AbstractCommandCollection {

    public TalesOfTheVoidPluginCommand() {
        super("tales", "TalesOfTheVoid plugin commands");

        // Add subcommands
        this.addSubCommand(new HelpSubCommand());
        this.addSubCommand(new InfoSubCommand());
        this.addSubCommand(new ReloadSubCommand());
        this.addSubCommand(new UISubCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false; // No permission required for base command
    }
}