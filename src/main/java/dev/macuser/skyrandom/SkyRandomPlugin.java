package dev.macuser.skyrandom;

import dev.macuser.skyrandom.command.SkyRandomCommand;
import dev.macuser.skyrandom.game.GameManager;
import dev.macuser.skyrandom.listener.GameListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyRandomPlugin extends JavaPlugin {

    private GameManager gameManager;
    private BuildInfo buildInfo;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.buildInfo = BuildInfo.load(this);

        this.gameManager = new GameManager(this);
        this.gameManager.reload();

        PluginCommand command = getCommand("skyrandom");
        if (command != null) {
            SkyRandomCommand executor = new SkyRandomCommand(gameManager);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
    }

    public BuildInfo getBuildInfo() {
        return buildInfo;
    }
}
