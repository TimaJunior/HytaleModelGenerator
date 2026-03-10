package dev.denismasterherobrine.talesofthevoid.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/**
 * /tales help - Show available commands
 */
public class HelpSubCommand extends CommandBase {

    public HelpSubCommand() {
        super("help", "Show available commands");
        this.setPermissionGroup(null);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw(""));
        context.sendMessage(Message.raw("=== TalesOfTheVoid Commands ==="));
        context.sendMessage(Message.raw("/tales help - Show this help message"));
        context.sendMessage(Message.raw("/tales info - Show plugin information"));
        context.sendMessage(Message.raw("/tales reload - Reload configuration"));
        context.sendMessage(Message.raw("/tales ui - Open the dashboard UI"));
        context.sendMessage(Message.raw("========================"));
    }
}