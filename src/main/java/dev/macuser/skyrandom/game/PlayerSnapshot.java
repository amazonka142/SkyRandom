package dev.macuser.skyrandom.game;

import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;

public final class PlayerSnapshot {

    private final Location location;
    private final GameMode gameMode;
    private final ItemStack[] contents;
    private final ItemStack[] armorContents;
    private final ItemStack[] extraContents;
    private final Collection<PotionEffect> potionEffects;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int level;
    private final float exp;
    private final boolean allowFlight;
    private final boolean flying;
    private final boolean collidable;
    private final boolean canPickupItems;
    private final float flySpeed;
    private final Scoreboard scoreboard;

    private PlayerSnapshot(
        Location location,
        GameMode gameMode,
        ItemStack[] contents,
        ItemStack[] armorContents,
        ItemStack[] extraContents,
        Collection<PotionEffect> potionEffects,
        double health,
        int foodLevel,
        float saturation,
        int level,
        float exp,
        boolean allowFlight,
        boolean flying,
        boolean collidable,
        boolean canPickupItems,
        float flySpeed,
        Scoreboard scoreboard
    ) {
        this.location = location;
        this.gameMode = gameMode;
        this.contents = contents;
        this.armorContents = armorContents;
        this.extraContents = extraContents;
        this.potionEffects = potionEffects;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.level = level;
        this.exp = exp;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.collidable = collidable;
        this.canPickupItems = canPickupItems;
        this.flySpeed = flySpeed;
        this.scoreboard = scoreboard;
    }

    public static PlayerSnapshot capture(Player player) {
        PlayerInventory inventory = player.getInventory();
        return new PlayerSnapshot(
            player.getLocation().clone(),
            player.getGameMode(),
            inventory.getContents().clone(),
            inventory.getArmorContents().clone(),
            inventory.getExtraContents().clone(),
            new ArrayList<>(player.getActivePotionEffects()),
            player.getHealth(),
            player.getFoodLevel(),
            player.getSaturation(),
            player.getLevel(),
            player.getExp(),
            player.getAllowFlight(),
            player.isFlying(),
            player.isCollidable(),
            player.getCanPickupItems(),
            player.getFlySpeed(),
            player.getScoreboard()
        );
    }

    public void restore(Player player, Location overrideLocation) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setContents(contents.clone());
        inventory.setArmorContents(armorContents.clone());
        inventory.setExtraContents(extraContents.clone());

        for (PotionEffect activeEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(activeEffect.getType());
        }
        for (PotionEffect potionEffect : potionEffects) {
            player.addPotionEffect(potionEffect);
        }

        player.setGameMode(gameMode);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);
        player.setCollidable(collidable);
        player.setCanPickupItems(canPickupItems);
        player.setFlySpeed(flySpeed);
        if (scoreboard != null) {
            player.setScoreboard(scoreboard);
        }
        player.setFireTicks(0);
        player.setFallDistance(0.0F);

        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
            ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
            : 20.0D;
        player.setHealth(Math.max(1.0D, Math.min(maxHealth, health)));

        Location target = overrideLocation != null ? overrideLocation : location;
        if (target != null && target.getWorld() != null) {
            player.teleport(target);
        }
    }
}
