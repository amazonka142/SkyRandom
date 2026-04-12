package dev.macuser.skyrandom.game;

import dev.macuser.skyrandom.BuildInfo;
import dev.macuser.skyrandom.SkyRandomPlugin;
import dev.macuser.skyrandom.gui.AboutMenuHolder;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public final class GameManager {

    private static final long COMBAT_TAG_MILLIS = 15_000L;
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
    private final MessageManager messages;
    private final PlayerStatsStore statsStore;
    private final NamespacedKey spectatorExitItemKey;
    private final NamespacedKey languageSelectorItemKey;
    private final NamespacedKey languageOptionItemKey;
    private final NamespacedKey profileStatsItemKey;
    private final NamespacedKey profileLanguageItemKey;
    private final NamespacedKey profileAboutItemKey;
    private final NamespacedKey menuBackItemKey;

    private DropTable dropTable = DropTable.empty();
    private Location lobbyLocation;
    private String prefix = ChatColor.AQUA + "[SkyRandom] " + ChatColor.GRAY;
    private boolean restorePlayerStateOnLeave = false;
    private boolean teleportToLobbyOnJoin = true;
    private boolean autoQueueOnJoin = true;
    private boolean dedicatedServerMode = true;
    private boolean soloTestMode = true;
    private String defaultArenaId = "alpha";
    private int lobbyProtectionRadius = 12;
    private int lobbyProtectionBelow = 14;
    private int lobbyProtectionAbove = 6;
    private int arenaBorderWarningDistance = 4;

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
        this.defaultArenaId = settings != null ? settings.getString("default-arena", "alpha") : "alpha";
        this.lobbyProtectionRadius = settings != null ? Math.max(1, settings.getInt("lobby-protection-radius", 12)) : 12;
        this.lobbyProtectionBelow = settings != null ? Math.max(0, settings.getInt("lobby-protection-below", 14)) : 14;
        this.lobbyProtectionAbove = settings != null ? Math.max(0, settings.getInt("lobby-protection-above", 6)) : 6;
        this.arenaBorderWarningDistance = settings != null ? Math.max(0, settings.getInt("arena-border-warning-distance", 4)) : 4;
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
        int globalDropInterval = settings != null ? settings.getInt("drop-interval-ticks", 50) : 50;
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
                arenaSection.getInt("rounds-before-reset", globalRoundsBeforeReset),
                arenaSection.getInt("round-start-freeze-ticks", globalRoundStartFreezeTicks),
                arenaSection.getDouble("player-boundary-margin", globalPlayerBoundaryMargin),
                arenaSection.getDouble("player-max-y-margin", globalPlayerMaxYMargin)
            );
            arenas.put(arenaId.toLowerCase(Locale.ROOT), arena);
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " arena(s).");
        restoreOnlinePlayersAfterReload();
    }

    public void shutdown() {
        for (Arena arena : arenas.values()) {
            arena.forceStop("system.match_stopped");
        }
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
        giveLanguageSelector(player);
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
            giveLanguageSelector(player);
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
                giveLanguageSelector(player);
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

    public void handleQuit(Player player) {
        Arena arena = getArena(player);
        if (arena != null) {
            arena.leave(player, "reason.server_leave");
        }
    }

    public void handleJoin(Player player) {
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
                giveLanguageSelector(player);
            }

            if (autoQueue && autoQueueOnJoin && defaultArenaId != null && !defaultArenaId.isBlank()) {
                joinArena(player, defaultArenaId);
            }
        }, 2L);
    }

    private void restoreOnlinePlayersAfterReload() {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (getArena(player) == null) {
                    forceLobbyFlow(player, true);
                }
            }
        }, 1L);
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
                giveLanguageSelector(player);
            }
            return;
        }

        giveLanguageSelector(player);
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

    private void giveLanguageSelector(Player player) {
        if (!shouldGiveLanguageSelector(player)) {
            return;
        }
        player.getInventory().setItem(0, createLanguageSelectorItem(player));
    }

    private boolean shouldGiveLanguageSelector(Player player) {
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

    private boolean isBackItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(menuBackItemKey, PersistentDataType.BYTE);
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
        world.setTime(6000L);
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
}
