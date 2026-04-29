package dev.macuser.skyrandom.game;

import dev.macuser.skyrandom.BuildInfo;
import dev.macuser.skyrandom.SkyRandomPlugin;
import dev.macuser.skyrandom.gui.AboutMenuHolder;
import dev.macuser.skyrandom.gui.HostMenuHolder;
import dev.macuser.skyrandom.gui.HostTransferMenuHolder;
import dev.macuser.skyrandom.gui.LanguageMenuHolder;
import dev.macuser.skyrandom.gui.ProfileMenuHolder;
import dev.macuser.skyrandom.gui.StatsMenuHolder;
import dev.macuser.skyrandom.lang.MessageManager;
import dev.macuser.skyrandom.lang.PlayerLanguage;
import dev.macuser.skyrandom.stats.PlayerStatsSnapshot;
import dev.macuser.skyrandom.stats.PlayerStatsStore;
import dev.macuser.skyrandom.world.DefaultMapBuilder;
import dev.macuser.skyrandom.world.VoidChunkGenerator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class GameManager {

    private static final long COMBAT_TAG_MILLIS = 15_000L;
    private static final long DEFAULT_WORLD_DAY_TIME = 6_000L;
    private static final long DEFAULT_WORLD_NIGHT_TIME = 18_000L;
    private static final int DEFAULT_SUDDEN_NIGHT_DELAY_SECONDS = 180;
    private static final int DEFAULT_SUDDEN_NIGHT_DURATION_SECONDS = 180;
    private static final int DROP_INTERVAL_FAST_TICKS = 60;
    private static final int DROP_INTERVAL_CLASSIC_TICKS = 100;
    private static final int DROP_INTERVAL_SLOW_TICKS = 200;
    private static final String DEFAULT_VOID_WORLD_NAME = "skyrandom_void";
    private static final String SAFE_DEMO_WORLD_PREFIX = "skyrandom_";
    private static final String ABOUT_AUTHOR = "amazonka142 (vkkooz)";

    private final SkyRandomPlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, Arena> playerArenas = new HashMap<>();
    private final Set<UUID> randomQueuePlayers = new java.util.HashSet<>();
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Map<UUID, UUID> lastAttackers = new HashMap<>();
    private final Map<UUID, Long> lastAttackTimes = new HashMap<>();
    private final Map<UUID, SuddenNightWorldState> suddenNightStates = new HashMap<>();
    private final MessageManager messages;
    private final PlayerStatsStore statsStore;
    private final NamespacedKey spectatorExitItemKey;
    private final NamespacedKey languageSelectorItemKey;
    private final NamespacedKey languageOptionItemKey;
    private final NamespacedKey profileStatsItemKey;
    private final NamespacedKey profileLanguageItemKey;
    private final NamespacedKey profileAboutItemKey;
    private final NamespacedKey menuBackItemKey;
    private final NamespacedKey lobbyHostItemKey;
    private final NamespacedKey hostMenuToggleSuddenNightItemKey;
    private final NamespacedKey hostMenuOpenSubmenuItemKey;
    private final NamespacedKey hostMenuToggleSuddenNightAlwaysItemKey;
    private final NamespacedKey hostMenuSuddenNightDelayAdjustItemKey;
    private final NamespacedKey hostMenuDropIntervalItemKey;
    private final NamespacedKey hostMenuArenaSelectItemKey;
    private final NamespacedKey hostMenuRoundsAdjustItemKey;
    private final NamespacedKey hostMenuArenaCycleItemKey;
    private final NamespacedKey hostMenuAutostartItemKey;
    private final NamespacedKey hostMenuTransferItemKey;
    private final NamespacedKey hostTransferTargetItemKey;

    private DropTable dropTable = DropTable.empty();
    private Location lobbyLocation;
    private String prefix = ChatColor.AQUA + "[SkyRandom] " + ChatColor.GRAY;
    private boolean restorePlayerStateOnLeave = false;
    private boolean teleportToLobbyOnJoin = true;
    private boolean autoQueueOnJoin = true;
    private boolean dedicatedServerMode = true;
    private boolean soloTestMode = true;
    private boolean suddenNightEnabled = true;
    private boolean suddenNightAlways = false;
    private String defaultArenaId = "random";
    private int lobbyProtectionRadius = 12;
    private int lobbyProtectionBelow = 14;
    private int lobbyProtectionAbove = 6;
    private int arenaBorderWarningDistance = 4;
    private int suddenNightDelaySeconds = DEFAULT_SUDDEN_NIGHT_DELAY_SECONDS;
    private int suddenNightDurationSeconds = DEFAULT_SUDDEN_NIGHT_DURATION_SECONDS;
    private boolean lobbyAutostartEnabled = true;
    private int lobbyRoundsBeforeReset = 10;
    private int lobbyDropIntervalTicks = DROP_INTERVAL_CLASSIC_TICKS;
    private UUID lobbyHostId;
    private final Map<UUID, Long> lobbyHostJoinOrder = new HashMap<>();
    private long nextLobbyJoinOrder = 1L;
    private BukkitTask staticDaylightTask;

    public GameManager(SkyRandomPlugin plugin) {
        this.plugin = plugin;
        this.messages = new MessageManager(plugin);
        this.statsStore = new PlayerStatsStore(plugin);
        this.spectatorExitItemKey = new NamespacedKey(plugin, "spectator_exit_item");
        this.languageSelectorItemKey = new NamespacedKey(plugin, "language_selector_item");
        this.languageOptionItemKey = new NamespacedKey(plugin, "language_option_item");
        this.profileStatsItemKey = new NamespacedKey(plugin, "profile_stats_item");
        this.profileLanguageItemKey = new NamespacedKey(plugin, "profile_language_item");
        this.profileAboutItemKey = new NamespacedKey(plugin, "profile_about_item");
        this.menuBackItemKey = new NamespacedKey(plugin, "menu_back_item");
        this.lobbyHostItemKey = new NamespacedKey(plugin, "lobby_host_item");
        this.hostMenuToggleSuddenNightItemKey = new NamespacedKey(plugin, "host_menu_toggle_sudden_night");
        this.hostMenuOpenSubmenuItemKey = new NamespacedKey(plugin, "host_menu_open_submenu");
        this.hostMenuToggleSuddenNightAlwaysItemKey = new NamespacedKey(plugin, "host_menu_toggle_sudden_night_always");
        this.hostMenuSuddenNightDelayAdjustItemKey = new NamespacedKey(plugin, "host_menu_sudden_night_delay_adjust");
        this.hostMenuDropIntervalItemKey = new NamespacedKey(plugin, "host_menu_drop_interval");
        this.hostMenuArenaSelectItemKey = new NamespacedKey(plugin, "host_menu_arena_select");
        this.hostMenuRoundsAdjustItemKey = new NamespacedKey(plugin, "host_menu_rounds_adjust");
        this.hostMenuArenaCycleItemKey = new NamespacedKey(plugin, "host_menu_arena_cycle");
        this.hostMenuAutostartItemKey = new NamespacedKey(plugin, "host_menu_autostart");
        this.hostMenuTransferItemKey = new NamespacedKey(plugin, "host_menu_transfer");
        this.hostTransferTargetItemKey = new NamespacedKey(plugin, "host_transfer_target");
    }

    public void reload() {
        shutdown();
        plugin.reloadConfig();

        ConfigurationSection settings = plugin.getConfig().getConfigurationSection("settings");
        this.prefix = colorize(settings != null ? settings.getString("prefix", "&b[SkyRandom] &7") : "&b[SkyRandom] &7");
        this.restorePlayerStateOnLeave = settings != null && settings.getBoolean("restore-player-state-on-leave", false);
        this.teleportToLobbyOnJoin = settings == null || settings.getBoolean("teleport-to-lobby-on-join", true);
        this.autoQueueOnJoin = settings == null || settings.getBoolean("auto-queue-on-join", true);
        this.dedicatedServerMode = settings == null || settings.getBoolean("dedicated-server-mode", true);
        this.soloTestMode = settings == null || settings.getBoolean("solo-test-mode", true);
        this.suddenNightEnabled = settings == null || settings.getBoolean("sudden-night-enabled", true);
        this.suddenNightAlways = settings != null && settings.getBoolean("sudden-night-always", false);
        this.defaultArenaId = settings != null ? settings.getString("default-arena", "random") : "random";
        this.lobbyProtectionRadius = settings != null ? Math.max(1, settings.getInt("lobby-protection-radius", 12)) : 12;
        this.lobbyProtectionBelow = settings != null ? Math.max(0, settings.getInt("lobby-protection-below", 14)) : 14;
        this.lobbyProtectionAbove = settings != null ? Math.max(0, settings.getInt("lobby-protection-above", 6)) : 6;
        this.arenaBorderWarningDistance = settings != null ? Math.max(0, settings.getInt("arena-border-warning-distance", 4)) : 4;
        this.suddenNightDelaySeconds = settings != null
            ? Math.max(1, settings.getInt("sudden-night-delay-seconds", DEFAULT_SUDDEN_NIGHT_DELAY_SECONDS))
            : DEFAULT_SUDDEN_NIGHT_DELAY_SECONDS;
        this.suddenNightDurationSeconds = settings != null
            ? Math.max(1, settings.getInt("sudden-night-duration-seconds", DEFAULT_SUDDEN_NIGHT_DURATION_SECONDS))
            : DEFAULT_SUDDEN_NIGHT_DURATION_SECONDS;
        double globalPlayerBoundaryMargin = settings != null ? Math.max(0.0D, settings.getDouble("player-boundary-margin", 12.0D)) : 12.0D;
        double globalPlayerMaxYMargin = settings != null ? Math.max(0.0D, settings.getDouble("player-max-y-margin", 64.0D)) : 64.0D;
        this.messages.reload(settings);

        ensureVoidWorld(settings);
        this.lobbyLocation = parseLocation(plugin.getConfig().getConfigurationSection("lobby"), null);
        this.dropTable = DropTable.fromConfig(plugin.getConfig().getConfigurationSection("loot-table"), plugin.getLogger());

        ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("arenas");
        if (arenasSection == null) {
            plugin.getLogger().warning("No arenas section found in config.yml.");
            return;
        }

        int globalCountdown = settings != null ? settings.getInt("countdown-seconds", 15) : 15;
        int globalDropInterval = settings != null ? settings.getInt("drop-interval-ticks", DROP_INTERVAL_CLASSIC_TICKS) : DROP_INTERVAL_CLASSIC_TICKS;
        this.lobbyDropIntervalTicks = Math.max(1, globalDropInterval);
        int globalWinDelay = settings != null ? settings.getInt("win-delay-seconds", 0) : 0;
        int globalResetWinDelay = settings != null ? settings.getInt("reset-win-delay-seconds", 20) : 20;
        double globalMinY = settings != null ? settings.getDouble("eliminate-below-y", 55.0D) : 55.0D;
        int globalMaxBuildY = settings != null ? settings.getInt("max-build-y", 95) : 95;
        double globalDropHeightOffset = settings != null ? settings.getDouble("drop-height-offset", 5.0D) : 5.0D;
        int globalRoundsBeforeReset = settings != null ? settings.getInt("rounds-before-reset", 10) : 10;
        int globalRoundStartFreezeTicks = settings != null ? settings.getInt("round-start-freeze-ticks", 40) : 40;
        boolean globalAllowPlace = settings == null || settings.getBoolean("allow-place-blocks", true);
        boolean globalAllowBreakMap = settings != null && settings.getBoolean("allow-break-map-blocks", false);
        boolean globalOnlyBreakOwn = settings == null || settings.getBoolean("only-break-own-placed-blocks", true);
        this.lobbyAutostartEnabled = true;
        this.lobbyRoundsBeforeReset = Math.max(1, globalRoundsBeforeReset);

        for (String arenaId : arenasSection.getKeys(false)) {
            ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaId);
            if (arenaSection == null || !arenaSection.getBoolean("enabled", true)) {
                continue;
            }

            String worldName = arenaSection.getString("world");
            World world = worldName == null ? null : Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Arena '" + arenaId + "' skipped because world '" + worldName + "' is missing.");
                continue;
            }

            ConfigurationSection regionSection = arenaSection.getConfigurationSection("region");
            ConfigurationSection minSection = regionSection != null ? regionSection.getConfigurationSection("min") : null;
            ConfigurationSection maxSection = regionSection != null ? regionSection.getConfigurationSection("max") : null;
            if (minSection == null || maxSection == null) {
                plugin.getLogger().warning("Arena '" + arenaId + "' skipped because region.min/max is missing.");
                continue;
            }

            Vector min = parseVector(minSection);
            Vector max = parseVector(maxSection);
            BoundingBox region = BoundingBox.of(min, max);

            List<Location> spawns = new ArrayList<>();
            for (Map<?, ?> spawnMap : arenaSection.getMapList("spawns")) {
                Location spawn = parseLocationMap(spawnMap, worldName);
                if (spawn != null) {
                    spawns.add(spawn);
                }
            }
            if (spawns.isEmpty()) {
                plugin.getLogger().warning("Arena '" + arenaId + "' skipped because it has no valid spawn points.");
                continue;
            }

            int maxPlayers = Math.min(arenaSection.getInt("max-players", spawns.size()), spawns.size());
            int minPlayers = Math.max(1, arenaSection.getInt("min-players", 2));
            if (minPlayers > maxPlayers) {
                minPlayers = maxPlayers;
            }

            Arena arena = new Arena(
                this,
                arenaId,
                colorize(arenaSection.getString("display-name", arenaId)),
                world,
                parseLocation(arenaSection.getConfigurationSection("waiting-spawn"), worldName),
                region,
                spawns,
                minPlayers,
                maxPlayers,
                arenaSection.getInt("countdown-seconds", globalCountdown),
                arenaSection.getInt("drop-interval-ticks", globalDropInterval),
                arenaSection.getInt("win-delay-seconds", globalWinDelay),
                arenaSection.getInt("reset-win-delay-seconds", globalResetWinDelay),
                arenaSection.getDouble("eliminate-below-y", globalMinY),
                arenaSection.getInt("max-build-y", globalMaxBuildY),
                arenaSection.getDouble("drop-height-offset", globalDropHeightOffset),
                arenaSection.getBoolean("allow-place-blocks", globalAllowPlace),
                arenaSection.getBoolean("allow-break-map-blocks", globalAllowBreakMap),
                arenaSection.getBoolean("only-break-own-placed-blocks", globalOnlyBreakOwn),
                arenaSection.getInt("rounds-before-reset", lobbyRoundsBeforeReset),
                arenaSection.getInt("round-start-freeze-ticks", globalRoundStartFreezeTicks),
                arenaSection.getDouble("player-boundary-margin", globalPlayerBoundaryMargin),
                arenaSection.getDouble("player-max-y-margin", globalPlayerMaxYMargin)
            );
            arenas.put(arenaId.toLowerCase(Locale.ROOT), arena);
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
        if (!suddenNightEnabled) {
            enforceStaticDaylight();
        }
        startStaticDaylightTask();
        restoreOnlinePlayersAfterReload();
    }

    public void shutdown() {
        for (Arena arena : arenas.values()) {
            arena.forceStop("system.match_stopped");
        }
        clearSuddenNightStates();
        stopStaticDaylightTask();
        arenas.clear();
        playerArenas.clear();
        randomQueuePlayers.clear();
        snapshots.clear();
        lastAttackers.clear();
        lastAttackTimes.clear();
    }

    public Collection<Arena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public Set<String> getArenaIds() {
        return arenas.keySet();
    }

    public DropTable getDropTable() {
        return dropTable;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getDefaultArenaId() {
        return defaultArenaId;
    }

    public boolean isDedicatedServerMode() {
        return dedicatedServerMode;
    }

    public boolean isSoloTestMode() {
        return soloTestMode;
    }

    public boolean isLobbyAutostartEnabled() {
        return lobbyAutostartEnabled;
    }

    public int getLobbyRoundsBeforeReset() {
        return lobbyRoundsBeforeReset;
    }

    public boolean isLobbyHost(Player player) {
        return player != null && lobbyHostId != null && lobbyHostId.equals(player.getUniqueId());
    }

    public Location getLobbyOrFallback(Location fallback) {
        if (lobbyLocation != null) {
            return lobbyLocation.clone();
        }
        return fallback != null ? fallback.clone() : null;
    }

    public boolean isInsideProtectedLobbyZone(Location location) {
        if (lobbyLocation == null || location == null || location.getWorld() == null || lobbyLocation.getWorld() == null) {
            return false;
        }
        if (!lobbyLocation.getWorld().getUID().equals(location.getWorld().getUID())) {
            return false;
        }

        double dx = location.getX() - lobbyLocation.getX();
        double dz = location.getZ() - lobbyLocation.getZ();
        if ((dx * dx) + (dz * dz) > (double) (lobbyProtectionRadius * lobbyProtectionRadius)) {
            return false;
        }

        double minY = lobbyLocation.getY() - lobbyProtectionBelow;
        double maxY = lobbyLocation.getY() + lobbyProtectionAbove;
        return location.getY() >= minY && location.getY() <= maxY;
    }

    public PlayerLanguage getLanguage(CommandSender sender) {
        return messages.getLanguage(sender);
    }

    public PlayerLanguage getLanguage(Player player) {
        return messages.getLanguage(player);
    }

    public String tr(CommandSender sender, String key, Object... replacements) {
        return messages.get(sender, key, replacements);
    }

    public String tr(PlayerLanguage language, String key, Object... replacements) {
        return messages.get(language, key, replacements);
    }

    public java.util.List<String> trList(CommandSender sender, String key, Object... replacements) {
        return messages.getList(sender, key, replacements);
    }

    public void sendLocalized(CommandSender sender, String key, Object... replacements) {
        send(sender, tr(sender, key, replacements));
    }

    public PlayerStatsSnapshot getPlayerStats(Player player) {
        return player == null ? new PlayerStatsSnapshot(0, 0, 0, 0, 0L) : statsStore.getStats(player.getUniqueId());
    }

    public void recordMatch(Player player) {
        if (player != null) {
            statsStore.recordMatch(player.getUniqueId());
        }
    }

    public void recordWin(Player player) {
        if (player != null) {
            statsStore.recordWin(player.getUniqueId());
        }
    }

    public void recordKill(Player player) {
        if (player != null) {
            statsStore.recordKill(player.getUniqueId());
        }
    }

    public void recordDeath(Player player) {
        if (player != null) {
            statsStore.recordDeath(player.getUniqueId());
        }
    }

    public void recordRoundDuration(UUID playerId, long durationSeconds) {
        statsStore.recordRoundDuration(playerId, durationSeconds);
    }

    public String getStateDisplay(CommandSender sender, GameState state) {
        return switch (state) {
            case WAITING -> tr(sender, "state.waiting");
            case COUNTDOWN -> tr(sender, "state.countdown");
            case RUNNING -> tr(sender, "state.running");
            case ENDING -> tr(sender, "state.ending");
        };
    }

    public void joinArena(Player player, String arenaId) {
        Arena existingArena = getArena(player);
        if (existingArena != null) {
            sendLocalized(player, "manager.already_in_arena", "arena", existingArena.getDisplayName());
            return;
        }

        String requestedArenaId = arenaId == null || arenaId.isBlank() ? defaultArenaId : arenaId;
        Arena arena = resolveArenaForJoin(arenaId);
        if (arena == null) {
            if (requestedArenaId == null || requestedArenaId.isBlank() || "random".equalsIgnoreCase(requestedArenaId)) {
                sendLocalized(player, "manager.no_random_arena");
            } else {
                sendLocalized(player, "manager.arena_not_found", "arena", requestedArenaId);
            }
            return;
        }

        updateQueuePreference(player.getUniqueId(), requestedArenaId);
        arena.join(player);
    }

    public void leaveArena(Player player, String reason) {
        Arena arena = getArena(player);
        if (arena == null) {
            sendLocalized(player, "manager.not_in_arena");
            return;
        }

        arena.leave(player, reason);
    }

    public void forceStart(String arenaId, CommandSender sender) {
        Arena arena = arenas.get(arenaId.toLowerCase(Locale.ROOT));
        if (arena == null) {
            sendLocalized(sender, "manager.arena_not_found", "arena", arenaId);
            return;
        }
        arena.forceStart(sender);
    }

    public void rebuildArena(String arenaId, CommandSender sender) {
        Arena arena = arenas.get(arenaId.toLowerCase(Locale.ROOT));
        if (arena == null) {
            sendLocalized(sender, "manager.arena_not_found", "arena", arenaId);
            return;
        }

        ConfigurationSection settings = plugin.getConfig().getConfigurationSection("settings");
        boolean autoBuildDefaultMap = settings == null || settings.getBoolean("auto-build-default-map", true);
        String voidWorldName = settings != null ? settings.getString("void-world-name", DEFAULT_VOID_WORLD_NAME) : DEFAULT_VOID_WORLD_NAME;
        boolean allowUnsafeDemoBuildOverride = isUnsafeDemoBuildOverrideAllowed(settings);

        if (!autoBuildDefaultMap || voidWorldName == null || !arena.getWorld().getName().equalsIgnoreCase(voidWorldName)) {
            sendLocalized(sender, "manager.rebuild_demo_only", "world", voidWorldName);
            return;
        }

        if (!isSafeDemoBuildWorld(voidWorldName, allowUnsafeDemoBuildOverride)) {
            sendLocalized(sender, "manager.unsafe_demo_build_blocked", "world", voidWorldName, "prefix", SAFE_DEMO_WORLD_PREFIX);
            logUnsafeDemoBuildBlocked(voidWorldName);
            return;
        }

        shutdown();
        DefaultMapBuilder.ensureBuilt(arena.getWorld());
        reload();
        sendLocalized(sender, "manager.demo_rebuilt", "arena", arena.getDisplayName());
    }

    public Arena getArena(Player player) {
        return playerArenas.get(player.getUniqueId());
    }

    public Arena findArenaByLocation(Location location) {
        for (Arena arena : arenas.values()) {
            if (arena.contains(location)) {
                return arena;
            }
        }
        return null;
    }

    public void captureSnapshot(Player player) {
        if (!restorePlayerStateOnLeave) {
            return;
        }
        snapshots.putIfAbsent(player.getUniqueId(), PlayerSnapshot.capture(player));
    }

    public void prepareQueuedPlayer(Player player, Location waitingLocation) {
        clearPlayer(player);
        clearPlayerWorldBorder(player);
        clearRecentAttacker(player.getUniqueId());
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(getSafeMaxHealth(player));
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        Location target = waitingLocation != null ? waitingLocation : getLobbyOrFallback(player.getLocation());
        if (target != null && target.getWorld() != null) {
            player.teleport(target);
        }
        giveLobbyUtilityItems(player);
    }

    public void prepareMatchPlayer(Player player) {
        clearPlayer(player);
        clearRecentAttacker(player.getUniqueId());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(getSafeMaxHealth(player));
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    public void prepareSpectatorPlayer(Player player, Location spectateLocation) {
        clearPlayer(player);
        clearRecentAttacker(player.getUniqueId());
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(getSafeMaxHealth(player));
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0.09F);
        player.setCollidable(false);
        player.setCanPickupItems(false);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));

        Location target = spectateLocation != null ? spectateLocation : player.getLocation();
        if (target != null && target.getWorld() != null) {
            player.teleport(target);
        }

        player.getInventory().setItem(8, createSpectatorExitItem(player));
    }

    public void restorePlayer(Player player, Location fallback) {
        clearPlayerWorldBorder(player);
        if (!restorePlayerStateOnLeave) {
            clearPlayer(player);
            player.setGameMode(dedicatedServerMode ? GameMode.ADVENTURE : GameMode.SURVIVAL);
            player.setHealth(getSafeMaxHealth(player));
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            resetMainScoreboard(player);
            Location exit = getLobbyOrFallback(fallback);
            if (exit != null && exit.getWorld() != null) {
                player.teleport(exit);
            }
            giveLobbyUtilityItems(player);
            return;
        }

        PlayerSnapshot snapshot = snapshots.remove(player.getUniqueId());
        if (snapshot == null) {
            if (fallback != null && fallback.getWorld() != null) {
                player.teleport(fallback);
            }
            clearPlayer(player);
            player.setGameMode(GameMode.SURVIVAL);
            resetMainScoreboard(player);
            if (dedicatedServerMode) {
                giveLobbyUtilityItems(player);
            }
            return;
        }

        Location exit = getLobbyOrFallback(fallback);
        snapshot.restore(player, exit);
    }

    public void discardSnapshot(UUID uuid) {
        snapshots.remove(uuid);
    }

    public void bindPlayer(Player player, Arena arena) {
        playerArenas.put(player.getUniqueId(), arena);
    }

    public void unbindPlayer(Player player) {
        playerArenas.remove(player.getUniqueId());
        randomQueuePlayers.remove(player.getUniqueId());
    }

    public void unbindOffline(UUID uuid) {
        playerArenas.remove(uuid);
        randomQueuePlayers.remove(uuid);
    }

    public boolean isRandomQueuePlayer(UUID uuid) {
        return randomQueuePlayers.contains(uuid);
    }

    public void rotateRandomQueuePlayers(Arena currentArena) {
        List<UUID> playersToMove = currentArena.getQueuedPlayersSnapshot().stream()
            .filter(randomQueuePlayers::contains)
            .toList();
        if (playersToMove.isEmpty()) {
            return;
        }

        Arena nextArena = selectRotationArena(currentArena, playersToMove.size());
        if (nextArena == null) {
            return;
        }

        currentArena.transferQueuedPlayersTo(nextArena, playersToMove);
    }

    public boolean startSuddenNightSession(Arena arena) {
        if (!suddenNightEnabled || arena == null || arena.getWorld() == null) {
            return false;
        }

        UUID worldId = arena.getWorld().getUID();
        // One world can be reused by different arenas, so keep one shared sudden-night state per world.
        SuddenNightWorldState state = suddenNightStates.computeIfAbsent(worldId, ignored -> new SuddenNightWorldState(arena.getWorld()));
        state.activeSessions++;
        if (suddenNightAlways) {
            if (state.completedCycle) {
                return true;
            }
            if (state.startTask != null) {
                state.startTask.cancel();
                state.startTask = null;
            }
            if (state.nightTask != null) {
                state.nightTask.cancel();
                state.nightTask = null;
            }
            state.completedCycle = true;
            state.world.setTime(DEFAULT_WORLD_NIGHT_TIME);
            state.world.setStorm(false);
            announceSuddenNight(state.world, true);
            return true;
        }

        // If the cycle is already running for this world, do not schedule it again.
        if (state.activeSessions > 1 || state.completedCycle || state.startTask != null || state.nightTask != null) {
            return true;
        }

        // Start from daytime, then switch later.
        resetWorldToDay(state.world);
        state.startTask = plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> startSuddenNight(worldId),
            suddenNightDelaySeconds * 20L
        );
        return true;
    }

    public void endSuddenNightSession(Arena arena) {
        if (arena == null || arena.getWorld() == null) {
            return;
        }

        UUID worldId = arena.getWorld().getUID();
        SuddenNightWorldState state = suddenNightStates.get(worldId);
        if (state == null) {
            resetWorldToDay(arena.getWorld());
            return;
        }

        state.activeSessions = Math.max(0, state.activeSessions - 1);
        if (state.activeSessions > 0) {
            return;
        }

        cancelSuddenNightState(state, true);
        suddenNightStates.remove(worldId);
    }

    public void handleQuit(Player player) {
        Arena arena = getArena(player);
        if (arena != null) {
            arena.leave(player, "reason.server_leave");
        }
        unregisterLobbyHostJoin(player);
        handleLobbyHostDeparture(player);
    }

    public void handleJoin(Player player) {
        registerLobbyHostJoin(player);
        ensureLobbyHostAssigned(player);
        forceLobbyFlow(player, true);
    }

    public void handleRespawn(Player player) {
        forceLobbyFlow(player, false);
    }

    public void applyArenaWorldBorder(Player player, Arena arena) {
        if (player == null || arena == null) {
            return;
        }

        WorldBorder border = plugin.getServer().createWorldBorder();
        border.setCenter(arena.getBorderCenterX(), arena.getBorderCenterZ());
        border.setSize(Math.max(1.0D, arena.getBorderSize()));
        border.setDamageBuffer(0.0D);
        border.setDamageAmount(0.0D);
        border.setWarningDistance(arenaBorderWarningDistance);
        player.setWorldBorder(border);
    }

    public void clearPlayerWorldBorder(Player player) {
        if (player == null) {
            return;
        }
        player.setWorldBorder(null);
    }

    private void forceLobbyFlow(Player player, boolean autoQueue) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            clearPlayerWorldBorder(player);
            if (teleportToLobbyOnJoin) {
                clearPlayer(player);
                player.setGameMode(dedicatedServerMode ? GameMode.ADVENTURE : GameMode.SURVIVAL);
                resetMainScoreboard(player);
                Location target = getLobbyOrFallback(player.getWorld().getSpawnLocation());
                if (target != null && target.getWorld() != null) {
                    player.teleport(target);
                }
                giveLobbyUtilityItems(player);
            }

            if (autoQueue && autoQueueOnJoin && defaultArenaId != null && !defaultArenaId.isBlank()) {
                joinArena(player, defaultArenaId);
            }
        }, 2L);
    }

    private void restoreOnlinePlayersAfterReload() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                registerLobbyHostJoin(player);
                if (getArena(player) == null) {
                    forceLobbyFlow(player, true);
                }
            }
            ensureLobbyHostAssigned(null);
            refreshLobbyUtilityItemsForOnlinePlayers();
        }, 1L);
    }

    private void startSuddenNight(UUID worldId) {
        SuddenNightWorldState state = suddenNightStates.get(worldId);
        if (state == null) {
            return;
        }

        state.startTask = null;
        if (state.activeSessions <= 0) {
            cancelSuddenNightState(state, true);
            suddenNightStates.remove(worldId);
            return;
        }

        state.completedCycle = true;
        World world = state.world;
        if (world == null) {
            suddenNightStates.remove(worldId);
            return;
        }

        world.setTime(DEFAULT_WORLD_NIGHT_TIME);
        world.setStorm(false);
        announceSuddenNight(world, true);

        state.nightTask = plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> endSuddenNight(worldId),
            suddenNightDurationSeconds * 20L
        );
    }

    private void endSuddenNight(UUID worldId) {
        SuddenNightWorldState state = suddenNightStates.get(worldId);
        if (state == null) {
            return;
        }

        state.nightTask = null;
        resetWorldToDay(state.world);
        announceSuddenNight(state.world, false);

        if (state.activeSessions <= 0) {
            suddenNightStates.remove(worldId);
        }
    }

    private void announceSuddenNight(World world, boolean started) {
        if (world == null) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!world.getUID().equals(player.getWorld().getUID()) || getArena(player) == null) {
                continue;
            }

            if (started) {
                sendLocalized(player, "arena.sudden_night_started");
                player.sendTitle(
                    tr(player, "arena.sudden_night_title"),
                    tr(player, "arena.sudden_night_subtitle"),
                    10,
                    60,
                    10
                );
            } else {
                sendLocalized(player, "arena.sudden_night_ended");
                player.sendTitle(
                    tr(player, "arena.day_returns_title"),
                    tr(player, "arena.day_returns_subtitle"),
                    10,
                    50,
                    10
                );
            }
        }
    }

    private void clearSuddenNightStates() {
        for (SuddenNightWorldState state : suddenNightStates.values()) {
            cancelSuddenNightState(state, true);
        }
        suddenNightStates.clear();
    }

    private void startStaticDaylightTask() {
        stopStaticDaylightTask();
        staticDaylightTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::enforceStaticDaylight,
            1L,
            5L
        );
    }

    private void stopStaticDaylightTask() {
        if (staticDaylightTask != null) {
            staticDaylightTask.cancel();
            staticDaylightTask = null;
        }
    }

    private void enforceStaticDaylight() {
        Set<World> worlds = new java.util.HashSet<>();
        if (lobbyLocation != null && lobbyLocation.getWorld() != null) {
            worlds.add(lobbyLocation.getWorld());
        }
        for (Arena arena : arenas.values()) {
            World world = arena.getWorld();
            if (world != null) {
                worlds.add(world);
            }
        }
        for (World world : worlds) {
            if (hasActiveSuddenNight(world)) {
                continue;
            }
            resetWorldToDay(world);
        }
    }

    private boolean hasActiveSuddenNight(World world) {
        if (world == null) {
            return false;
        }
        SuddenNightWorldState state = suddenNightStates.get(world.getUID());
        return state != null && (state.nightTask != null || (suddenNightAlways && state.completedCycle && state.activeSessions > 0));
    }

    private void cancelSuddenNightState(SuddenNightWorldState state, boolean resetDay) {
        if (state == null) {
            return;
        }
        if (state.startTask != null) {
            state.startTask.cancel();
            state.startTask = null;
        }
        if (state.nightTask != null) {
            state.nightTask.cancel();
            state.nightTask = null;
        }
        if (resetDay) {
            resetWorldToDay(state.world);
        }
    }

    private void resetWorldToDay(World world) {
        if (world == null) {
            return;
        }
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setTime(DEFAULT_WORLD_DAY_TIME);
        world.setStorm(false);
    }

    public void filterExplosion(List<Block> blocks, Location location) {
        for (Arena arena : arenas.values()) {
            if (arena.isWorld(location.getWorld())) {
                arena.filterExplosion(blocks);
            }
        }
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(prefix + colorize(message));
    }

    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public void recordPlayerAttack(Player victim, Player attacker) {
        if (victim == null || attacker == null || victim.getUniqueId().equals(attacker.getUniqueId())) {
            return;
        }
        lastAttackers.put(victim.getUniqueId(), attacker.getUniqueId());
        lastAttackTimes.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    public Player findRecentAttacker(Player victim) {
        if (victim == null) {
            return null;
        }

        UUID victimId = victim.getUniqueId();
        Long attackTime = lastAttackTimes.get(victimId);
        if (attackTime != null && System.currentTimeMillis() - attackTime <= COMBAT_TAG_MILLIS) {
            UUID attackerId = lastAttackers.get(victimId);
            if (attackerId != null && !attackerId.equals(victimId)) {
                Player attacker = Bukkit.getPlayer(attackerId);
                if (attacker != null && attacker.isOnline() && getArena(attacker) == getArena(victim)) {
                    return attacker;
                }
            }
        }

        Player killer = victim.getKiller();
        if (killer != null && killer.isOnline() && !killer.getUniqueId().equals(victimId) && getArena(killer) == getArena(victim)) {
            return killer;
        }
        return null;
    }

    public void clearRecentAttacker(UUID victimId) {
        lastAttackers.remove(victimId);
        lastAttackTimes.remove(victimId);
    }

    public boolean isSpectatorExitItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(spectatorExitItemKey, PersistentDataType.BYTE);
    }

    public boolean isLanguageSelectorItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(languageSelectorItemKey, PersistentDataType.BYTE);
    }

    public boolean isLobbyHostItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(lobbyHostItemKey, PersistentDataType.BYTE);
    }

    public boolean isProfileMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof ProfileMenuHolder;
    }

    public boolean isStatsMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof StatsMenuHolder;
    }

    public boolean isLanguageMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof LanguageMenuHolder;
    }

    public boolean isAboutMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof AboutMenuHolder;
    }

    public boolean isHostMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof HostMenuHolder;
    }

    public boolean isHostTransferMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof HostTransferMenuHolder;
    }

    public void openProfileMenu(Player player) {
        ProfileMenuHolder holder = new ProfileMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, tr(player, "profile.menu_title"));
        holder.setInventory(inventory);
        inventory.setItem(10, createProfileStatsItem(player));
        inventory.setItem(13, createProfileAboutItem(player));
        inventory.setItem(16, createProfileLanguageItem(player));
        player.openInventory(inventory);
    }

    public void handleProfileMenuClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        if (meta.getPersistentDataContainer().has(profileStatsItemKey, PersistentDataType.BYTE)) {
            openStatsMenu(player);
            return;
        }
        if (meta.getPersistentDataContainer().has(profileAboutItemKey, PersistentDataType.BYTE)) {
            openAboutMenu(player);
            return;
        }
        if (meta.getPersistentDataContainer().has(profileLanguageItemKey, PersistentDataType.BYTE)) {
            openLanguageMenu(player);
        }
    }

    public void openStatsMenu(Player player) {
        StatsMenuHolder holder = new StatsMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, tr(player, "profile.stats_menu_title"));
        holder.setInventory(inventory);
        inventory.setItem(13, createStatsDisplayItem(player));
        inventory.setItem(22, createBackItem(player));
        player.openInventory(inventory);
    }

    public void openLanguageMenu(Player player) {
        LanguageMenuHolder holder = new LanguageMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, tr(player, "language.menu_title"));
        holder.setInventory(inventory);
        inventory.setItem(11, createLanguageOptionItem(player, PlayerLanguage.RU, Material.WHITE_BANNER));
        inventory.setItem(15, createLanguageOptionItem(player, PlayerLanguage.EN, Material.BLUE_BANNER));
        inventory.setItem(22, createBackItem(player));
        player.openInventory(inventory);
    }

    public void openAboutMenu(Player player) {
        AboutMenuHolder holder = new AboutMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, tr(player, "profile.about_menu_title"));
        holder.setInventory(inventory);
        inventory.setItem(13, createAboutDisplayItem(player));
        inventory.setItem(22, createBackItem(player));
        player.openInventory(inventory);
    }

    public void handleStatsMenuClick(Player player, ItemStack clickedItem) {
        if (isBackItem(clickedItem)) {
            openProfileMenu(player);
        }
    }

    public void handleAboutMenuClick(Player player, ItemStack clickedItem) {
        if (isBackItem(clickedItem)) {
            openProfileMenu(player);
        }
    }

    public void openHostMenu(Player player) {
        if (!isLobbyHost(player)) {
            sendLocalized(player, "host.not_host");
            return;
        }

        HostMenuHolder holder = new HostMenuHolder(HostMenuHolder.Section.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 45, tr(player, "host.menu_title"));
        holder.setInventory(inventory);
        inventory.setItem(10, createHostMapMenuItem(player));
        inventory.setItem(12, createHostRoundsAdjustItem(player, -1));
        inventory.setItem(13, createHostRoundsDisplayItem(player));
        inventory.setItem(14, createHostRoundsAdjustItem(player, 1));
        inventory.setItem(28, createHostSuddenNightMenuItem(player));
        inventory.setItem(30, createHostGameSpeedMenuItem(player));
        inventory.setItem(32, createHostAutostartItem(player));
        inventory.setItem(31, createHostTransferItem(player));
        inventory.setItem(40, createBackItem(player));
        player.openInventory(inventory);
    }

    public void openHostSuddenNightMenu(Player player) {
        if (!isLobbyHost(player)) {
            sendLocalized(player, "host.not_host");
            return;
        }

        HostMenuHolder holder = new HostMenuHolder(HostMenuHolder.Section.SUDDEN_NIGHT);
        Inventory inventory = Bukkit.createInventory(holder, 27, tr(player, "host.sudden_night_menu_title"));
        holder.setInventory(inventory);
        inventory.setItem(10, createHostSuddenNightToggleItem(player));
        inventory.setItem(12, createHostSuddenNightDelayAdjustItem(player, -1));
        inventory.setItem(13, createHostSuddenNightDelayDisplayItem(player));
        inventory.setItem(14, createHostSuddenNightDelayAdjustItem(player, 1));
        inventory.setItem(16, createHostSuddenNightAlwaysItem(player));
        inventory.setItem(22, createBackItem(player));
        player.openInventory(inventory);
    }

    public void openHostGameSpeedMenu(Player player) {
        if (!isLobbyHost(player)) {
            sendLocalized(player, "host.not_host");
            return;
        }

        HostMenuHolder holder = new HostMenuHolder(HostMenuHolder.Section.GAME_SPEED);
        Inventory inventory = Bukkit.createInventory(holder, 27, tr(player, "host.speed_menu_title"));
        holder.setInventory(inventory);
        inventory.setItem(11, createHostDropIntervalItem(player, DROP_INTERVAL_FAST_TICKS, "host.speed_fast_name"));
        inventory.setItem(13, createHostDropIntervalItem(player, DROP_INTERVAL_CLASSIC_TICKS, "host.speed_classic_name"));
        inventory.setItem(15, createHostDropIntervalItem(player, DROP_INTERVAL_SLOW_TICKS, "host.speed_slow_name"));
        inventory.setItem(22, createBackItem(player));
        player.openInventory(inventory);
    }

    public void openHostMapSelectionMenu(Player player) {
        if (!isLobbyHost(player)) {
            sendLocalized(player, "host.not_host");
            return;
        }

        HostMenuHolder holder = new HostMenuHolder(HostMenuHolder.Section.MAP_SELECTION);
        Inventory inventory = Bukkit.createInventory(holder, 27, tr(player, "host.map_menu_title"));
        holder.setInventory(inventory);
        inventory.setItem(10, createHostMapSelectionItem(player, null));

        int slot = 11;
        for (Arena arena : arenas.values()) {
            if (slot >= 17) {
                break;
            }
            inventory.setItem(slot, createHostMapSelectionItem(player, arena));
            slot++;
        }

        inventory.setItem(22, createBackItem(player));
        player.openInventory(inventory);
    }

    public void openHostTransferMenu(Player player) {
        if (!isLobbyHost(player)) {
            sendLocalized(player, "host.not_host");
            return;
        }

        HostTransferMenuHolder holder = new HostTransferMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, tr(player, "host.transfer_menu_title"));
        holder.setInventory(inventory);

        // Oldest lobby joins first.
        // Bukkit gives ? extends Player here, so copy it into a plain List<Player>.
        List<Player> targets = new ArrayList<>(
            Bukkit.getOnlinePlayers().stream()
                .filter(Player::isOnline)
                .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
                .sorted(
                    // Name is just a stable fallback when join order matches.
                    Comparator.comparingLong((Player target) -> lobbyHostJoinOrder.getOrDefault(target.getUniqueId(), Long.MAX_VALUE))
                        .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER)
                )
                .toList()
        );

        if (targets.isEmpty()) {
            inventory.setItem(22, createHostTransferEmptyItem(player));
        } else {
            int slot = 0;
            for (Player target : targets) {
                // Keep the last row free for controls.
                if (slot >= 45) {
                    break;
                }
                inventory.setItem(slot, createHostTransferTargetItem(player, target));
                slot++;
            }
        }

        inventory.setItem(49, createBackItem(player));
        player.openInventory(inventory);
    }

    public void handleLanguageMenuClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        if (meta.getPersistentDataContainer().has(menuBackItemKey, PersistentDataType.BYTE)) {
            openProfileMenu(player);
            return;
        }

        String code = meta.getPersistentDataContainer().get(languageOptionItemKey, PersistentDataType.STRING);
        if (code == null || code.isBlank()) {
            return;
        }

        PlayerLanguage selected = PlayerLanguage.fromCode(code);
        messages.setLanguage(player, selected);
        refreshLocalizedPlayerState(player);
        player.closeInventory();
        sendLocalized(player, "language.changed", "language", tr(selected, "language.option_" + selected.getCode()));
    }

    public void handleHostMenuClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }
        if (!isLobbyHost(player)) {
            player.closeInventory();
            sendLocalized(player, "host.not_host");
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        HostMenuHolder.Section section = HostMenuHolder.Section.MAIN;
        Inventory openInventory = player.getOpenInventory().getTopInventory();
        if (openInventory != null && openInventory.getHolder() instanceof HostMenuHolder holder) {
            section = holder.getSection();
        }

        if (meta.getPersistentDataContainer().has(menuBackItemKey, PersistentDataType.BYTE)) {
            if (section == HostMenuHolder.Section.MAIN) {
                player.closeInventory();
            } else {
                openHostMenu(player);
            }
            return;
        }

        String submenu = meta.getPersistentDataContainer().get(hostMenuOpenSubmenuItemKey, PersistentDataType.STRING);
        if (submenu != null) {
            switch (submenu) {
                case "sudden_night" -> openHostSuddenNightMenu(player);
                case "game_speed" -> openHostGameSpeedMenu(player);
                case "map_selection" -> openHostMapSelectionMenu(player);
                default -> {
                }
            }
            return;
        }

        // Menu actions are identified by metadata markers on the clicked item.
        if (meta.getPersistentDataContainer().has(hostMenuToggleSuddenNightItemKey, PersistentDataType.BYTE)) {
            suddenNightEnabled = !suddenNightEnabled;
            if (!suddenNightEnabled) {
                clearSuddenNightStates();
                enforceStaticDaylight();
            }
            refreshSuddenNightSessions();
            sendLocalized(player, "host.sudden_night_changed", "status", getHostStatusText(player, suddenNightEnabled));
            openHostSuddenNightMenu(player);
            return;
        }

        if (meta.getPersistentDataContainer().has(hostMenuToggleSuddenNightAlwaysItemKey, PersistentDataType.BYTE)) {
            suddenNightAlways = !suddenNightAlways;
            refreshSuddenNightSessions();
            sendLocalized(player, "host.sudden_night_always_changed", "status", getHostStatusText(player, suddenNightAlways));
            openHostSuddenNightMenu(player);
            return;
        }

        Integer suddenNightDelayDelta = meta.getPersistentDataContainer().get(hostMenuSuddenNightDelayAdjustItemKey, PersistentDataType.INTEGER);
        if (suddenNightDelayDelta != null) {
            int minutes = Math.max(1, getSuddenNightDelayMinutes() + suddenNightDelayDelta);
            suddenNightDelaySeconds = minutes * 60;
            refreshSuddenNightSessions();
            sendLocalized(player, "host.sudden_night_delay_changed", "minutes", minutes);
            openHostSuddenNightMenu(player);
            return;
        }

        Integer dropIntervalTicks = meta.getPersistentDataContainer().get(hostMenuDropIntervalItemKey, PersistentDataType.INTEGER);
        if (dropIntervalTicks != null) {
            setLobbyDropIntervalTicks(dropIntervalTicks);
            sendLocalized(player, "host.speed_changed", "speed", getDropIntervalDisplay(player, lobbyDropIntervalTicks));
            openHostGameSpeedMenu(player);
            return;
        }

        String selectedArenaId = meta.getPersistentDataContainer().get(hostMenuArenaSelectItemKey, PersistentDataType.STRING);
        if (selectedArenaId != null) {
            setLobbyArenaSelection(selectedArenaId);
            sendLocalized(player, "host.map_changed", "map", getLobbyMapDisplay(player));
            openHostMapSelectionMenu(player);
            return;
        }

        Integer roundsDelta = meta.getPersistentDataContainer().get(hostMenuRoundsAdjustItemKey, PersistentDataType.INTEGER);
        if (roundsDelta != null) {
            setLobbyRoundsBeforeReset(Math.max(1, lobbyRoundsBeforeReset + roundsDelta));
            sendLocalized(player, "host.rounds_changed", "value", lobbyRoundsBeforeReset);
            openHostMenu(player);
            return;
        }

        if (meta.getPersistentDataContainer().has(hostMenuArenaCycleItemKey, PersistentDataType.BYTE)) {
            cycleLobbyArena();
            sendLocalized(player, "host.map_changed", "map", getLobbyMapDisplay(player));
            openHostMapSelectionMenu(player);
            return;
        }

        if (meta.getPersistentDataContainer().has(hostMenuAutostartItemKey, PersistentDataType.BYTE)) {
            setLobbyAutostartEnabled(!lobbyAutostartEnabled);
            sendLocalized(player, "host.autostart_changed", "status", getHostStatusText(player, lobbyAutostartEnabled));
            openHostMenu(player);
            return;
        }

        if (meta.getPersistentDataContainer().has(hostMenuTransferItemKey, PersistentDataType.BYTE)) {
            openHostTransferMenu(player);
        }
    }

    public void handleHostTransferMenuClick(Player player, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }
        if (!isLobbyHost(player)) {
            player.closeInventory();
            sendLocalized(player, "host.not_host");
            return;
        }
        if (isBackItem(clickedItem)) {
            openHostMenu(player);
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) {
            return;
        }

        // Target player id is stored on the item itself.
        String rawTargetId = meta.getPersistentDataContainer().get(hostTransferTargetItemKey, PersistentDataType.STRING);
        if (rawTargetId == null || rawTargetId.isBlank()) {
            return;
        }

        UUID targetId;
        try {
            targetId = UUID.fromString(rawTargetId);
        } catch (IllegalArgumentException exception) {
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            sendLocalized(player, "host.transfer_target_unavailable");
            openHostTransferMenu(player);
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            return;
        }

        lobbyHostId = target.getUniqueId();
        refreshLobbyUtilityItemsForOnlinePlayers();
        player.closeInventory();
        sendLocalized(player, "host.transfer_sent", "player", target.getName());
        sendLocalized(target, "host.transfer_received", "player", player.getName());
    }

    public void refreshLocalizedPlayerState(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Arena arena = getArena(player);
            if (arena != null) {
                arena.refreshPlayerLocale(player);
                if (arena.isSpectator(player)) {
                    player.getInventory().setItem(8, createSpectatorExitItem(player));
                } else if (!arena.isActivePlayer(player)) {
                    giveLobbyUtilityItems(player);
                }
                return;
        }

        giveLobbyUtilityItems(player);
    }

    private void registerLobbyHostJoin(Player player) {
        if (player == null) {
            return;
        }
        lobbyHostJoinOrder.putIfAbsent(player.getUniqueId(), nextLobbyJoinOrder++);
    }

    private void unregisterLobbyHostJoin(Player player) {
        if (player == null) {
            return;
        }
        lobbyHostJoinOrder.remove(player.getUniqueId());
    }

    private void ensureLobbyHostAssigned(Player preferredPlayer) {
        Player currentHost = lobbyHostId == null ? null : Bukkit.getPlayer(lobbyHostId);
        if (currentHost != null && currentHost.isOnline()) {
            refreshLobbyUtilityItemsForOnlinePlayers();
            return;
        }

        Player nextHost = findNextLobbyHostCandidate();
        if (nextHost == null && preferredPlayer != null && preferredPlayer.isOnline()) {
            nextHost = preferredPlayer;
        }

        UUID previousHostId = lobbyHostId;
        lobbyHostId = nextHost != null ? nextHost.getUniqueId() : null;
        refreshLobbyUtilityItemsForOnlinePlayers();

        if (nextHost != null && !nextHost.getUniqueId().equals(previousHostId)) {
            sendLocalized(nextHost, "host.assigned");
        }
    }

    private void handleLobbyHostDeparture(Player player) {
        if (!isLobbyHost(player)) {
            return;
        }
        lobbyHostId = null;
        Player nextHost = findNextLobbyHostCandidate();
        if (nextHost != null) {
            lobbyHostId = nextHost.getUniqueId();
            sendLocalized(nextHost, "host.assigned");
        }
        refreshLobbyUtilityItemsForOnlinePlayers();
    }

    private Player findNextLobbyHostCandidate() {
        return Bukkit.getOnlinePlayers().stream()
            .filter(Player::isOnline)
            .min(
                Comparator.comparingLong((Player player) -> lobbyHostJoinOrder.getOrDefault(player.getUniqueId(), Long.MAX_VALUE))
                    .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER)
            )
            .orElse(null);
    }

    private void refreshLobbyUtilityItemsForOnlinePlayers() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            giveLobbyUtilityItems(onlinePlayer);
        }
    }

    private void giveLobbyUtilityItems(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        clearTaggedItems(player, item -> isLanguageSelectorItem(item) || isLobbyHostItem(item));
        if (shouldGiveLanguageSelector(player)) {
            player.getInventory().setItem(0, createLanguageSelectorItem(player));
        }
        if (shouldGiveLobbyHostItem(player)) {
            player.getInventory().setItem(4, createLobbyHostItem(player));
        }
    }

    private Arena resolveArenaForJoin(String arenaId) {
        String requestedId = arenaId == null || arenaId.isBlank() ? defaultArenaId : arenaId;
        if (requestedId == null || requestedId.isBlank()) {
            requestedId = "random";
        }

        if ("random".equalsIgnoreCase(requestedId)) {
            return selectRandomQueueArena();
        }

        return arenas.get(requestedId.toLowerCase(Locale.ROOT));
    }

    private Arena selectRandomQueueArena() {
        List<Arena> candidates = arenas.values().stream()
            .filter(arena -> arena.getPlayerCount() < arena.getMaxPlayers())
            .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        int highestPopulation = candidates.stream()
            .mapToInt(Arena::getPlayerCount)
            .max()
            .orElse(0);

        List<Arena> groupedCandidates = candidates.stream()
            .filter(arena -> arena.getPlayerCount() == highestPopulation)
            .toList();

        return groupedCandidates.get(ThreadLocalRandom.current().nextInt(groupedCandidates.size()));
    }

    private void updateQueuePreference(UUID playerId, String requestedArenaId) {
        if (requestedArenaId == null || requestedArenaId.isBlank()) {
            requestedArenaId = defaultArenaId;
        }

        if (requestedArenaId != null && "random".equalsIgnoreCase(requestedArenaId)) {
            randomQueuePlayers.add(playerId);
            return;
        }

        randomQueuePlayers.remove(playerId);
    }

    private Arena selectRotationArena(Arena currentArena, int movingPlayers) {
        List<Arena> candidates = arenas.values().stream()
            .filter(arena -> arena != currentArena)
            .filter(arena -> arena.getPlayerCount() + movingPlayers <= arena.getMaxPlayers())
            .toList();
        if (candidates.isEmpty()) {
            return null;
        }

        int lowestPopulation = candidates.stream()
            .mapToInt(Arena::getPlayerCount)
            .min()
            .orElse(0);

        List<Arena> preferredCandidates = candidates.stream()
            .filter(arena -> arena.getPlayerCount() == lowestPopulation)
            .toList();

        return preferredCandidates.get(ThreadLocalRandom.current().nextInt(preferredCandidates.size()));
    }

    private void clearPlayer(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);
        for (org.bukkit.potion.PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1F);
        player.setCollidable(true);
        player.setCanPickupItems(true);
    }

    private void clearTaggedItems(Player player, Predicate<ItemStack> predicate) {
        if (player == null || predicate == null) {
            return;
        }

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (predicate.test(contents[slot])) {
                player.getInventory().setItem(slot, null);
            }
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (predicate.test(offHand)) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private void giveLanguageSelector(Player player) {
        if (!shouldGiveLanguageSelector(player)) {
            return;
        }
        player.getInventory().setItem(0, createLanguageSelectorItem(player));
    }

    private boolean shouldGiveLanguageSelector(Player player) {
        return isLobbyUtilityEligible(player);
    }

    private boolean shouldGiveLobbyHostItem(Player player) {
        return isLobbyHost(player) && isLobbyUtilityEligible(player);
    }

    private boolean isLobbyUtilityEligible(Player player) {
        Arena arena = getArena(player);
        return arena == null || (!arena.isActivePlayer(player) && !arena.isSpectator(player));
    }

    private ItemStack createLanguageSelectorItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "profile.selector_name"));
            meta.setLore(trList(player, "profile.selector_lore"));
            meta.getPersistentDataContainer().set(languageSelectorItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLobbyHostItem(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.selector_name"));
            meta.setLore(trList(player, "host.selector_lore"));
            meta.getPersistentDataContainer().set(lobbyHostItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createProfileStatsItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(player);
        meta.setDisplayName(tr(player, "profile.stats_name"));
        meta.setLore(trList(player, "profile.stats_lore"));
        meta.getPersistentDataContainer().set(profileStatsItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProfileLanguageItem(Player player) {
        ItemStack item = new ItemStack(Material.WHITE_BANNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "profile.language_name"));
            meta.setLore(trList(player, "profile.language_lore"));
            meta.getPersistentDataContainer().set(profileLanguageItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createProfileAboutItem(Player player) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "profile.about_name"));
            meta.setLore(trList(player, "profile.about_lore"));
            meta.getPersistentDataContainer().set(profileAboutItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStatsDisplayItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        PlayerStatsSnapshot stats = getPlayerStats(player);
        meta.setOwningPlayer(player);
        meta.setDisplayName(tr(player, "profile.stats_view_name", "player", player.getName()));
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(tr(player, "profile.stats_matches", "value", stats.matches()));
        lore.add(tr(player, "profile.stats_wins", "value", stats.wins()));
        lore.add(tr(player, "profile.stats_kills", "value", stats.kills()));
        lore.add(tr(player, "profile.stats_deaths", "value", stats.deaths()));
        lore.add(tr(player, "profile.stats_kd", "value", formatKd(stats)));
        lore.add(tr(player, "profile.stats_winrate", "value", formatColoredWinRate(stats)));
        lore.add(tr(player, "profile.stats_avg_round_duration", "value", formatDuration(stats.averageRoundDurationSeconds())));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAboutDisplayItem(Player player) {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            BuildInfo buildInfo = plugin.getBuildInfo();
            String version = buildInfo != null ? buildInfo.version() : plugin.getDescription().getVersion();
            String buildNumber = buildInfo != null && buildInfo.buildNumber() > 0
                ? "#" + buildInfo.buildNumber()
                : "#?";
            String buildDate = buildInfo != null ? buildInfo.buildDate() : "unknown";
            meta.setDisplayName(tr(player, "profile.about_view_name"));
            meta.setLore(trList(
                player,
                "profile.about_view_lore",
                "author", ABOUT_AUTHOR,
                "version", version,
                "build_number", buildNumber,
                "build_date", buildDate
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostSuddenNightMenuItem(Player player) {
        ItemStack item = new ItemStack(Material.DAYLIGHT_DETECTOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.sudden_night_settings_name"));
            meta.setLore(trList(
                player,
                "host.sudden_night_settings_lore",
                "status", getHostStatusText(player, suddenNightEnabled),
                "mode", getSuddenNightModeDisplay(player),
                "minutes", getSuddenNightDelayMinutes(),
                "action", tr(player, "host.click_open")
            ));
            meta.getPersistentDataContainer().set(hostMenuOpenSubmenuItemKey, PersistentDataType.STRING, "sudden_night");
            if (suddenNightEnabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostSuddenNightToggleItem(Player player) {
        ItemStack item = new ItemStack(suddenNightEnabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.sudden_night_toggle_name"));
            meta.setLore(trList(
                player,
                "host.sudden_night_toggle_lore",
                "status", getHostStatusText(player, suddenNightEnabled),
                "action", tr(player, "host.click_toggle")
            ));
            meta.getPersistentDataContainer().set(hostMenuToggleSuddenNightItemKey, PersistentDataType.BYTE, (byte) 1);
            addSelectedGlow(meta, suddenNightEnabled);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostSuddenNightAlwaysItem(Player player) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.sudden_night_always_name"));
            meta.setLore(trList(
                player,
                "host.sudden_night_always_lore",
                "status", getHostStatusText(player, suddenNightAlways),
                "action", tr(player, "host.click_toggle")
            ));
            meta.getPersistentDataContainer().set(hostMenuToggleSuddenNightAlwaysItemKey, PersistentDataType.BYTE, (byte) 1);
            addSelectedGlow(meta, suddenNightAlways);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostSuddenNightDelayAdjustItem(Player player, int delta) {
        ItemStack item = new ItemStack(delta > 0 ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, delta > 0 ? "host.sudden_night_delay_plus_name" : "host.sudden_night_delay_minus_name"));
            meta.setLore(trList(
                player,
                delta > 0 ? "host.sudden_night_delay_plus_lore" : "host.sudden_night_delay_minus_lore",
                "minutes", getSuddenNightDelayMinutes()
            ));
            meta.getPersistentDataContainer().set(hostMenuSuddenNightDelayAdjustItemKey, PersistentDataType.INTEGER, delta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostSuddenNightDelayDisplayItem(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.sudden_night_delay_name"));
            meta.setLore(trList(
                player,
                "host.sudden_night_delay_lore",
                "minutes", getSuddenNightDelayMinutes()
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostGameSpeedMenuItem(Player player) {
        ItemStack item = new ItemStack(Material.SUGAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.speed_settings_name"));
            meta.setLore(trList(
                player,
                "host.speed_settings_lore",
                "speed", getDropIntervalDisplay(player, lobbyDropIntervalTicks),
                "action", tr(player, "host.click_open")
            ));
            meta.getPersistentDataContainer().set(hostMenuOpenSubmenuItemKey, PersistentDataType.STRING, "game_speed");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostDropIntervalItem(Player player, int ticks, String nameKey) {
        ItemStack item = new ItemStack(ticks == DROP_INTERVAL_FAST_TICKS ? Material.FEATHER : ticks == DROP_INTERVAL_SLOW_TICKS ? Material.HONEY_BLOCK : Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean selected = lobbyDropIntervalTicks == ticks;
            meta.setDisplayName(tr(player, nameKey));
            meta.setLore(trList(
                player,
                "host.speed_option_lore",
                "seconds", ticks / 20,
                "status", selected ? tr(player, "host.selected") : tr(player, "host.not_selected")
            ));
            meta.getPersistentDataContainer().set(hostMenuDropIntervalItemKey, PersistentDataType.INTEGER, ticks);
            addSelectedGlow(meta, selected);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostMapMenuItem(Player player) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.map_name"));
            meta.setLore(trList(
                player,
                "host.map_lore",
                "map", getLobbyMapDisplay(player),
                "action", tr(player, "host.click_open")
            ));
            meta.getPersistentDataContainer().set(hostMenuOpenSubmenuItemKey, PersistentDataType.STRING, "map_selection");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostRoundsAdjustItem(Player player, int delta) {
        ItemStack item = new ItemStack(delta > 0 ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, delta > 0 ? "host.rounds_plus_name" : "host.rounds_minus_name"));
            meta.setLore(trList(
                player,
                delta > 0 ? "host.rounds_plus_lore" : "host.rounds_minus_lore",
                "value", lobbyRoundsBeforeReset
            ));
            meta.getPersistentDataContainer().set(hostMenuRoundsAdjustItemKey, PersistentDataType.INTEGER, delta);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostRoundsDisplayItem(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.rounds_info_name"));
            meta.setLore(trList(player, "host.rounds_info_lore", "value", lobbyRoundsBeforeReset));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostArenaCycleItem(Player player) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.map_name"));
            meta.setLore(trList(
                player,
                "host.map_lore",
                "map", getLobbyMapDisplay(player),
                "action", tr(player, "host.click_cycle_map")
            ));
            meta.getPersistentDataContainer().set(hostMenuArenaCycleItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostMapSelectionItem(Player player, Arena arena) {
        boolean random = arena == null;
        boolean selected = random ? isRandomLobbyArenaSelected() : arena.getId().equalsIgnoreCase(defaultArenaId);
        ItemStack item = new ItemStack(random ? Material.COMPASS : Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String mapName = random ? tr(player, "host.map_random") : arena.getDisplayName();
            meta.setDisplayName(tr(player, random ? "host.map_random_option_name" : "host.map_option_name", "map", mapName));
            meta.setLore(trList(
                player,
                "host.map_option_lore",
                "status", selected ? tr(player, "host.selected") : tr(player, "host.not_selected")
            ));
            meta.getPersistentDataContainer().set(hostMenuArenaSelectItemKey, PersistentDataType.STRING, random ? "random" : arena.getId());
            addSelectedGlow(meta, selected);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostAutostartItem(Player player) {
        ItemStack item = new ItemStack(Material.LEVER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.autostart_name"));
            meta.setLore(trList(
                player,
                "host.autostart_lore",
                "status", getHostStatusText(player, lobbyAutostartEnabled),
                "action", tr(player, "host.click_toggle")
            ));
            meta.getPersistentDataContainer().set(hostMenuAutostartItemKey, PersistentDataType.BYTE, (byte) 1);
            if (lobbyAutostartEnabled) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHostTransferItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(player);
        meta.setDisplayName(tr(player, "host.transfer_name"));
        meta.setLore(trList(player, "host.transfer_lore"));
        meta.getPersistentDataContainer().set(hostMenuTransferItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHostTransferTargetItem(Player viewer, Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        OfflinePlayer offlineTarget = target;
        meta.setOwningPlayer(offlineTarget);
        meta.setDisplayName(tr(viewer, "host.transfer_target_name", "player", target.getName()));
        meta.setLore(trList(viewer, "host.transfer_target_lore"));
        meta.getPersistentDataContainer().set(hostTransferTargetItemKey, PersistentDataType.STRING, target.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHostTransferEmptyItem(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "host.transfer_empty_name"));
            meta.setLore(trList(player, "host.transfer_empty_lore"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isBackItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(menuBackItemKey, PersistentDataType.BYTE);
    }

    private void addSelectedGlow(ItemMeta meta, boolean selected) {
        if (!selected) {
            return;
        }
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private ItemStack createBackItem(Player player) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "menu.back_name"));
            meta.setLore(trList(player, "menu.back_lore"));
            meta.getPersistentDataContainer().set(menuBackItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatColoredWinRate(PlayerStatsSnapshot stats) {
        long roundedWinRate = Math.round(stats.winRate());
        String color = roundedWinRate < 20 ? "&c" : roundedWinRate <= 50 ? "&e" : "&a";
        return color + roundedWinRate + "%";
    }

    private String formatKd(PlayerStatsSnapshot stats) {
        if (stats.deaths() <= 0) {
            return stats.kills() <= 0 ? "0.00" : "∞";
        }
        double kd = stats.kills() / (double) stats.deaths();
        return String.format(Locale.ROOT, "%.2f", kd);
    }

    private String formatDuration(double totalSeconds) {
        long roundedSeconds = Math.max(0L, Math.round(totalSeconds));
        long minutes = roundedSeconds / 60L;
        long seconds = roundedSeconds % 60L;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }

    private String getHostStatusText(Player player, boolean enabled) {
        return tr(player, enabled ? "host.status_enabled" : "host.status_disabled");
    }

    private String getSuddenNightModeDisplay(Player player) {
        if (suddenNightAlways) {
            return tr(player, "host.sudden_night_mode_always");
        }
        return tr(player, "host.sudden_night_mode_after", "minutes", getSuddenNightDelayMinutes());
    }

    private int getSuddenNightDelayMinutes() {
        return Math.max(1, (int) Math.ceil(suddenNightDelaySeconds / 60.0D));
    }

    private String getDropIntervalDisplay(Player player, int ticks) {
        if (ticks == DROP_INTERVAL_FAST_TICKS) {
            return tr(player, "host.speed_fast_name");
        }
        if (ticks == DROP_INTERVAL_SLOW_TICKS) {
            return tr(player, "host.speed_slow_name");
        }
        if (ticks == DROP_INTERVAL_CLASSIC_TICKS) {
            return tr(player, "host.speed_classic_name");
        }
        return tr(player, "host.speed_custom_name", "seconds", Math.max(1, ticks / 20));
    }

    private void setLobbyRoundsBeforeReset(int roundsBeforeReset) {
        lobbyRoundsBeforeReset = Math.max(1, roundsBeforeReset);
        for (Arena arena : arenas.values()) {
            arena.setRoundsBeforeReset(lobbyRoundsBeforeReset);
        }
    }

    private void setLobbyDropIntervalTicks(int dropIntervalTicks) {
        lobbyDropIntervalTicks = Math.max(1, dropIntervalTicks);
        for (Arena arena : arenas.values()) {
            arena.setDropIntervalTicks(lobbyDropIntervalTicks);
        }
    }

    private void setLobbyAutostartEnabled(boolean enabled) {
        lobbyAutostartEnabled = enabled;
        for (Arena arena : arenas.values()) {
            arena.reevaluateCountdownState();
        }
    }

    private void setLobbyArenaSelection(String selectedArenaId) {
        if (selectedArenaId == null || selectedArenaId.isBlank() || "random".equalsIgnoreCase(selectedArenaId)) {
            defaultArenaId = "random";
            setRandomQueuePreferenceForWaitingPlayers();
            return;
        }

        Arena selectedArena = arenas.get(selectedArenaId.toLowerCase(Locale.ROOT));
        if (selectedArena == null) {
            return;
        }

        defaultArenaId = selectedArena.getId();
        clearRandomQueuePreferenceForWaitingPlayers();
        moveQueuedPlayersToSelectedArena(selectedArena);
    }

    private void cycleLobbyArena() {
        List<Arena> arenaList = new ArrayList<>(arenas.values());
        if (arenaList.isEmpty()) {
            defaultArenaId = "random";
            return;
        }

        if (isRandomLobbyArenaSelected()) {
            Arena selectedArena = arenaList.get(0);
            defaultArenaId = selectedArena.getId();
            clearRandomQueuePreferenceForWaitingPlayers();
            moveQueuedPlayersToSelectedArena(selectedArena);
            return;
        }

        int currentIndex = -2;
        for (int index = 0; index < arenaList.size(); index++) {
            Arena arena = arenaList.get(index);
            if (arena.getId().equalsIgnoreCase(defaultArenaId)) {
                currentIndex = index;
                break;
            }
        }

        if (currentIndex == -2 || currentIndex >= arenaList.size() - 1) {
            defaultArenaId = "random";
            setRandomQueuePreferenceForWaitingPlayers();
            return;
        }

        Arena selectedArena = arenaList.get(currentIndex + 1);
        defaultArenaId = selectedArena.getId();
        clearRandomQueuePreferenceForWaitingPlayers();
        moveQueuedPlayersToSelectedArena(selectedArena);
    }

    private void clearRandomQueuePreferenceForWaitingPlayers() {
        for (Arena arena : arenas.values()) {
            if (arena.getState() == GameState.RUNNING || arena.getState() == GameState.ENDING) {
                continue;
            }
            for (UUID playerId : arena.getQueuedPlayersSnapshot()) {
                randomQueuePlayers.remove(playerId);
            }
        }
    }

    private void setRandomQueuePreferenceForWaitingPlayers() {
        for (Arena arena : arenas.values()) {
            if (arena.getState() == GameState.RUNNING || arena.getState() == GameState.ENDING) {
                continue;
            }
            for (UUID playerId : arena.getQueuedPlayersSnapshot()) {
                randomQueuePlayers.add(playerId);
            }
        }
    }

    private void refreshSuddenNightSessions() {
        clearSuddenNightStates();
        for (Arena arena : arenas.values()) {
            arena.restartSuddenNightSession();
        }
        if (!suddenNightEnabled) {
            enforceStaticDaylight();
        }
    }

    private Arena getSelectedLobbyArena() {
        return defaultArenaId == null ? null : arenas.get(defaultArenaId.toLowerCase(Locale.ROOT));
    }

    private boolean isRandomLobbyArenaSelected() {
        return defaultArenaId == null || defaultArenaId.isBlank() || "random".equalsIgnoreCase(defaultArenaId);
    }

    private String getLobbyMapDisplay(Player player) {
        Arena selectedArena = getSelectedLobbyArena();
        if (selectedArena != null) {
            return selectedArena.getDisplayName();
        }
        return tr(player, "host.map_random");
    }

    private void moveQueuedPlayersToSelectedArena(Arena selectedArena) {
        if (selectedArena == null) {
            return;
        }

        int pendingPlayers = arenas.values().stream()
            .filter(arena -> arena != selectedArena)
            .filter(arena -> arena.getState() != GameState.RUNNING && arena.getState() != GameState.ENDING)
            .mapToInt(Arena::getPlayerCount)
            .sum();
        if (pendingPlayers <= 0 || selectedArena.getPlayerCount() + pendingPlayers > selectedArena.getMaxPlayers()) {
            return;
        }

        for (Arena arena : arenas.values()) {
            if (arena == selectedArena || arena.getState() == GameState.RUNNING || arena.getState() == GameState.ENDING) {
                continue;
            }
            List<UUID> queuedPlayers = arena.getQueuedPlayersSnapshot();
            if (!queuedPlayers.isEmpty()) {
                arena.transferQueuedPlayersTo(selectedArena, queuedPlayers);
            }
        }
    }

    private ItemStack createLanguageOptionItem(Player viewer, PlayerLanguage option, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean selected = getLanguage(viewer) == option;
            meta.setDisplayName(tr(viewer, "language.option_" + option.getCode()));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(selected ? tr(viewer, "language.selected") : tr(viewer, "language.click_to_select"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(languageOptionItemKey, PersistentDataType.STRING, option.getCode());
            if (selected) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpectatorExitItem(Player player) {
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tr(player, "spectator.exit_name"));
            meta.setLore(trList(player, "spectator.exit_lore"));
            meta.getPersistentDataContainer().set(spectatorExitItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private double getSafeMaxHealth(Player player) {
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
            ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
            : 20.0D;
        return Math.min(20.0D, maxHealth);
    }

    private void resetMainScoreboard(Player player) {
        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private Location parseLocation(ConfigurationSection section, String fallbackWorldName) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world", fallbackWorldName);
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw", 0.0D),
            (float) section.getDouble("pitch", 0.0D)
        );
    }

    private Vector parseVector(ConfigurationSection section) {
        return new Vector(
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z")
        );
    }

    private Location parseLocationMap(Map<?, ?> values, String fallbackWorldName) {
        String worldName = getString(values, "world", fallbackWorldName);
        World world = worldName == null ? null : Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        return new Location(
            world,
            getDouble(values, "x", 0.0D),
            getDouble(values, "y", 0.0D),
            getDouble(values, "z", 0.0D),
            (float) getDouble(values, "yaw", 0.0D),
            (float) getDouble(values, "pitch", 0.0D)
        );
    }

    private String getString(Map<?, ?> values, String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private double getDouble(Map<?, ?> values, String key, double fallback) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String string) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private boolean isUnsafeDemoBuildOverrideAllowed(ConfigurationSection settings) {
        return settings != null && settings.getBoolean("allow-unsafe-demo-build-world-override", false);
    }

    private boolean isSafeDemoBuildWorld(String worldName, boolean allowUnsafeDemoBuildOverride) {
        if (allowUnsafeDemoBuildOverride) {
            return true;
        }
        return worldName != null && worldName.toLowerCase(Locale.ROOT).startsWith(SAFE_DEMO_WORLD_PREFIX);
    }

    private void logUnsafeDemoBuildBlocked(String worldName) {
        plugin.getLogger().warning(
            "Blocked demo-world auto-build for '" + worldName + "'. "
                + "Use a dedicated world name starting with '" + SAFE_DEMO_WORLD_PREFIX + "' "
                + "or enable settings.allow-unsafe-demo-build-world-override to bypass."
        );
    }

    private void ensureVoidWorld(ConfigurationSection settings) {
        boolean autoCreateVoidWorld = settings == null || settings.getBoolean("auto-create-void-world", true);
        if (!autoCreateVoidWorld) {
            return;
        }

        String worldName = settings != null ? settings.getString("void-world-name", DEFAULT_VOID_WORLD_NAME) : DEFAULT_VOID_WORLD_NAME;
        if (worldName == null || worldName.isBlank()) {
            worldName = DEFAULT_VOID_WORLD_NAME;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(new VoidChunkGenerator());
            world = Bukkit.createWorld(creator);
        }

        if (world == null) {
            plugin.getLogger().warning("Failed to create or load void world '" + worldName + "'.");
            return;
        }

        world.setAutoSave(true);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.MOB_GRIEFING, false);
        world.setTime(DEFAULT_WORLD_DAY_TIME);
        world.setStorm(false);

        boolean autoBuildDefaultMap = settings == null || settings.getBoolean("auto-build-default-map", true);
        if (autoBuildDefaultMap) {
            if (!isSafeDemoBuildWorld(worldName, isUnsafeDemoBuildOverrideAllowed(settings))) {
                logUnsafeDemoBuildBlocked(worldName);
                return;
            }
            DefaultMapBuilder.ensureBuilt(world);
        }
    }

    private static final class SuddenNightWorldState {
        private final World world;
        private int activeSessions;
        private boolean completedCycle;
        private BukkitTask startTask;
        private BukkitTask nightTask;

        private SuddenNightWorldState(World world) {
            this.world = world;
        }
    }
}
