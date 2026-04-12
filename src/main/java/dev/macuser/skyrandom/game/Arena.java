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
    private final int dropIntervalTicks;
    private final int winDelaySeconds;
    private final int resetWinDelaySeconds;
    private final double eliminateBelowY;
    private final int maxBuildY;
    private final double dropHeightOffset;
    private final boolean allowPlaceBlocks;
    private final boolean allowBreakMapBlocks;
    private final boolean onlyBreakOwnPlacedBlocks;
    private final int roundsBeforeReset;
    private final int roundStartFreezeTicks;
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
    private BukkitTask finishTask;
    private int countdownRemaining;
    private int ticksUntilNextDrop;
    private int intermissionRemaining;
    private int intermissionDurationSeconds;
    private int currentRound;
    private long roundStartedAtMillis;

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
        this.minX = (int) Math.floor(region.getMinX());
        this.maxX = (int) Math.floor(region.getMaxX());
        this.minY = (int) Math.floor(region.getMinY());
        this.maxY = (int) Math.floor(region.getMaxY());
        this.minZ = (int) Math.floor(region.getMinZ());
        this.maxZ = (int) Math.floor(region.getMaxZ());
        this.borderCenterX = (region.getMinX() + region.getMaxX()) / 2.0D;
        this.borderCenterZ = (region.getMinZ() + region.getMaxZ()) / 2.0D;
        this.borderSize = Math.max(region.getMaxX() - region.getMinX(), region.getMaxZ() - region.getMinZ());
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

    public int getRoundsBeforeReset() {
        return roundsBeforeReset;
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
        if (!removedFromQueue && !removedFromRound && !removedFromSpectators) {
            return;
        }

        removeUi(player);
        revealToArenaPlayers(player);
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
        Player attacker = manager.findRecentAttacker(player);
        if (!isSoloTestRound()) {
            manager.recordDeath(player);
            if (attacker != null) {
                manager.recordKill(attacker);
            }
        }
        manager.prepareSpectatorPlayer(player, getSpectatorLocation(player.getLocation()));
        manager.applyArenaWorldBorder(player, this);
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
            if (allowBreakMapBlocks || owner != null) {
                placedBlocks.remove(key);
                continue;
            }

            iterator.remove();
        }
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
            manager.applyArenaWorldBorder(player, this);
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

        refreshVisibility();
        updateSidebar();
        broadcastLocalized("arena.match_started", "current", currentRound, "max", roundsBeforeReset);
        if (activePlayers.size() <= 1 && !isSoloTestRound()) {
            checkForWinner();
            return;
        }
        startDrops();
    }

    private void startDrops() {
        if (dropIntervalTicks <= 0 || manager.getDropTable().isEmpty()) {
            setBarsVisible(false);
            updateSidebar();
            return;
        }

        ticksUntilNextDrop = dropIntervalTicks;
        updateDropBar();

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
                        manager.getDropTable().roll(player, this);
                    }
                    ticksUntilNextDrop = dropIntervalTicks;
                }

                updateDropBar();
            },
            1L,
            1L
        );
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
        boolean reachedResetRound = currentRound >= roundsBeforeReset;
        if (reachedResetRound) {
            restoreArenaRegion();
            currentRound = 0;
            broadcastLocalized("arena.cleanup_after", "rounds", roundsBeforeReset);
        }

        activePlayers.clear();
        spectators.clear();
        movementLockUntil.clear();
        state = GameState.WAITING;
        countdownRemaining = 0;
        ticksUntilNextDrop = 0;
        intermissionRemaining = 0;
        intermissionDurationSeconds = 0;
        roundStartedAtMillis = 0L;
        currentRoundParticipants.clear();

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
            resetArena(true);
            return;
        }

        refreshVisibility();
        updateSidebar();
        evaluateCountdown();
    }

    private void resetArena(boolean cleanupBlocks) {
        cancelAllTasks();
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
        if (finishTask != null) {
            finishTask.cancel();
            finishTask = null;
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

    private Location getSpectatorLocation(Location source) {
        if (source != null && isWorld(source.getWorld()) && source.getY() >= eliminateBelowY) {
            return source.clone().add(0.0D, 1.0D, 0.0D);
        }
        if (waitingSpawn != null) {
            return waitingSpawn.clone();
        }
        return new Location(world, region.getCenterX(), region.getMaxY() + 3.0D, region.getCenterZ());
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
        } else if (state == GameState.RUNNING && dropTask != null) {
            updateDropBar();
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
                    BlockKey key = new BlockKey(x, y, z);
                    Block block = world.getBlockAt(x, y, z);
                    String baselineData = baselineBlocks.get(key);
                    if (baselineData == null) {
                        if (!block.getType().isAir()) {
                            block.setType(Material.AIR, false);
                        }
                        continue;
                    }

                    String currentData = block.getBlockData().getAsString();
                    if (!baselineData.equals(currentData)) {
                        block.setBlockData(Bukkit.createBlockData(baselineData), false);
                    }
                }
            }
        }
        placedBlocks.clear();
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
}
