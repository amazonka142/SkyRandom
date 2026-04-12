package dev.macuser.skyrandom.stats;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerStatsStore {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File file;
    private FileConfiguration config;

    public PlayerStatsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), "player-stats.yml");
        load();
    }

    public PlayerStatsSnapshot getStats(UUID playerId) {
        if (playerId == null) {
            return new PlayerStatsSnapshot(0, 0, 0, 0, 0L);
        }

        String base = path(playerId);
        return new PlayerStatsSnapshot(
            Math.max(0, config.getInt(base + ".matches", 0)),
            Math.max(0, config.getInt(base + ".wins", 0)),
            Math.max(0, config.getInt(base + ".kills", 0)),
            Math.max(0, config.getInt(base + ".deaths", 0)),
            Math.max(0L, config.getLong(base + ".total-round-seconds", 0L))
        );
    }

    public void recordMatch(UUID playerId) {
        increment(playerId, "matches");
    }

    public void recordWin(UUID playerId) {
        increment(playerId, "wins");
    }

    public void recordKill(UUID playerId) {
        increment(playerId, "kills");
    }

    public void recordDeath(UUID playerId) {
        increment(playerId, "deaths");
    }

    public void recordRoundDuration(UUID playerId, long durationSeconds) {
        if (playerId == null || durationSeconds <= 0L) {
            return;
        }

        String path = path(playerId) + ".total-round-seconds";
        config.set(path, Math.max(0L, config.getLong(path, 0L)) + durationSeconds);
        save();
    }

    private void increment(UUID playerId, String statKey) {
        if (playerId == null) {
            return;
        }

        String path = path(playerId) + "." + statKey;
        config.set(path, Math.max(0, config.getInt(path, 0)) + 1);
        save();
    }

    private void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                logger.warning("Failed to create " + file.getName() + ": " + exception.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            logger.warning("Failed to save " + file.getName() + ": " + exception.getMessage());
        }
    }

    private String path(UUID playerId) {
        return "players." + playerId;
    }
}
