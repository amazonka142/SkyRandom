package dev.macuser.skyrandom.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import dev.macuser.skyrandom.lang.PlayerLanguage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;

public final class Arena {

    private static final int ROUND_TITLE_STAY_TICKS = 40;
    private static final String ROUND_ENTRY = ChatColor.AQUA.toString();
    private static final String ALIVE_ENTRY = ChatColor.BLUE.toString();
    private static final String DROP_ENTRY = ChatColor.GREEN.toString();
    private static final String CLEANUP_ENTRY = ChatColor.GOLD.toString();
    private static final int DEFAULT_CLEANUP_BLOCKS_PER_TICK = 4096;
    private static final int SHRINKING_ZONE_TASK_PERIOD_TICKS = 20;
    private static final int RANDOM_EVENT_TASK_PERIOD_TICKS = 20;
    private static final int RANDOM_EVENT_MIN_DELAY_SECONDS = 45;
    private static final int RANDOM_EVENT_MAX_DELAY_SECONDS = 75;
    private static final int RANDOM_EVENT_FIRST_MIN_DELAY_SECONDS = 25;
    private static final int RANDOM_EVENT_FIRST_MAX_DELAY_SECONDS = 40;
    private static final int LOW_GRAVITY_DURATION_TICKS = 15 * 20;
    private static final int LUCKY_DROPS_DURATION_TICKS = 10 * 20;
    private static final int RESISTANCE_DURATION_TICKS = 15 * 20;
    private static final int SUDDEN_HUNGER_DURATION_TICKS = 15 * 20;
    private static final int SUDDEN_HUNGER_INTERVAL_TICKS = 20;
    private static final int FIRE_FEET_DURATION_TICKS = 15 * 20;
    private static final int FIRE_FEET_INTERVAL_TICKS = 10;

    private final GameManager manager;
    private final String id;
    private final String displayName;
    private final World world;
    private final Location waitingSpawn;
    private final BoundingBox region;
    private final List<Location> spawnPoints;
    private final int minPlayers;
    private final int maxPlayers;
    private final int countdownSeconds;
    private int dropIntervalTicks;
    private final int winDelaySeconds;
    private final int resetWinDelaySeconds;
    private final double eliminateBelowY;
    private final int maxBuildY;
    private final double dropHeightOffset;
    private final boolean allowPlaceBlocks;
    private final boolean allowBreakMapBlocks;
    private final boolean onlyBreakOwnPlacedBlocks;
    private int roundsBeforeReset;
    private final int roundStartFreezeTicks;
    private final int cleanupBlocksPerTick;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;
    private final double borderCenterX;
    private final double borderCenterZ;
    private final double borderSize;
    private final double playerBoundaryMargin;
    private final double playerMaxY;

    private final Set<UUID> players = new LinkedHashSet<>();
    private final Set<UUID> activePlayers = new LinkedHashSet<>();
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Set<UUID> currentRoundParticipants = new LinkedHashSet<>();
    private final Map<BlockKey, UUID> placedBlocks = new HashMap<>();
    private final Map<BlockKey, String> baselineBlocks = new HashMap<>();
    private final Map<UUID, Long> movementLockUntil = new HashMap<>();
    private final Map<PlayerLanguage, ArenaUiBundle> uiBundles = new EnumMap<>(PlayerLanguage.class);

    private GameState state = GameState.WAITING;
    private BukkitTask countdownTask;
    private BukkitTask dropTask;
    private BukkitTask randomEventTask;
    private BukkitTask activeRandomEventTask;
    private BukkitTask activeRandomEventEndTask;
    private BukkitTask shrinkingZoneTask;
    private BukkitTask finishTask;
    private BukkitTask cleanupTask;
    private int countdownRemaining;
    private int ticksUntilNextDrop;
    private int ticksUntilNextRandomEvent;
    private int luckyDropsRemainingTicks;
    private int intermissionRemaining;
    private int intermissionDurationSeconds;
    private int currentRound;
    private long roundStartedAtMillis;
    private boolean suddenNightSessionActive;
    private int shrinkingZoneElapsedTicks;
    private int shrinkingZoneStartDelayTicks;
    private int shrinkingZoneDurationTicks;
    private double shrinkingZoneCurrentSize;
    private boolean shrinkingZoneStartAnnounced;
    private boolean shrinkingZoneSecondStartAnnounced;
    private boolean shrinkingZoneFinalAnnounced;
    private int lastShrinkingZoneCountdownSeconds = -1;
    private int lastShrinkingZoneSecondCountdownSeconds = -1;
    private final Set<UUID> playersOutsideShrinkingZone = new LinkedHashSet<>();
    private final Map<UUID, FoodState> randomEventFoodStates = new HashMap<>();

    public Arena(
        GameManager manager,
        String id,
        String displayName,
        World world,
        Location waitingSpawn,
        BoundingBox region,
        List<Location> spawnPoints,
        int minPlayers,
        int maxPlayers,
        int countdownSeconds,
        int dropIntervalTicks,
        int winDelaySeconds,
        int resetWinDelaySeconds,
        double eliminateBelowY,
        int maxBuildY,
        double dropHeightOffset,
        boolean allowPlaceBlocks,
        boolean allowBreakMapBlocks,
        boolean onlyBreakOwnPlacedBlocks,
        int roundsBeforeReset,
        int roundStartFreezeTicks,
        int cleanupBlocksPerTick,
        double playerBoundaryMargin,
        double playerMaxYMargin
    ) {
        this.manager = manager;
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.waitingSpawn = waitingSpawn;
        this.region = region;
        this.spawnPoints = List.copyOf(spawnPoints);
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
        this.countdownSeconds = countdownSeconds;
        this.dropIntervalTicks = dropIntervalTicks;
        this.winDelaySeconds = Math.max(0, winDelaySeconds);
        this.resetWinDelaySeconds = Math.max(0, resetWinDelaySeconds);
        this.eliminateBelowY = eliminateBelowY;
        this.maxBuildY = maxBuildY;
        this.dropHeightOffset = dropHeightOffset;
        this.allowPlaceBlocks = allowPlaceBlocks;
        this.allowBreakMapBlocks = allowBreakMapBlocks;
        this.onlyBreakOwnPlacedBlocks = onlyBreakOwnPlacedBlocks;
        this.roundsBeforeReset = Math.max(1, roundsBeforeReset);
        this.roundStartFreezeTicks = Math.max(0, roundStartFreezeTicks);
        this.cleanupBlocksPerTick = Math.max(128, cleanupBlocksPerTick <= 0 ? DEFAULT_CLEANUP_BLOCKS_PER_TICK : cleanupBlocksPerTick);
        this.minX = (int) Math.floor(region.getMinX());
        this.maxX = (int) Math.floor(region.getMaxX());
        this.minY = (int) Math.floor(region.getMinY());
        this.maxY = (int) Math.floor(region.getMaxY());
        this.minZ = (int) Math.floor(region.getMinZ());
        this.maxZ = (int) Math.floor(region.getMaxZ());
        this.borderCenterX = (region.getMinX() + region.getMaxX()) / 2.0D;
        this.borderCenterZ = (region.getMinZ() + region.getMaxZ()) / 2.0D;
        this.borderSize = Math.max(region.getMaxX() - region.getMinX(), region.getMaxZ() - region.getMinZ());
        this.shrinkingZoneCurrentSize = borderSize;
        this.playerBoundaryMargin = Math.max(0.0D, playerBoundaryMargin);
        this.playerMaxY = Math.max(region.getMaxY(), maxBuildY) + Math.max(0.0D, playerMaxYMargin);
        for (PlayerLanguage language : PlayerLanguage.values()) {
            uiBundles.put(language, createUiBundle(language));
        }
        captureBaselineBlocks();
        updateSidebar();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public GameState getState() {
        return state;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isActivePlayer(Player player) {
        return player != null && activePlayers.contains(player.getUniqueId());
    }

    public boolean isSpectator(Player player) {
        return player != null && spectators.contains(player.getUniqueId());
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public List<UUID> getQueuedPlayersSnapshot() {
        return List.copyOf(players);
    }

    public List<UUID> getActivePlayersSnapshot() {
        return List.copyOf(activePlayers);
    }

    public int getRoundsBeforeReset() {
        return roundsBeforeReset;
    }

    public void setRoundsBeforeReset(int roundsBeforeReset) {
        this.roundsBeforeReset = Math.max(1, roundsBeforeReset);
        updateSidebar();
    }

    public void setDropIntervalTicks(int dropIntervalTicks) {
        this.dropIntervalTicks = Math.max(1, dropIntervalTicks);
        if (state == GameState.RUNNING && dropTask != null) {
            ticksUntilNextDrop = Math.max(1, Math.min(ticksUntilNextDrop, this.dropIntervalTicks));
            updateRunningBar();
        }
        updateSidebar();
    }

    public void restartSuddenNightSession() {
        if (state != GameState.RUNNING) {
            return;
        }
        endSuddenNightSessionIfNeeded();
        beginSuddenNightSessionIfNeeded();
    }

    public void restartShrinkingZoneSession() {
        if (state != GameState.RUNNING) {
            return;
        }
        stopShrinkingZoneSession(true);
        startShrinkingZoneIfNeeded();
        updateRunningBar();
    }

    public void restartRandomEventsSession() {
        if (state != GameState.RUNNING) {
            return;
        }
        stopRandomEventsSession(true);
        startRandomEventsIfNeeded();
    }

    public void reevaluateCountdownState() {
        evaluateCountdown();
    }

    public double getEliminateBelowY() {
        return eliminateBelowY;
    }

    public boolean isWorld(World checkedWorld) {
        return checkedWorld != null && checkedWorld.getUID().equals(world.getUID());
    }

    public World getWorld() {
        return world;
    }

    public double getBorderCenterX() {
        return borderCenterX;
    }

    public double getBorderCenterZ() {
        return borderCenterZ;
    }

    public double getBorderSize() {
        return borderSize;
    }

    public boolean contains(Location location) {
        return location != null
            && isWorld(location.getWorld())
            && region.contains(location.getX(), location.getY(), location.getZ());
    }

    public boolean containsPlayer(Location location) {
        if (location == null || !isWorld(location.getWorld())) {
            return false;
        }

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= (region.getMinX() - playerBoundaryMargin)
            && x <= (region.getMaxX() + playerBoundaryMargin)
            && z >= (region.getMinZ() - playerBoundaryMargin)
            && z <= (region.getMaxZ() + playerBoundaryMargin)
            && y <= playerMaxY;
    }

    public boolean isMovementLocked(Player player) {
        Long lockUntil = movementLockUntil.get(player.getUniqueId());
        if (lockUntil == null) {
            return false;
        }
        if (System.currentTimeMillis() >= lockUntil) {
            movementLockUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public void join(Player player) {
        if (players.contains(player.getUniqueId())) {
            sendLocalized(player, "arena.already_here");
            return;
        }
        if (players.size() >= maxPlayers) {
            sendLocalized(player, "arena.full");
            return;
        }

        manager.captureSnapshot(player);
        manager.bindPlayer(player, this);
        players.add(player.getUniqueId());
        manager.prepareQueuedPlayer(player, waitingSpawn);
        applyUi(player);
        refreshVisibility();

        if (state == GameState.RUNNING || state == GameState.ENDING) {
            sendLocalized(player, "arena.queued_next_round");
            broadcastLocalized(
                "arena.player_joined_queue",
                "player", player.getName(),
                "arena", displayName,
                "current", players.size(),
                "max", maxPlayers
            );
            updateSidebar();
            return;
        }

        broadcastLocalized(
            "arena.player_joined",
            "player", player.getName(),
            "arena", displayName,
            "current", players.size(),
            "max", maxPlayers
        );
        evaluateCountdown();
        updateWaitingBar();
    }

    public void leave(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        boolean removedFromQueue = players.remove(playerId);
        boolean removedFromRound = activePlayers.remove(playerId);
        boolean removedFromSpectators = spectators.remove(playerId);
        movementLockUntil.remove(playerId);
        playersOutsideShrinkingZone.remove(playerId);
        if (!removedFromQueue && !removedFromRound && !removedFromSpectators) {
            return;
        }

        removeUi(player);
        revealToArenaPlayers(player);
        revealArenaPlayersTo(player);
        refreshVisibility();
        manager.unbindPlayer(player);
        manager.restorePlayer(player, waitingSpawn);

        if (removedFromRound && (state == GameState.RUNNING || state == GameState.ENDING)) {
            if (state == GameState.RUNNING && !isSoloTestRound()) {
                manager.recordDeath(player);
            }
            broadcastReason(player, reason);
            checkForWinner();
        } else if (removedFromSpectators && (state == GameState.RUNNING || state == GameState.ENDING)) {
            broadcastLocalized("arena.player_left_spectator", "player", player.getName());
        } else {
            broadcastLocalized("arena.player_left_queue", "player", player.getName());
            evaluateCountdown();
        }

        if (players.isEmpty() && state != GameState.RUNNING) {
            resetArena(true);
        }

        updateSidebar();
        updateWaitingBar();
    }

    public void eliminate(Player player, String reason) {
        UUID playerId = player.getUniqueId();
        if (state != GameState.RUNNING || !activePlayers.contains(playerId)) {
            return;
        }

        player.setFallDistance(0.0F);
        activePlayers.remove(playerId);
        spectators.add(playerId);
        movementLockUntil.remove(playerId);
        playersOutsideShrinkingZone.remove(playerId);
        Player attacker = manager.findRecentAttacker(player);
        if (!isSoloTestRound()) {
            manager.recordDeath(player);
            if (attacker != null) {
                manager.recordKill(attacker);
            }
        }
        manager.prepareSpectatorPlayer(player, getSpectatorLocation(player.getLocation()));
        applyCurrentWorldBorder(player);
        applyUi(player);
        refreshVisibility();

        if (attacker != null) {
            sendLocalized(player, "arena.eliminated_by", "killer", attacker.getName());
            broadcastLocalized("arena.killer_broadcast", "killer", attacker.getName(), "player", player.getName());
        } else {
            sendLocalized(player, "arena.eliminated_reason", "reason", manager.tr(player, reason));
            broadcastReason(player, reason);
        }
        sendLocalized(player, "spectator.info");
        updateSidebar();
        checkForWinner();
    }

    public void forceStart(CommandSender sender) {
        if (state == GameState.RUNNING || state == GameState.ENDING) {
            manager.sendLocalized(sender, "arena.force_start_already_running", "arena", displayName);
            return;
        }
        if (players.size() < minPlayers) {
            manager.sendLocalized(sender, "arena.force_start_not_enough", "arena", displayName);
            return;
        }

        manager.sendLocalized(sender, "arena.force_start", "arena", displayName);
        cancelCountdown(false);
        startMatch();
    }

    public void forceStop(String reasonKey) {
        if (players.isEmpty() && placedBlocks.isEmpty()) {
            cancelAllTasks();
            resetArena(true);
            return;
        }

        if (reasonKey != null && !reasonKey.isBlank()) {
            broadcastLocalized(reasonKey);
        }

        cancelAllTasks();
        for (UUID uuid : List.copyOf(players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                revealToArenaPlayers(player);
                manager.unbindPlayer(player);
                manager.restorePlayer(player, waitingSpawn);
            } else {
                manager.unbindOffline(uuid);
                manager.discardSnapshot(uuid);
            }
        }

        resetArena(true);
    }

    public void transferQueuedPlayersTo(Arena destination, List<UUID> queuedPlayers) {
        boolean movedAny = false;

        for (UUID uuid : queuedPlayers) {
            if (!players.remove(uuid)) {
                continue;
            }

            activePlayers.remove(uuid);
            spectators.remove(uuid);
            movementLockUntil.remove(uuid);
            playersOutsideShrinkingZone.remove(uuid);

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                manager.unbindOffline(uuid);
                manager.discardSnapshot(uuid);
                continue;
            }

            removeUi(player);
            revealToArenaPlayers(player);

            destination.players.add(uuid);
            manager.bindPlayer(player, destination);
            manager.prepareQueuedPlayer(player, destination.waitingSpawn);
            destination.applyUi(player);
            destination.sendLocalized(player, "arena.random_queue_map", "arena", destination.getDisplayName());
            movedAny = true;
        }

        if (!movedAny) {
            return;
        }

        refreshVisibility();
        updateSidebar();
        updateWaitingBar();

        destination.refreshVisibility();
        destination.updateSidebar();
        if (destination.state == GameState.WAITING || destination.state == GameState.COUNTDOWN) {
            destination.evaluateCountdown();
        } else {
            destination.updateSidebar();
        }
        if (players.isEmpty()) {
            endSuddenNightSessionIfNeeded();
        }
        destination.broadcastLocalized("arena.random_map_changed", "arena", destination.getDisplayName());
    }

    public boolean handleBlockPlace(Player player, Block block) {
        if (state != GameState.RUNNING) {
            sendLocalized(player, "arena.place_match_only");
            return false;
        }
        if (!activePlayers.contains(player.getUniqueId())) {
            return false;
        }
        if (!contains(block.getLocation())) {
            sendLocalized(player, "arena.place_outside");
            return false;
        }
        if (!allowPlaceBlocks) {
            sendLocalized(player, "arena.place_disabled");
            return false;
        }
        if (block.getY() > maxBuildY) {
            sendLocalized(player, "arena.build_height_limit", "y", maxBuildY);
            return false;
        }

        placedBlocks.put(BlockKey.of(block), player.getUniqueId());
        return true;
    }

    public boolean handleBlockBreak(Player player, Block block) {
        if (state != GameState.RUNNING) {
            return false;
        }
        if (!activePlayers.contains(player.getUniqueId()) || !contains(block.getLocation())) {
            return false;
        }

        BlockKey key = BlockKey.of(block);
        UUID owner = placedBlocks.get(key);
        boolean changedFromBaseline = isChangedFromBaseline(block);

        if (allowBreakMapBlocks) {
            placedBlocks.remove(key);
            return true;
        }

        if (isBreakableMapCover(block)) {
            placedBlocks.remove(key);
            return true;
        }

        if (owner == null && !changedFromBaseline) {
            sendLocalized(player, "arena.break_map_forbidden");
            return false;
        }
        if (onlyBreakOwnPlacedBlocks && owner != null && !owner.equals(player.getUniqueId())) {
            sendLocalized(player, "arena.break_only_own");
            return false;
        }

        placedBlocks.remove(key);
        return true;
    }

    public void filterExplosion(List<Block> blocks) {
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!contains(block.getLocation())) {
                continue;
            }

            BlockKey key = BlockKey.of(block);
            UUID owner = placedBlocks.get(key);
            if (allowBreakMapBlocks || owner != null || isBreakableMapCover(block)) {
                placedBlocks.remove(key);
                continue;
            }

            iterator.remove();
        }
    }

    private boolean isBreakableMapCover(Block block) {
        return block != null && block.getType() == Material.SNOW;
    }

    public Location getDropLocation(Location playerLocation) {
        Location base = playerLocation.clone();
        base.setY(Math.min(maxBuildY, base.getY() + dropHeightOffset));
        return base;
    }

    public Location getMobSpawnLocation(Location playerLocation) {
        Location base = playerLocation.clone().add(
            (Math.random() - 0.5D) * 1.5D,
            1.0D,
            (Math.random() - 0.5D) * 1.5D
        );
        return base;
    }

    public void sendRaw(Player player, String message) {
        manager.send(player, "&f[" + displayName + "&f] &7" + message);
    }

    public void sendLocalized(Player player, String key, Object... replacements) {
        sendRaw(player, manager.tr(player, key, replacements));
    }

    private void broadcastLocalized(String key, Object... replacements) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                sendLocalized(player, key, replacements);
            }
        }
    }

    private void broadcastReason(Player affectedPlayer, String reasonKey) {
        for (UUID uuid : players) {
            Player viewer = Bukkit.getPlayer(uuid);
            if (viewer != null && viewer.isOnline()) {
                sendLocalized(
                    viewer,
                    "arena.player_eliminated_reason",
                    "player", affectedPlayer.getName(),
                    "reason", manager.tr(viewer, reasonKey)
                );
            }
        }
    }

    private void evaluateCountdown() {
        if (state == GameState.RUNNING || state == GameState.ENDING) {
            return;
        }

        if (!manager.isLobbyAutostartEnabled()) {
            if (state == GameState.COUNTDOWN) {
                cancelCountdown(false);
            } else {
                updateWaitingBar();
            }
            return;
        }

        if (players.size() >= minPlayers) {
            if (state != GameState.COUNTDOWN) {
                startCountdown();
            }
        } else if (state == GameState.COUNTDOWN) {
            cancelCountdown(true);
        } else {
            updateWaitingBar();
        }
    }

    private void startCountdown() {
        state = GameState.COUNTDOWN;
        countdownRemaining = countdownSeconds;
        broadcastLocalized("arena.autostart_started", "seconds", countdownRemaining);
        updateCountdownBar();

        countdownTask = manager.getPlugin().getServer().getScheduler().runTaskTimer(
            manager.getPlugin(),
            () -> {
                if (players.size() < minPlayers) {
                    cancelCountdown(true);
                    return;
                }

                if (countdownRemaining <= 0) {
                    cancelCountdown(false);
                    startMatch();
                    return;
                }

                if (countdownRemaining <= 5 || countdownRemaining % 5 == 0) {
                    for (UUID uuid : players) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && player.isOnline()) {
                            player.sendTitle(
                                manager.tr(player, "arena.countdown_title", "seconds", countdownRemaining),
                                displayName,
                                0,
                                20,
                                0
                            );
                        }
                    }
                    broadcastLocalized("arena.countdown_broadcast", "seconds", countdownRemaining);
                }

                updateCountdownBar();
                countdownRemaining--;
            },
            0L,
            20L
        );
    }

    private void cancelCountdown(boolean notify) {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (state == GameState.COUNTDOWN) {
            state = GameState.WAITING;
            if (notify) {
                broadcastLocalized("arena.autostart_cancelled");
            }
            updateWaitingBar();
        }
    }

    private void startMatch() {
        if (players.size() < minPlayers) {
            state = GameState.WAITING;
            broadcastLocalized("arena.start_not_enough");
            return;
        }

        removeOfflinePlayers();
        if (players.size() < minPlayers) {
            state = GameState.WAITING;
            broadcastLocalized("arena.match_cancelled_left");
            return;
        }

        endSuddenNightSessionIfNeeded();
        state = GameState.RUNNING;
        activePlayers.clear();
        spectators.clear();
        currentRoundParticipants.clear();
        currentRound = currentRound >= roundsBeforeReset ? 1 : currentRound + 1;

        List<Location> shuffledSpawns = new ArrayList<>(spawnPoints);
        Collections.shuffle(shuffledSpawns);

        int index = 0;
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }

            activePlayers.add(uuid);
            currentRoundParticipants.add(uuid);
            manager.prepareMatchPlayer(player);
            player.teleport(shuffledSpawns.get(index++).clone());
            applyCurrentWorldBorder(player);
            player.setNoDamageTicks(60);
            lockMovement(player);
            player.sendTitle(
                manager.tr(player, "arena.round_title", "current", currentRound, "max", roundsBeforeReset),
                roundStartFreezeTicks > 0 ? manager.tr(player, "arena.countdown_subtitle_freeze") : displayName,
                0,
                ROUND_TITLE_STAY_TICKS,
                10
            );
            applyUi(player);
            player.playSound(player.getLocation(), Sound.EVENT_RAID_HORN, SoundCategory.MASTER, 0.9F, 1.15F);
        }

        if (!isSoloTestRound()) {
            for (UUID uuid : activePlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    manager.recordMatch(player);
                }
            }
        }

        roundStartedAtMillis = System.currentTimeMillis();
        beginSuddenNightSessionIfNeeded();

        refreshVisibility();
        updateSidebar();
        broadcastLocalized("arena.match_started", "current", currentRound, "max", roundsBeforeReset);
        if (activePlayers.size() <= 1 && !isSoloTestRound()) {
            checkForWinner();
            return;
        }
        startShrinkingZoneIfNeeded();
        startDrops();
        startRandomEventsIfNeeded();
    }

    private void startDrops() {
        if (dropIntervalTicks <= 0 || manager.getDropTable().isEmpty()) {
            if (shrinkingZoneTask == null) {
                setBarsVisible(false);
            } else {
                updateRunningBar();
            }
            updateSidebar();
            return;
        }

        ticksUntilNextDrop = dropIntervalTicks;
        updateRunningBar();

        dropTask = manager.getPlugin().getServer().getScheduler().runTaskTimer(
            manager.getPlugin(),
            () -> {
                if (state != GameState.RUNNING) {
                    return;
                }

                ticksUntilNextDrop--;
                if (ticksUntilNextDrop <= 0) {
                    for (UUID uuid : List.copyOf(activePlayers)) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null || !player.isOnline()) {
                            continue;
                        }
                        if (luckyDropsRemainingTicks > 0 && manager.getDropTable().hasRareDrops()) {
                            manager.getDropTable().rollRare(player, this);
                        } else {
                            manager.getDropTable().roll(player, this);
                        }
                    }
                    ticksUntilNextDrop = dropIntervalTicks;
                }

                if (luckyDropsRemainingTicks > 0) {
                    luckyDropsRemainingTicks = Math.max(0, luckyDropsRemainingTicks - 1);
                }

                updateRunningBar();
            },
            1L,
            1L
        );
    }

    private void startRandomEventsIfNeeded() {
        if (!manager.isRandomEventsEnabled() || randomEventTask != null) {
            return;
        }

        ticksUntilNextRandomEvent = randomEventDelayTicks(RANDOM_EVENT_FIRST_MIN_DELAY_SECONDS, RANDOM_EVENT_FIRST_MAX_DELAY_SECONDS);
        randomEventTask = manager.getPlugin().getServer().getScheduler().runTaskTimer(
            manager.getPlugin(),
            () -> {
                if (state != GameState.RUNNING) {
                    stopRandomEventsSession(true);
                    return;
                }

                ticksUntilNextRandomEvent -= RANDOM_EVENT_TASK_PERIOD_TICKS;
                if (ticksUntilNextRandomEvent <= 0) {
                    triggerRandomEvent();
                    ticksUntilNextRandomEvent = randomEventDelayTicks(RANDOM_EVENT_MIN_DELAY_SECONDS, RANDOM_EVENT_MAX_DELAY_SECONDS);
                }
            },
            RANDOM_EVENT_TASK_PERIOD_TICKS,
            RANDOM_EVENT_TASK_PERIOD_TICKS
        );
    }

    private void triggerRandomEvent() {
        RandomRoundEvent event = RandomRoundEvent.random();
        announceRandomEvent(event);

        switch (event) {
            case LOW_GRAVITY -> applyLowGravityEvent();
            case LUCKY_DROPS -> startLuckyDropsEvent();
            case RESISTANCE -> applyResistanceEvent();
            case SUDDEN_HUNGER -> startSuddenHungerEvent();
            case FIRE_FEET -> startFireFeetEvent();
        }
    }

    private void announceRandomEvent(RandomRoundEvent event) {
        playArenaSound(Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9F, 1.25F);
        for (UUID uuid : List.copyOf(activePlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            player.sendTitle(
                manager.tr(player, "arena.random_event_title", "event", manager.tr(player, event.nameKey())),
                manager.tr(player, event.subtitleKey()),
                5,
                45,
                10
            );
            sendLocalized(player, "arena.random_event_started", "event", manager.tr(player, event.nameKey()));
        }
    }

    private void applyLowGravityEvent() {
        forEachActivePlayer(player -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, LOW_GRAVITY_DURATION_TICKS, 0, true, true, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, LOW_GRAVITY_DURATION_TICKS, 1, true, true, true));
        });
    }

    private void startLuckyDropsEvent() {
        luckyDropsRemainingTicks = LUCKY_DROPS_DURATION_TICKS;
    }

    private void applyResistanceEvent() {
        forEachActivePlayer(player ->
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, RESISTANCE_DURATION_TICKS, 0, true, true, true))
        );
    }

    private void startSuddenHungerEvent() {
        cancelActiveRandomEventTask();
        randomEventFoodStates.clear();
        forEachActivePlayer(player -> randomEventFoodStates.put(player.getUniqueId(), FoodState.of(player)));

        activeRandomEventTask = manager.getPlugin().getServer().getScheduler().runTaskTimer(
            manager.getPlugin(),
            () -> {
                if (state != GameState.RUNNING) {
                    restoreRandomEventFoodStates();
                    cancelActiveRandomEventTask();
                    return;
                }

                forEachActivePlayer(player -> player.setFoodLevel(Math.max(0, player.getFoodLevel() - 1)));
            },
            SUDDEN_HUNGER_INTERVAL_TICKS,
            SUDDEN_HUNGER_INTERVAL_TICKS
        );
        activeRandomEventEndTask = manager.getPlugin().getServer().getScheduler().runTaskLater(
            manager.getPlugin(),
            () -> {
                restoreRandomEventFoodStates();
                cancelActiveRandomEventTask();
            },
            SUDDEN_HUNGER_DURATION_TICKS
        );
    }

    private void startFireFeetEvent() {
        cancelActiveRandomEventTask();

        activeRandomEventTask = manager.getPlugin().getServer().getScheduler().runTaskTimer(
            manager.getPlugin(),
            () -> {
                if (state != GameState.RUNNING) {
                    cancelActiveRandomEventTask();
                    return;
                }

                forEachActivePlayer(this::igniteBlockUnderPlayer);
            },
            0L,
            FIRE_FEET_INTERVAL_TICKS
        );
        activeRandomEventEndTask = manager.getPlugin().getServer().getScheduler().runTaskLater(
            manager.getPlugin(),
            this::cancelActiveRandomEventTask,
            FIRE_FEET_DURATION_TICKS
        );
    }

    private void igniteBlockUnderPlayer(Player player) {
        Location location = player.getLocation();
        if (!contains(location)) {
            return;
        }

        Block fireBlock = location.getBlock();
        Block supportBlock = fireBlock.getRelative(org.bukkit.block.BlockFace.DOWN);
        if (!fireBlock.getType().isAir() || supportBlock.getType().isAir()) {
            return;
        }

        fireBlock.setType(Material.FIRE, false);
    }

    private void stopRandomEventsSession(boolean restoreFood) {
        if (randomEventTask != null) {
            randomEventTask.cancel();
            randomEventTask = null;
        }
        cancelActiveRandomEventTask();
        luckyDropsRemainingTicks = 0;
        ticksUntilNextRandomEvent = 0;
        if (restoreFood) {
            restoreRandomEventFoodStates();
        } else {
            randomEventFoodStates.clear();
        }
    }

    private void cancelActiveRandomEventTask() {
        if (activeRandomEventTask != null) {
            activeRandomEventTask.cancel();
            activeRandomEventTask = null;
        }
        if (activeRandomEventEndTask != null) {
            activeRandomEventEndTask.cancel();
            activeRandomEventEndTask = null;
        }
    }

    private void restoreRandomEventFoodStates() {
        for (Map.Entry<UUID, FoodState> entry : randomEventFoodStates.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                entry.getValue().restore(player);
            }
        }
        randomEventFoodStates.clear();
    }

    private void forEachActivePlayer(java.util.function.Consumer<Player> action) {
        for (UUID uuid : List.copyOf(activePlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                action.accept(player);
            }
        }
    }

    private static int randomEventDelayTicks(int minSeconds, int maxSeconds) {
        int min = Math.max(1, minSeconds);
        int max = Math.max(min, maxSeconds);
        return ThreadLocalRandom.current().nextInt(min, max + 1) * 20;
    }

    private void checkForWinner() {
        if (state != GameState.RUNNING && state != GameState.ENDING) {
            return;
        }
        if (state == GameState.ENDING) {
            return;
        }
        if (activePlayers.size() > 1) {
            return;
        }
        if (isSoloTestRound()) {
            return;
        }

        state = GameState.ENDING;
        if (dropTask != null) {
            dropTask.cancel();
            dropTask = null;
        }
        stopRandomEventsSession(true);
        endSuddenNightSessionIfNeeded();
        stopShrinkingZoneSession(true);
        updateBarsAppearance(BarColor.PURPLE, 1.0D, null, false);

        if (activePlayers.isEmpty()) {
            broadcastLocalized("arena.no_winner");
            updateBarsAppearance(BarColor.PURPLE, 1.0D, "bossbar.match_finished", true);
        } else {
            UUID winnerId = activePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                if (!isSoloTestRound()) {
                    manager.recordWin(winner);
                }
                broadcastLocalized("arena.winner", "player", winner.getName());
                winner.sendTitle(manager.tr(winner, "arena.victory_title"), displayName, 0, 40, 10);
                updateBarsAppearance(BarColor.PURPLE, 1.0D, "bossbar.winner", true, "player", winner.getName());
            }
        }
        playArenaSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
        updateSidebar();
        recordCompletedRoundDuration();

        movePlayersToIntermission();
        startIntermission();
    }

    private void startIntermission() {
        intermissionDurationSeconds = getIntermissionDelaySeconds();
        if (intermissionDurationSeconds <= 0) {
            finishRound();
            return;
        }

        intermissionRemaining = intermissionDurationSeconds;
        updateIntermissionBar();

        finishTask = manager.getPlugin().getServer().getScheduler().runTaskTimer(
            manager.getPlugin(),
            () -> {
                intermissionRemaining--;
                if (intermissionRemaining <= 0) {
                    if (finishTask != null) {
                        finishTask.cancel();
                        finishTask = null;
                    }
                    finishRound();
                    return;
                }

                if (intermissionRemaining <= 5 || intermissionRemaining % 5 == 0) {
                    broadcastLocalized("arena.next_round_in", "seconds", intermissionRemaining);
                }

                updateIntermissionBar();
            },
            20L,
            20L
        );
    }

    private void finishRound() {
        endSuddenNightSessionIfNeeded();
        stopShrinkingZoneSession(true);
        stopRandomEventsSession(true);
        boolean reachedResetRound = currentRound >= roundsBeforeReset;
        if (reachedResetRound) {
            startArenaRegionCleanup(() -> completeRound(true));
            return;
        }

        completeRound(false);
    }

    private void completeRound(boolean reachedResetRound) {
        if (reachedResetRound) {
            currentRound = 0;
            broadcastLocalized("arena.cleanup_after", "rounds", roundsBeforeReset);
        }

        activePlayers.clear();
        spectators.clear();
        movementLockUntil.clear();
        playersOutsideShrinkingZone.clear();
        state = GameState.WAITING;
        countdownRemaining = 0;
        ticksUntilNextDrop = 0;
        intermissionRemaining = 0;
        intermissionDurationSeconds = 0;
        roundStartedAtMillis = 0L;
        currentRoundParticipants.clear();
        resetShrinkingZoneState();
        stopRandomEventsSession(true);

        if (reachedResetRound) {
            manager.rotateRandomQueuePlayers(this);
        }

        for (UUID uuid : List.copyOf(players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                manager.prepareQueuedPlayer(player, waitingSpawn);
                applyUi(player);
            } else {
                players.remove(uuid);
                activePlayers.remove(uuid);
                spectators.remove(uuid);
                manager.unbindOffline(uuid);
                manager.discardSnapshot(uuid);
            }
        }

        if (players.isEmpty()) {
            resetArena(!reachedResetRound);
            return;
        }

        refreshVisibility();
        updateSidebar();
        evaluateCountdown();
    }

    private void resetArena(boolean cleanupBlocks) {
        cancelAllTasks();
        endSuddenNightSessionIfNeeded();
        stopShrinkingZoneSession(false);
        stopRandomEventsSession(false);
        if (cleanupBlocks) {
            restoreArenaRegion();
        }
        players.clear();
        activePlayers.clear();
        spectators.clear();
        movementLockUntil.clear();
        state = GameState.WAITING;
        countdownRemaining = 0;
        ticksUntilNextDrop = 0;
        intermissionRemaining = 0;
        intermissionDurationSeconds = 0;
        currentRound = 0;
        roundStartedAtMillis = 0L;
        currentRoundParticipants.clear();
        resetShrinkingZoneState();
        for (ArenaUiBundle bundle : uiBundles.values()) {
            bundle.bar().removeAll();
            bundle.bar().setVisible(false);
        }
    }

    private void removeOfflinePlayers() {
        for (UUID uuid : List.copyOf(players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                continue;
            }

            players.remove(uuid);
            activePlayers.remove(uuid);
            spectators.remove(uuid);
            movementLockUntil.remove(uuid);
            playersOutsideShrinkingZone.remove(uuid);
            manager.unbindOffline(uuid);
            manager.discardSnapshot(uuid);
        }
    }

    private void cancelAllTasks() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (dropTask != null) {
            dropTask.cancel();
            dropTask = null;
        }
        if (randomEventTask != null) {
            randomEventTask.cancel();
            randomEventTask = null;
        }
        if (activeRandomEventTask != null) {
            activeRandomEventTask.cancel();
            activeRandomEventTask = null;
        }
        if (activeRandomEventEndTask != null) {
            activeRandomEventEndTask.cancel();
            activeRandomEventEndTask = null;
        }
        if (shrinkingZoneTask != null) {
            shrinkingZoneTask.cancel();
            shrinkingZoneTask = null;
        }
        if (finishTask != null) {
            finishTask.cancel();
            finishTask = null;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    private void updateWaitingBar() {
        if (players.isEmpty()) {
            setBarsVisible(false);
            return;
        }

        updateBarsAppearance(
            BarColor.YELLOW,
            Math.max(0.0D, Math.min(1.0D, players.size() / (double) minPlayers)),
            "bossbar.waiting",
            true,
            "current", players.size(),
            "required", minPlayers
        );
        updateSidebar();
    }

    private void updateCountdownBar() {
        double progress = countdownSeconds <= 0 ? 1.0D : countdownRemaining / (double) countdownSeconds;
        updateBarsAppearance(
            BarColor.BLUE,
            Math.max(0.0D, Math.min(1.0D, progress)),
            "bossbar.countdown",
            true,
            "seconds", countdownRemaining
        );
        updateSidebar();
    }

    private void updateRunningBar() {
        if (shrinkingZoneTask != null) {
            updateShrinkingZoneBar();
            return;
        }
        if (dropTask != null && dropIntervalTicks > 0 && !manager.getDropTable().isEmpty()) {
            updateDropBar();
            return;
        }
        setBarsVisible(false);
        updateSidebar();
    }

    private void updateDropBar() {
        int seconds = Math.max(1, (int) Math.ceil(ticksUntilNextDrop / 20.0D));
        double progress = dropIntervalTicks <= 0 ? 1.0D : ticksUntilNextDrop / (double) dropIntervalTicks;
        updateBarsAppearance(
            BarColor.GREEN,
            Math.max(0.0D, Math.min(1.0D, progress)),
            "bossbar.drop",
            true,
            "seconds", seconds
        );
        updateSidebar();
    }

    private void updateIntermissionBar() {
        double progress = intermissionDurationSeconds <= 0
            ? 1.0D
            : intermissionRemaining / (double) intermissionDurationSeconds;
        updateBarsAppearance(
            BarColor.PINK,
            Math.max(0.0D, Math.min(1.0D, progress)),
            "bossbar.next_round",
            true,
            "seconds", intermissionRemaining
        );
        updateSidebar();
    }

    private void updateCleanupBar(double progress) {
        int percent = (int) Math.round(Math.max(0.0D, Math.min(1.0D, progress)) * 100.0D);
        updateBarsAppearance(
            BarColor.WHITE,
            Math.max(0.0D, Math.min(1.0D, progress)),
            "bossbar.cleanup",
            true,
            "percent", percent
        );
        updateSidebar();
    }

    private void startShrinkingZoneIfNeeded() {
        if (!manager.isShrinkingZoneEnabled()) {
            resetShrinkingZoneState();
            return;
        }
        if (shrinkingZoneTask != null) {
            return;
        }

        resetShrinkingZoneState();
        shrinkingZoneStartDelayTicks = Math.max(0, manager.getShrinkingZoneStartDelaySeconds() * 20);
        shrinkingZoneDurationTicks = Math.max(SHRINKING_ZONE_TASK_PERIOD_TICKS, manager.getShrinkingZoneDurationSeconds() * 20);
        shrinkingZoneCurrentSize = borderSize;
        applyShrinkingZoneWorldBorder();

        if (shrinkingZoneStartDelayTicks > 0) {
            lastShrinkingZoneCountdownSeconds = shrinkingZoneStartDelayTicks / 20;
            broadcastLocalized("arena.shrinking_zone_countdown", "seconds", lastShrinkingZoneCountdownSeconds);
        }

        shrinkingZoneTask = manager.getPlugin().getServer().getScheduler().runTaskTimer(
            manager.getPlugin(),
            () -> {
                if (state != GameState.RUNNING) {
                    stopShrinkingZoneSession(true);
                    return;
                }

                updateShrinkingZoneState();
                shrinkingZoneElapsedTicks += SHRINKING_ZONE_TASK_PERIOD_TICKS;
            },
            0L,
            SHRINKING_ZONE_TASK_PERIOD_TICKS
        );
    }

    private void updateShrinkingZoneState() {
        shrinkingZoneCurrentSize = calculateShrinkingZoneSize();
        announceShrinkingZoneState();
        applyShrinkingZoneWorldBorder();
        if (isShrinkingZoneDamaging()) {
            damagePlayersOutsideShrinkingZone();
        }
        updateShrinkingZoneBar();
    }

    private double calculateShrinkingZoneSize() {
        double firstTargetSize = getShrinkingZoneFirstTargetSize();
        double finalTargetSize = getShrinkingZoneFinalSize();
        int firstShrinkStartTick = getFirstShrinkStartTick();
        int firstShrinkEndTick = getFirstShrinkEndTick();
        int secondShrinkStartTick = getSecondShrinkStartTick();
        int secondShrinkEndTick = getSecondShrinkEndTick();

        if (shrinkingZoneElapsedTicks < firstShrinkStartTick) {
            return borderSize;
        }
        if (shrinkingZoneElapsedTicks < firstShrinkEndTick) {
            double progress = getProgress(shrinkingZoneElapsedTicks - firstShrinkStartTick, shrinkingZoneDurationTicks);
            return interpolate(borderSize, firstTargetSize, progress);
        }
        if (shrinkingZoneElapsedTicks < secondShrinkStartTick) {
            return firstTargetSize;
        }
        if (shrinkingZoneElapsedTicks < secondShrinkEndTick) {
            double progress = getProgress(shrinkingZoneElapsedTicks - secondShrinkStartTick, shrinkingZoneDurationTicks);
            return interpolate(firstTargetSize, finalTargetSize, progress);
        }
        return finalTargetSize;
    }

    private double getProgress(int elapsedTicks, int durationTicks) {
        return durationTicks <= 0
            ? 1.0D
            : Math.max(0.0D, Math.min(1.0D, elapsedTicks / (double) durationTicks));
    }

    private double interpolate(double from, double to, double progress) {
        return from - ((from - to) * progress);
    }

    private double getShrinkingZoneFirstTargetSize() {
        return Math.max(getShrinkingZoneFinalSize(), Math.min(borderSize, manager.getShrinkingZoneMinSize()));
    }

    private double getShrinkingZoneFinalSize() {
        return Math.max(1.0D, Math.min(borderSize, manager.getShrinkingZoneFinalSize()));
    }

    private int getFirstShrinkStartTick() {
        return shrinkingZoneStartDelayTicks;
    }

    private int getFirstShrinkEndTick() {
        return getFirstShrinkStartTick() + shrinkingZoneDurationTicks;
    }

    private int getSecondShrinkStartTick() {
        return getFirstShrinkEndTick() + shrinkingZoneStartDelayTicks;
    }

    private int getSecondShrinkEndTick() {
        return getSecondShrinkStartTick() + shrinkingZoneDurationTicks;
    }

    private void announceShrinkingZoneState() {
        int firstShrinkStartTick = getFirstShrinkStartTick();
        int secondShrinkStartTick = getSecondShrinkStartTick();
        int secondShrinkEndTick = getSecondShrinkEndTick();
        int secondsUntilStart = Math.max(0, (int) Math.ceil((firstShrinkStartTick - shrinkingZoneElapsedTicks) / 20.0D));
        if (shrinkingZoneElapsedTicks < firstShrinkStartTick
            && shouldAnnounceShrinkingZoneCountdown(secondsUntilStart)
            && secondsUntilStart != lastShrinkingZoneCountdownSeconds) {
            lastShrinkingZoneCountdownSeconds = secondsUntilStart;
            broadcastLocalized("arena.shrinking_zone_countdown", "seconds", secondsUntilStart);
            return;
        }

        if (!shrinkingZoneStartAnnounced && shrinkingZoneElapsedTicks >= firstShrinkStartTick) {
            shrinkingZoneStartAnnounced = true;
            broadcastLocalized("arena.shrinking_zone_started");
            playArenaSound(Sound.BLOCK_BEACON_DEACTIVATE, 0.7F, 1.3F);
        }

        if (shrinkingZoneElapsedTicks >= getFirstShrinkEndTick() && shrinkingZoneElapsedTicks < secondShrinkStartTick) {
            int secondsUntilSecondShrink = Math.max(0, (int) Math.ceil((secondShrinkStartTick - shrinkingZoneElapsedTicks) / 20.0D));
            if ((lastShrinkingZoneSecondCountdownSeconds < 0 || shouldAnnounceShrinkingZoneCountdown(secondsUntilSecondShrink))
                && secondsUntilSecondShrink != lastShrinkingZoneSecondCountdownSeconds) {
                lastShrinkingZoneSecondCountdownSeconds = secondsUntilSecondShrink;
                broadcastLocalized("arena.shrinking_zone_second_countdown", "seconds", secondsUntilSecondShrink);
            }
            return;
        }

        if (!shrinkingZoneSecondStartAnnounced && shrinkingZoneElapsedTicks >= secondShrinkStartTick) {
            shrinkingZoneSecondStartAnnounced = true;
            broadcastLocalized("arena.shrinking_zone_final_started");
            playArenaSound(Sound.BLOCK_BEACON_DEACTIVATE, 0.8F, 0.9F);
        }

        if (!shrinkingZoneFinalAnnounced && shrinkingZoneElapsedTicks >= secondShrinkEndTick) {
            shrinkingZoneFinalAnnounced = true;
            broadcastLocalized("arena.shrinking_zone_final");
        }
    }

    private boolean shouldAnnounceShrinkingZoneCountdown(int secondsUntilStart) {
        return secondsUntilStart == 30 || secondsUntilStart == 10 || secondsUntilStart == 5;
    }

    private boolean isShrinkingZoneDamaging() {
        return shrinkingZoneTask != null && shrinkingZoneElapsedTicks >= shrinkingZoneStartDelayTicks;
    }

    private void damagePlayersOutsideShrinkingZone() {
        double damage = manager.getShrinkingZoneDamagePerSecond();
        for (UUID uuid : List.copyOf(activePlayers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }

            if (isInsideShrinkingZone(player.getLocation())) {
                playersOutsideShrinkingZone.remove(uuid);
                continue;
            }

            if (playersOutsideShrinkingZone.add(uuid)) {
                sendLocalized(player, "arena.shrinking_zone_outside");
                player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 0.7F, 0.8F);
            }

            if (damage <= 0.0D) {
                continue;
            }
            if (player.getHealth() <= damage) {
                eliminate(player, "reason.shrinking_zone");
                continue;
            }
            player.damage(damage);
        }
    }

    private boolean isInsideShrinkingZone(Location location) {
        if (location == null || !isWorld(location.getWorld())) {
            return false;
        }
        double halfSize = Math.max(0.5D, shrinkingZoneCurrentSize / 2.0D);
        return Math.abs(location.getX() - borderCenterX) <= halfSize
            && Math.abs(location.getZ() - borderCenterZ) <= halfSize;
    }

    private void updateShrinkingZoneBar() {
        int firstShrinkStartTick = getFirstShrinkStartTick();
        int firstShrinkEndTick = getFirstShrinkEndTick();
        int secondShrinkStartTick = getSecondShrinkStartTick();
        if (shrinkingZoneElapsedTicks < firstShrinkStartTick) {
            int seconds = Math.max(0, (int) Math.ceil((firstShrinkStartTick - shrinkingZoneElapsedTicks) / 20.0D));
            double progress = shrinkingZoneStartDelayTicks <= 0
                ? 1.0D
                : Math.max(0.0D, Math.min(1.0D, (firstShrinkStartTick - shrinkingZoneElapsedTicks) / (double) shrinkingZoneStartDelayTicks));
            updateBarsAppearance(BarColor.YELLOW, progress, "bossbar.zone_waiting", true, "seconds", seconds);
            updateSidebar();
            return;
        }
        if (shrinkingZoneElapsedTicks >= firstShrinkEndTick && shrinkingZoneElapsedTicks < secondShrinkStartTick) {
            int seconds = Math.max(0, (int) Math.ceil((secondShrinkStartTick - shrinkingZoneElapsedTicks) / 20.0D));
            double progress = shrinkingZoneStartDelayTicks <= 0
                ? 1.0D
                : Math.max(0.0D, Math.min(1.0D, (secondShrinkStartTick - shrinkingZoneElapsedTicks) / (double) shrinkingZoneStartDelayTicks));
            updateBarsAppearance(BarColor.YELLOW, progress, "bossbar.zone_next", true, "seconds", seconds);
            updateSidebar();
            return;
        }

        int size = Math.max(1, (int) Math.round(shrinkingZoneCurrentSize));
        double progress = borderSize <= 0.0D
            ? 1.0D
            : Math.max(0.0D, Math.min(1.0D, shrinkingZoneCurrentSize / borderSize));
        updateBarsAppearance(
            BarColor.RED,
            progress,
            shrinkingZoneFinalAnnounced ? "bossbar.zone_final" : "bossbar.zone_shrinking",
            true,
            "size", size
        );
        updateSidebar();
    }

    private void stopShrinkingZoneSession(boolean restoreWorldBorder) {
        if (shrinkingZoneTask != null) {
            shrinkingZoneTask.cancel();
            shrinkingZoneTask = null;
        }
        resetShrinkingZoneState();
        if (restoreWorldBorder) {
            applyFullWorldBorderToRoundPlayers();
        }
    }

    private void resetShrinkingZoneState() {
        shrinkingZoneElapsedTicks = 0;
        shrinkingZoneStartDelayTicks = 0;
        shrinkingZoneDurationTicks = 0;
        shrinkingZoneCurrentSize = borderSize;
        shrinkingZoneStartAnnounced = false;
        shrinkingZoneSecondStartAnnounced = false;
        shrinkingZoneFinalAnnounced = false;
        lastShrinkingZoneCountdownSeconds = -1;
        lastShrinkingZoneSecondCountdownSeconds = -1;
        playersOutsideShrinkingZone.clear();
    }

    private void applyCurrentWorldBorder(Player player) {
        manager.applyArenaWorldBorder(player, this, shrinkingZoneCurrentSize);
    }

    private void applyShrinkingZoneWorldBorder() {
        for (UUID uuid : players) {
            if (!activePlayers.contains(uuid) && !spectators.contains(uuid)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyCurrentWorldBorder(player);
            }
        }
    }

    private void applyFullWorldBorderToRoundPlayers() {
        for (UUID uuid : players) {
            if (!activePlayers.contains(uuid) && !spectators.contains(uuid)) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                manager.applyArenaWorldBorder(player, this);
            }
        }
    }

    private int getIntermissionDelaySeconds() {
        return currentRound >= roundsBeforeReset ? resetWinDelaySeconds : winDelaySeconds;
    }

    private void recordCompletedRoundDuration() {
        if (isSoloTestRound() || currentRoundParticipants.isEmpty() || roundStartedAtMillis <= 0L) {
            return;
        }

        long durationSeconds = Math.max(1L, (long) Math.ceil((System.currentTimeMillis() - roundStartedAtMillis) / 1000.0D));
        for (UUID participantId : currentRoundParticipants) {
            manager.recordRoundDuration(participantId, durationSeconds);
        }
    }

    private void movePlayersToIntermission() {
        activePlayers.clear();
        spectators.clear();
        movementLockUntil.clear();
        for (UUID uuid : List.copyOf(players)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                continue;
            }
            manager.prepareQueuedPlayer(player, waitingSpawn);
            applyUi(player);
        }
        refreshVisibility();
    }

    private void updateSidebar() {
        for (Map.Entry<PlayerLanguage, ArenaUiBundle> entry : uiBundles.entrySet()) {
            PlayerLanguage language = entry.getKey();
            ArenaUiBundle bundle = entry.getValue();
            bundle.roundTeam().setPrefix(ChatColor.YELLOW + manager.tr(language, "sidebar.round"));
            bundle.roundTeam().setSuffix(ChatColor.WHITE + getRoundSidebarValue());
            bundle.aliveTeam().setPrefix(ChatColor.RED + manager.tr(language, "sidebar.alive"));
            bundle.aliveTeam().setSuffix(ChatColor.WHITE + String.valueOf(activePlayers.size()));
            bundle.dropTeam().setPrefix(ChatColor.GREEN + manager.tr(language, "sidebar.drop"));
            bundle.dropTeam().setSuffix(ChatColor.WHITE + getDropSidebarValue(language));
            bundle.cleanupTeam().setPrefix(ChatColor.GOLD + manager.tr(language, "sidebar.cleanup"));
            bundle.cleanupTeam().setSuffix(ChatColor.WHITE + String.valueOf(getRoundsUntilCleanup()));
        }
    }

    private String getRoundSidebarValue() {
        if (currentRound <= 0) {
            return "0/" + roundsBeforeReset;
        }
        return currentRound + "/" + roundsBeforeReset;
    }

    private String getDropSidebarValue(PlayerLanguage language) {
        if (state != GameState.RUNNING || dropIntervalTicks <= 0 || manager.getDropTable().isEmpty()) {
            return manager.tr(language, "sidebar.none");
        }
        int seconds = Math.max(1, (int) Math.ceil(ticksUntilNextDrop / 20.0D));
        return seconds + manager.tr(language, "sidebar.seconds_suffix");
    }

    private int getRoundsUntilCleanup() {
        if (currentRound <= 0) {
            return roundsBeforeReset;
        }

        int remaining = roundsBeforeReset - currentRound;
        if (state == GameState.RUNNING || state == GameState.ENDING) {
            remaining++;
        }
        return Math.max(1, remaining);
    }

    private void refreshVisibility() {
        for (UUID viewerId : players) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }

            for (UUID targetId : players) {
                if (viewerId.equals(targetId)) {
                    continue;
                }

                Player target = Bukkit.getPlayer(targetId);
                if (target == null || !target.isOnline()) {
                    continue;
                }

                if (spectators.contains(targetId) && activePlayers.contains(viewerId)) {
                    viewer.hidePlayer(manager.getPlugin(), target);
                } else {
                    viewer.showPlayer(manager.getPlugin(), target);
                }
            }
        }
    }

    private void revealToArenaPlayers(Player player) {
        for (UUID viewerId : players) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline() && !viewer.getUniqueId().equals(player.getUniqueId())) {
                viewer.showPlayer(manager.getPlugin(), player);
            }
        }
    }

    private void revealArenaPlayersTo(Player viewer) {
        for (UUID targetId : players) {
            if (viewer.getUniqueId().equals(targetId)) {
                continue;
            }

            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                viewer.showPlayer(manager.getPlugin(), target);
            }
        }
    }

    private Location getSpectatorLocation(Location source) {
        if (source != null && isWorld(source.getWorld()) && source.getY() >= eliminateBelowY) {
            return source.clone().add(0.0D, 1.0D, 0.0D);
        }
        if (waitingSpawn != null) {
            return waitingSpawn.clone();
        }
        return new Location(world, region.getCenterX(), region.getMaxY() + 3.0D, region.getCenterZ());
    }

    private void beginSuddenNightSessionIfNeeded() {
        if (suddenNightSessionActive) {
            return;
        }
        suddenNightSessionActive = manager.startSuddenNightSession(this);
    }

    private void endSuddenNightSessionIfNeeded() {
        if (!suddenNightSessionActive) {
            return;
        }
        manager.endSuddenNightSession(this);
        suddenNightSessionActive = false;
    }

    private void playArenaSound(Sound sound, float volume, float pitch) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
            }
        }
    }

    private boolean isSoloTestRound() {
        return manager.isSoloTestMode() && players.size() == 1 && activePlayers.size() == 1;
    }

    private void lockMovement(Player player) {
        if (roundStartFreezeTicks <= 0) {
            return;
        }
        movementLockUntil.put(player.getUniqueId(), System.currentTimeMillis() + (roundStartFreezeTicks * 50L));
    }

    public void refreshPlayerLocale(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        applyUi(player);
        updateSidebar();
        if (state == GameState.WAITING) {
            updateWaitingBar();
        } else if (state == GameState.COUNTDOWN) {
            updateCountdownBar();
        } else if (state == GameState.RUNNING) {
            updateRunningBar();
        } else if (state == GameState.ENDING) {
            updateIntermissionBar();
        }
    }

    private ArenaUiBundle createUiBundle(PlayerLanguage language) {
        Scoreboard sidebar = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = sidebar.registerNewObjective("sr_" + id + "_" + language.getCode(), "dummy", ChatColor.AQUA + "SkyRandom");
        objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
        Team round = createSidebarLine(sidebar, objective, "round_" + language.getCode(), ROUND_ENTRY, 4);
        Team alive = createSidebarLine(sidebar, objective, "alive_" + language.getCode(), ALIVE_ENTRY, 3);
        Team drop = createSidebarLine(sidebar, objective, "drop_" + language.getCode(), DROP_ENTRY, 2);
        Team cleanup = createSidebarLine(sidebar, objective, "clean_" + language.getCode(), CLEANUP_ENTRY, 1);
        BossBar bossBar = Bukkit.createBossBar(manager.tr(language, "ui.title"), BarColor.BLUE, BarStyle.SOLID);
        return new ArenaUiBundle(language, sidebar, objective, round, alive, drop, cleanup, bossBar);
    }

    private Team createSidebarLine(Scoreboard scoreboard, Objective objective, String teamName, String entry, int score) {
        Team team = scoreboard.registerNewTeam(teamName);
        team.addEntry(entry);
        objective.getScore(entry).setScore(score);
        return team;
    }

    private void applyUi(Player player) {
        PlayerLanguage language = manager.getLanguage(player);
        ArenaUiBundle targetBundle = uiBundles.getOrDefault(language, uiBundles.get(PlayerLanguage.RU));
        if (targetBundle == null) {
            return;
        }

        player.setScoreboard(targetBundle.scoreboard());
        for (ArenaUiBundle bundle : uiBundles.values()) {
            if (bundle == targetBundle) {
                bundle.bar().addPlayer(player);
            } else {
                bundle.bar().removePlayer(player);
            }
        }
    }

    private void removeUi(Player player) {
        for (ArenaUiBundle bundle : uiBundles.values()) {
            bundle.bar().removePlayer(player);
        }
    }

    private void setBarsVisible(boolean visible) {
        for (ArenaUiBundle bundle : uiBundles.values()) {
            bundle.bar().setVisible(visible);
        }
    }

    private void updateBarsAppearance(BarColor color, double progress, String titleKey, boolean visible, Object... replacements) {
        for (Map.Entry<PlayerLanguage, ArenaUiBundle> entry : uiBundles.entrySet()) {
            ArenaUiBundle bundle = entry.getValue();
            bundle.bar().setVisible(visible);
            bundle.bar().setColor(color);
            bundle.bar().setProgress(progress);
            if (titleKey != null && !titleKey.isBlank()) {
                bundle.bar().setTitle(manager.tr(entry.getKey(), titleKey, replacements));
            }
        }
    }

    private void captureBaselineBlocks() {
        baselineBlocks.clear();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isAir()) {
                        continue;
                    }
                    baselineBlocks.put(new BlockKey(x, y, z), block.getBlockData().getAsString());
                }
            }
        }
    }

    private boolean isChangedFromBaseline(Block block) {
        String baselineData = baselineBlocks.get(BlockKey.of(block));
        if (baselineData == null) {
            return !block.getType().isAir();
        }
        return !baselineData.equals(block.getBlockData().getAsString());
    }

    private void restoreArenaRegion() {
        removeArenaEntities();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    restoreBlock(new BlockKey(x, y, z));
                }
            }
        }
        placedBlocks.clear();
    }

    private void startArenaRegionCleanup(Runnable onComplete) {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }

        removeArenaEntities();
        CleanupCursor cursor = new CleanupCursor(minX, maxX, minY, maxY, minZ, maxZ);
        broadcastLocalized("arena.cleanup_started");
        updateCleanupBar(0.0D);

        cleanupTask = manager.getPlugin().getServer().getScheduler().runTaskTimer(
            manager.getPlugin(),
            () -> {
                int processedThisTick = 0;
                while (processedThisTick < cleanupBlocksPerTick && cursor.hasNext()) {
                    restoreBlock(cursor.next());
                    processedThisTick++;
                }

                updateCleanupBar(cursor.progress());
                if (cursor.hasNext()) {
                    return;
                }

                if (cleanupTask != null) {
                    cleanupTask.cancel();
                    cleanupTask = null;
                }
                placedBlocks.clear();
                onComplete.run();
            },
            1L,
            1L
        );
    }

    private void restoreBlock(BlockKey key) {
        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        String baselineData = baselineBlocks.get(key);
        if (baselineData == null) {
            if (!block.getType().isAir()) {
                block.setType(Material.AIR, false);
            }
            return;
        }

        String currentData = block.getBlockData().getAsString();
        if (!baselineData.equals(currentData)) {
            block.setBlockData(Bukkit.createBlockData(baselineData), false);
        }
    }

    private void removeArenaEntities() {
        for (Entity entity : world.getNearbyEntities(region)) {
            if (entity instanceof Player) {
                continue;
            }
            entity.remove();
        }
    }

    private record ArenaUiBundle(
        PlayerLanguage language,
        Scoreboard scoreboard,
        Objective objective,
        Team roundTeam,
        Team aliveTeam,
        Team dropTeam,
        Team cleanupTeam,
        BossBar bar
    ) {
    }

    private record BlockKey(int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(block.getX(), block.getY(), block.getZ());
        }
    }

    private record FoodState(int foodLevel, float saturation, float exhaustion) {
        private static FoodState of(Player player) {
            return new FoodState(player.getFoodLevel(), player.getSaturation(), player.getExhaustion());
        }

        private void restore(Player player) {
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);
            player.setExhaustion(exhaustion);
        }
    }

    private enum RandomRoundEvent {
        LOW_GRAVITY("arena.random_event_low_gravity", "arena.random_event_low_gravity_subtitle"),
        LUCKY_DROPS("arena.random_event_lucky_drops", "arena.random_event_lucky_drops_subtitle"),
        RESISTANCE("arena.random_event_resistance", "arena.random_event_resistance_subtitle"),
        SUDDEN_HUNGER("arena.random_event_sudden_hunger", "arena.random_event_sudden_hunger_subtitle"),
        FIRE_FEET("arena.random_event_fire_feet", "arena.random_event_fire_feet_subtitle");

        private static final RandomRoundEvent[] VALUES = values();

        private final String nameKey;
        private final String subtitleKey;

        RandomRoundEvent(String nameKey, String subtitleKey) {
            this.nameKey = nameKey;
            this.subtitleKey = subtitleKey;
        }

        private String nameKey() {
            return nameKey;
        }

        private String subtitleKey() {
            return subtitleKey;
        }

        private static RandomRoundEvent random() {
            return VALUES[ThreadLocalRandom.current().nextInt(VALUES.length)];
        }
    }

    private static final class CleanupCursor {

        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private final long totalBlocks;

        private int x;
        private int y;
        private int z;
        private long processedBlocks;

        private CleanupCursor(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.x = minX;
            this.y = minY;
            this.z = minZ;
            this.totalBlocks = ((long) maxX - minX + 1L)
                * ((long) maxY - minY + 1L)
                * ((long) maxZ - minZ + 1L);
        }

        private boolean hasNext() {
            return processedBlocks < totalBlocks;
        }

        private BlockKey next() {
            BlockKey key = new BlockKey(x, y, z);
            processedBlocks++;
            advance();
            return key;
        }

        private double progress() {
            if (totalBlocks <= 0L) {
                return 1.0D;
            }
            return processedBlocks / (double) totalBlocks;
        }

        private void advance() {
            z++;
            if (z <= maxZ) {
                return;
            }
            z = minZ;
            y++;
            if (y <= maxY) {
                return;
            }
            y = minY;
            x++;
            if (x > maxX) {
                x = maxX;
                y = maxY;
                z = maxZ;
            }
        }
    }
}
