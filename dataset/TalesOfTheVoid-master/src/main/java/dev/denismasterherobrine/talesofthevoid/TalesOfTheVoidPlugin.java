package dev.denismasterherobrine.talesofthevoid;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;

import dev.denismasterherobrine.talesofthevoid.commands.TalesOfTheVoidPluginCommand;
import dev.denismasterherobrine.talesofthevoid.listeners.PlayerListener;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class TalesOfTheVoidPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static TalesOfTheVoidPlugin instance;

    public TalesOfTheVoidPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    /**
     * Get the plugin instance.
     * @return The plugin instance
     */
    public static TalesOfTheVoidPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("[Tales of the Void] Setting up...");

        // Register commands
        registerCommands();

        // Register event listeners
        registerListeners();

        LOGGER.at(Level.INFO).log("[Tales of the Void] Setup complete!");
    }

    /**
     * Register plugin commands.
     */
    private void registerCommands() {
        try {
            getCommandRegistry().registerCommand(new TalesOfTheVoidPluginCommand());
            LOGGER.at(Level.INFO).log("[Tales of the Void] Registered /tales command");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[Tales of the Void] Failed to register commands");
        }
    }

    /**
     * Register event listeners.
     */
    private void registerListeners() {
        EventRegistry eventBus = getEventRegistry();

        try {
            new PlayerListener().register(eventBus);
            LOGGER.at(Level.INFO).log("[Tales of the Void] Registered player event listeners");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).withCause(e).log("[Tales of the Void] Failed to register listeners");
        }
    }

    @Override
    protected void start() {
        LOGGER.at(Level.INFO).log("[Tales of the Void] Started!");
        LOGGER.at(Level.INFO).log("[Tales of the Void] Use /tales help for commands");
    }

    @Override
    protected void shutdown() {
        LOGGER.at(Level.INFO).log("[Tales of the Void] Shutting down...");
        instance = null;
    }
}