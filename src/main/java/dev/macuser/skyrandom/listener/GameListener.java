package dev.macuser.skyrandom.listener;

import dev.macuser.skyrandom.game.Arena;
import dev.macuser.skyrandom.game.GameManager;
import dev.macuser.skyrandom.game.GameState;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        gameManager.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameManager.handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (gameManager.isDedicatedServerMode()) {
            Location lobby = gameManager.getLobbyOrFallback(event.getRespawnLocation());
            if (lobby != null && lobby.getWorld() != null) {
                event.setRespawnLocation(lobby);
            }
            gameManager.handleRespawn(event.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Arena arena = gameManager.getArena(player);
        if (arena == null || arena.getState() != GameState.RUNNING || !arena.isActivePlayer(player)) {
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (arena.isMovementLocked(player)) {
            if (event.getFrom().getX() != to.getX()
                || event.getFrom().getY() != to.getY()
                || event.getFrom().getZ() != to.getZ()) {
                event.setTo(event.getFrom());
                return;
            }
        }

        if (!arena.containsPlayer(to) || gameManager.isInsideProtectedLobbyZone(to)) {
            arena.eliminate(player, "reason.left_arena");
            return;
        }

        if (to.getY() < arena.getEliminateBelowY()) {
            arena.eliminate(player, "reason.fell_down");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        Arena attackerArena = attacker == null ? null : gameManager.getArena(attacker);
        if (attacker != null) {
            if (attackerArena != null && !attackerArena.isActivePlayer(attacker)) {
                event.setCancelled(true);
                return;
            }
        }

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Arena arena = gameManager.getArena(victim);
        if (arena == null || arena.getState() != GameState.RUNNING || !arena.isActivePlayer(victim)) {
            return;
        }

        if (attacker == null) {
            return;
        }

        if (attackerArena != arena || !arena.isActivePlayer(attacker)) {
            event.setCancelled(true);
            return;
        }

        gameManager.recordPlayerAttack(victim, attacker);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Arena arena = gameManager.getArena(player);
        if (arena == null) {
            return;
        }

        if (arena.getState() != GameState.RUNNING || !arena.isActivePlayer(player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getFinalDamage() >= player.getHealth()) {
            if (tryUseTotem(player)) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            String reason = switch (event.getCause()) {
                case VOID -> "reason.death_void";
                case FALL -> "reason.death_fall";
                case LAVA, FIRE, FIRE_TICK -> "reason.death_burn";
                default -> "reason.death_fatal";
            };
            arena.eliminate(player, reason);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && gameManager.getArena(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpectatorInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Arena arena = gameManager.getArena(player);
        if (arena == null || !arena.isSpectator(player)) {
            if (event.getHand() == EquipmentSlot.HAND) {
                if (gameManager.isLanguageSelectorItem(event.getItem())) {
                    event.setCancelled(true);
                    gameManager.openProfileMenu(player);
                    return;
                }
                if (gameManager.isLobbyHostItem(event.getItem())) {
                    event.setCancelled(true);
                    gameManager.openHostMenu(player);
                }
            }
            return;
        }

        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (gameManager.isSpectatorTeleportItem(event.getItem())) {
            gameManager.openSpectatorTeleportMenu(player);
            return;
        }

        if (gameManager.isSpectatorExitItem(event.getItem())) {
            arena.leave(player, "reason.spectator_leave");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpectatorInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (gameManager.isProfileMenu(event.getView().getTopInventory())) {
                event.setCancelled(true);
                if (event.getClickedInventory() == event.getView().getTopInventory()) {
                    gameManager.handleProfileMenuClick(player, event.getCurrentItem());
                }
                return;
            }

            if (gameManager.isStatsMenu(event.getView().getTopInventory())) {
                event.setCancelled(true);
                if (event.getClickedInventory() == event.getView().getTopInventory()) {
                    gameManager.handleStatsMenuClick(player, event.getCurrentItem());
                }
                return;
            }

            if (gameManager.isLanguageMenu(event.getView().getTopInventory())) {
                event.setCancelled(true);
                if (event.getClickedInventory() == event.getView().getTopInventory()) {
                    gameManager.handleLanguageMenuClick(player, event.getCurrentItem());
                }
                return;
            }

            if (gameManager.isAboutMenu(event.getView().getTopInventory())) {
                event.setCancelled(true);
                if (event.getClickedInventory() == event.getView().getTopInventory()) {
                    gameManager.handleAboutMenuClick(player, event.getCurrentItem());
                }
                return;
            }

            if (gameManager.isHostMenu(event.getView().getTopInventory())) {
                event.setCancelled(true);
                if (event.getClickedInventory() == event.getView().getTopInventory()) {
                    gameManager.handleHostMenuClick(player, event.getCurrentItem());
                }
                return;
            }

            if (gameManager.isHostTransferMenu(event.getView().getTopInventory())) {
                event.setCancelled(true);
                if (event.getClickedInventory() == event.getView().getTopInventory()) {
                    gameManager.handleHostTransferMenuClick(player, event.getCurrentItem());
                }
                return;
            }

            if (gameManager.isSpectatorTeleportMenu(event.getView().getTopInventory())) {
                event.setCancelled(true);
                if (event.getClickedInventory() == event.getView().getTopInventory()) {
                    gameManager.handleSpectatorTeleportMenuClick(player, event.getCurrentItem());
                }
                return;
            }

            Arena arena = gameManager.getArena(player);
            if (arena != null && arena.isSpectator(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpectatorDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Arena arena = gameManager.getArena(player);
        if ((arena != null && arena.isSpectator(player))
            || gameManager.isLanguageSelectorItem(event.getItemDrop().getItemStack())
            || gameManager.isSpectatorTeleportItem(event.getItemDrop().getItemStack())
            || gameManager.isLobbyHostItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Arena regionArena = gameManager.findArenaByLocation(event.getBlockPlaced().getLocation());
        if (regionArena == null) {
            return;
        }

        if (gameManager.getArena(event.getPlayer()) != regionArena || !regionArena.handleBlockPlace(event.getPlayer(), event.getBlockPlaced())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Arena regionArena = gameManager.findArenaByLocation(event.getBlock().getLocation());
        if (regionArena == null) {
            return;
        }

        if (gameManager.getArena(event.getPlayer()) != regionArena || !regionArena.handleBlockBreak(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        if (gameManager.shouldBlockArenaHostileSpawn(event.getLocation(), event.getSpawnReason())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Location targetLocation = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();
        Arena regionArena = gameManager.findArenaByLocation(targetLocation);
        if (regionArena == null) {
            return;
        }

        if (gameManager.getArena(event.getPlayer()) != regionArena
            || !regionArena.handleBlockPlace(event.getPlayer(), targetLocation.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location location = event.getLocation();
        gameManager.filterExplosion(event.blockList(), location);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location location = event.getBlock().getLocation();
        gameManager.filterExplosion(event.blockList(), location);
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private boolean isVanillaHostileSpawn(CreatureSpawnEvent.SpawnReason reason) {
        return switch (reason) {
            case NATURAL, PATROL, REINFORCEMENTS -> true;
            default -> false;
        };
    }

    private boolean tryUseTotem(Player player) {
        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isTotem(offHand)) {
            decrementTotem(player, EquipmentSlot.OFF_HAND, offHand);
            applyTotemRescue(player);
            return true;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isTotem(mainHand)) {
            decrementTotem(player, EquipmentSlot.HAND, mainHand);
            applyTotemRescue(player);
            return true;
        }
        return false;
    }

    private boolean isTotem(ItemStack stack) {
        return stack != null && stack.getType() == Material.TOTEM_OF_UNDYING;
    }

    private void decrementTotem(Player player, EquipmentSlot slot, ItemStack stack) {
        if (stack.getAmount() <= 1) {
            if (slot == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            return;
        }
        stack.setAmount(stack.getAmount() - 1);
    }

    private void applyTotemRescue(Player player) {
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
            ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()
            : 20.0D;
        player.setHealth(Math.min(maxHealth, 1.0D));
        player.setFireTicks(0);
        for (PotionEffect activeEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(activeEffect.getType());
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0, true, true, true));
        player.playEffect(EntityEffect.TOTEM_RESURRECT);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0F, 1.0F);
    }
}
