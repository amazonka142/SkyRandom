package dev.macuser.skyrandom.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class DropTable {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final DropTable EMPTY = new DropTable(List.of());
    private static final int REROLL_ATTEMPTS = 10;
    private static final int RECENT_HISTORY_SIZE = 3;
    private static final Set<Material> BLOCKED_RANDOM_BLOCKS = Set.of(
        Material.BEDROCK,
        Material.BARRIER,
        Material.COMMAND_BLOCK,
        Material.CHAIN_COMMAND_BLOCK,
        Material.REPEATING_COMMAND_BLOCK,
        Material.STRUCTURE_BLOCK,
        Material.JIGSAW,
        Material.SPAWNER,
        Material.LIGHT
    );
    private static final List<Material> DEFAULT_RANDOM_BLOCK_POOL = buildDefaultRandomBlockPool();

    private final List<DropGroup> groups;
    private final double totalWeight;
    private final Map<java.util.UUID, Deque<String>> recentDropsByPlayer = new HashMap<>();

    private DropTable(List<DropGroup> groups) {
        this.groups = List.copyOf(groups);
        this.totalWeight = groups.stream().mapToDouble(DropGroup::weight).sum();
    }

    public static DropTable empty() {
        return EMPTY;
    }

    public static DropTable fromConfig(ConfigurationSection section, Logger logger) {
        if (section == null) {
            return EMPTY;
        }

        List<DropGroup> groups = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection groupSection = section.getConfigurationSection(key);
            if (groupSection == null) {
                continue;
            }

            double groupWeight = groupSection.getDouble("chance", 0.0D);
            if (groupWeight <= 0.0D) {
                logger.warning("Drop group '" + key + "' skipped because chance <= 0.");
                continue;
            }

            List<DropAction> entries = new ArrayList<>();
            List<Map<?, ?>> rawEntries = groupSection.getMapList("entries");
            for (Map<?, ?> rawEntry : rawEntries) {
                if (rawEntry.isEmpty()) {
                    continue;
                }

                ConfigurationSection entrySection = toSection(rawEntry);
                DropAction action = parseEntry(entrySection, logger);
                if (action != null) {
                    entries.add(action);
                }
            }

            if (!entries.isEmpty()) {
                groups.add(new DropGroup(key, groupWeight, entries));
            } else {
                logger.warning("Drop group '" + key + "' has no valid entries and was skipped.");
            }
        }

        return groups.isEmpty() ? EMPTY : new DropTable(groups);
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }

    public void roll(Player player, Arena arena) {
        if (groups.isEmpty()) {
            return;
        }

        Deque<String> recent = recentDropsByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        DropAction fallback = null;

        for (int attempt = 0; attempt < REROLL_ATTEMPTS; attempt++) {
            DropGroup group = pickWeighted(groups, totalWeight);
            if (group == null) {
                return;
            }

            DropAction action = group.pickEntry();
            if (action == null) {
                continue;
            }

            if (fallback == null) {
                fallback = action;
            }

            if (!recent.contains(action.key())) {
                action.apply(player, arena);
                rememberDrop(recent, action.key());
                return;
            }
        }

        if (fallback != null) {
            fallback.apply(player, arena);
            rememberDrop(recent, fallback.key());
        }
    }

    private static DropAction parseEntry(ConfigurationSection section, Logger logger) {
        String type = section.getString("type", "").toLowerCase(Locale.ROOT);
        double weight = parsePositiveDouble(section.get("weight"), 1.0D);
        return switch (type) {
            case "item" -> parseItem(section, weight, logger);
            case "bundle" -> parseBundle(section, weight, logger);
            case "random_block" -> parseRandomBlock(section, weight, logger);
            case "effect" -> parseEffect(section, weight, logger);
            case "mob" -> parseMob(section, weight, logger);
            default -> {
                logger.warning("Unknown drop type '" + type + "'.");
                yield null;
            }
        };
    }

    private static DropAction parseItem(ConfigurationSection section, double weight, Logger logger) {
        String materialName = section.getString("material");
        Material material = materialName == null ? null : Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            logger.warning("Invalid item material '" + materialName + "'.");
            return null;
        }

        AmountRange amount = AmountRange.parse(section.get("amount"));
        String customName = section.getString("name");

        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        ConfigurationSection enchantSection = section.getConfigurationSection("enchants");
        if (enchantSection != null) {
            for (String enchantKey : enchantSection.getKeys(false)) {
                Enchantment enchantment = Registry.ENCHANTMENT.get(
                    NamespacedKey.minecraft(enchantKey.toLowerCase(Locale.ROOT))
                );
                if (enchantment == null) {
                    logger.warning("Invalid enchantment '" + enchantKey + "'.");
                    continue;
                }
                enchantments.put(enchantment, Math.max(1, enchantSection.getInt(enchantKey, 1)));
            }
        }

        return new ItemDrop(weight, material, amount, customName, enchantments);
    }

    private static DropAction parseBundle(ConfigurationSection section, double weight, Logger logger) {
        List<ItemDrop> itemDrops = new ArrayList<>();
        List<Map<?, ?>> rawItems = section.getMapList("items");
        for (Map<?, ?> rawItem : rawItems) {
            if (rawItem.isEmpty()) {
                continue;
            }

            DropAction action = parseItem(toSection(rawItem), 1.0D, logger);
            if (action instanceof ItemDrop itemDrop) {
                itemDrops.add(itemDrop);
            }
        }

        if (itemDrops.isEmpty()) {
            logger.warning("Bundle drop has no valid item entries.");
            return null;
        }

        String bundleKey = section.getString("key", "bundle");
        bundleKey = bundleKey.toLowerCase(Locale.ROOT).replace(' ', '_');
        return new BundleDrop(weight, bundleKey, itemDrops);
    }

    private static DropAction parseRandomBlock(ConfigurationSection section, double weight, Logger logger) {
        List<String> configuredMaterials = section.getStringList("materials");
        List<Material> pool = configuredMaterials.isEmpty()
            ? DEFAULT_RANDOM_BLOCK_POOL
            : buildConfiguredMaterialPool(configuredMaterials, logger);

        if (pool.isEmpty()) {
            logger.warning("Random block pool is empty.");
            return null;
        }

        AmountRange amount = AmountRange.parse(section.get("amount"));
        return new RandomBlockDrop(weight, amount, pool);
    }

    private static DropAction parseEffect(ConfigurationSection section, double weight, Logger logger) {
        String effectName = section.getString("effect");
        PotionEffectType effectType = effectName == null ? null
            : Registry.MOB_EFFECT.get(NamespacedKey.minecraft(effectName.toLowerCase(Locale.ROOT)));
        if (effectType == null) {
            logger.warning("Invalid effect '" + effectName + "'.");
            return null;
        }

        Object durationConfig = section.get("duration-seconds");
        AmountRange durationSeconds = durationConfig == null ? new AmountRange(5, 5) : AmountRange.parse(durationConfig);
        int amplifier = Math.max(0, section.getInt("amplifier", 0));
        return new EffectDrop(weight, effectType, durationSeconds, amplifier);
    }

    private static DropAction parseMob(ConfigurationSection section, double weight, Logger logger) {
        String entityName = section.getString("entity");
        EntityType entityType;
        try {
            entityType = entityName == null ? null : EntityType.valueOf(entityName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            entityType = null;
        }

        if (entityType == null || !entityType.isSpawnable() || !entityType.isAlive()) {
            logger.warning("Invalid mob entity '" + entityName + "'.");
            return null;
        }

        AmountRange amount = AmountRange.parse(section.get("amount"));
        return new MobDrop(weight, entityType, amount);
    }

    private static DropGroup pickWeighted(List<DropGroup> groups, double totalWeight) {
        if (groups.isEmpty() || totalWeight <= 0.0D) {
            return null;
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cursor = 0.0D;
        for (DropGroup group : groups) {
            cursor += group.weight();
            if (roll <= cursor) {
                return group;
            }
        }
        return groups.get(groups.size() - 1);
    }

    private static double parsePositiveDouble(Object raw, double fallback) {
        if (raw instanceof Number number) {
            return Math.max(0.0D, number.doubleValue());
        }
        if (raw instanceof String string) {
            try {
                return Math.max(0.0D, Double.parseDouble(string));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static ConfigurationSection toSection(Map<?, ?> values) {
        MemoryConfiguration configuration = new MemoryConfiguration();
        populateSection(configuration, values);
        return configuration;
    }

    @SuppressWarnings("unchecked")
    private static void populateSection(ConfigurationSection section, Map<?, ?> values) {
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nestedMap) {
                ConfigurationSection nestedSection = section.createSection(key);
                populateSection(nestedSection, nestedMap);
                continue;
            }
            if (value instanceof List<?> list) {
                List<Object> normalized = new ArrayList<>();
                for (Object element : list) {
                    if (element instanceof Map<?, ?> map) {
                        MemoryConfiguration nestedConfig = new MemoryConfiguration();
                        populateSection(nestedConfig, map);
                        normalized.add(nestedConfig.getValues(true));
                    } else {
                        normalized.add(element);
                    }
                }
                section.set(key, normalized);
                continue;
            }
            section.set(key, value);
        }
    }

    private static String prettify(String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static List<Material> buildConfiguredMaterialPool(List<String> configuredMaterials, Logger logger) {
        List<Material> resolved = new ArrayList<>();
        for (String materialName : configuredMaterials) {
            Material material = materialName == null ? null : Material.matchMaterial(materialName);
            if (material == null || !isAllowedRandomBlock(material)) {
                logger.warning("Random block material '" + materialName + "' is not allowed.");
                continue;
            }
            resolved.add(material);
        }
        return resolved.stream()
            .distinct()
            .sorted(java.util.Comparator.comparing(Enum::name))
            .toList();
    }

    private static List<Material> buildDefaultRandomBlockPool() {
        return Arrays.stream(Material.values())
            .filter(DropTable::isAllowedRandomBlock)
            .distinct()
            .sorted(java.util.Comparator.comparing(Enum::name))
            .toList();
    }

    private static boolean isAllowedRandomBlock(Material material) {
        return material != null
            && material.isBlock()
            && material.isItem()
            && !material.isAir()
            && !BLOCKED_RANDOM_BLOCKS.contains(material);
    }

    private static void giveOrDrop(Player player, Arena arena, ItemStack prototype, int totalAmount) {
        int remaining = Math.max(1, totalAmount);
        while (remaining > 0) {
            int stackAmount = Math.min(Math.max(1, prototype.getMaxStackSize()), remaining);
            ItemStack stack = prototype.clone();
            stack.setAmount(stackAmount);

            Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
            if (!overflow.isEmpty()) {
                Location dropLocation = arena.getDropLocation(player.getLocation());
                for (ItemStack overflowItem : overflow.values()) {
                    dropLocation.getWorld().dropItem(dropLocation, overflowItem);
                }
            }

            remaining -= stackAmount;
        }
    }

    private static void rememberDrop(Deque<String> recent, String key) {
        recent.addLast(key);
        while (recent.size() > RECENT_HISTORY_SIZE) {
            recent.removeFirst();
        }
    }

    private interface DropAction {
        double weight();

        String key();

        void apply(Player player, Arena arena);
    }

    private record DropGroup(String key, double weight, List<DropAction> entries) {
        private double entryWeightSum() {
            return entries.stream().mapToDouble(DropAction::weight).sum();
        }

        private DropAction pickEntry() {
            double totalWeight = entryWeightSum();
            if (entries.isEmpty() || totalWeight <= 0.0D) {
                return null;
            }

            double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
            double cursor = 0.0D;
            for (DropAction entry : entries) {
                cursor += entry.weight();
                if (roll <= cursor) {
                    return entry;
                }
            }
            return entries.get(entries.size() - 1);
        }
    }

    private record ItemDrop(
        double weight,
        Material material,
        AmountRange amount,
        String customName,
        Map<Enchantment, Integer> enchantments
    ) implements DropAction {

        @Override
        public String key() {
            return "item:" + material.name();
        }

        @Override
        public void apply(Player player, Arena arena) {
            int rolledAmount = amount.roll();
            ItemStack itemStack = new ItemStack(material);
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (customName != null && !customName.isBlank()) {
                    meta.displayName(LEGACY.deserialize(customName));
                }
                for (Map.Entry<Enchantment, Integer> enchantmentEntry : enchantments.entrySet()) {
                    meta.addEnchant(enchantmentEntry.getKey(), enchantmentEntry.getValue(), true);
                }
                if (!enchantments.isEmpty()) {
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                itemStack.setItemMeta(meta);
            }

            giveOrDrop(player, arena, itemStack, rolledAmount);
            arena.sendLocalized(player, "drop.item", "item", prettify(material.name()), "amount", rolledAmount);
        }
    }

    private record BundleDrop(double weight, String bundleKey, List<ItemDrop> itemDrops) implements DropAction {

        @Override
        public String key() {
            return "bundle:" + bundleKey;
        }

        @Override
        public void apply(Player player, Arena arena) {
            for (ItemDrop itemDrop : itemDrops) {
                itemDrop.apply(player, arena);
            }
        }
    }

    private record RandomBlockDrop(double weight, AmountRange amount, List<Material> pool) implements DropAction {

        @Override
        public String key() {
            return "random_block";
        }

        @Override
        public void apply(Player player, Arena arena) {
            Material rolledMaterial = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            int rolledAmount = amount.roll();
            giveOrDrop(player, arena, new ItemStack(rolledMaterial), rolledAmount);
            arena.sendLocalized(player, "drop.random_block", "item", prettify(rolledMaterial.name()), "amount", rolledAmount);
        }
    }

    private record EffectDrop(
        double weight,
        PotionEffectType effectType,
        AmountRange durationSeconds,
        int amplifier
    ) implements DropAction {

        @Override
        public String key() {
            return "effect:" + effectType.getKey().getKey();
        }

        @Override
        public void apply(Player player, Arena arena) {
            int rolledDurationSeconds = durationSeconds.roll();
            player.addPotionEffect(new PotionEffect(effectType, rolledDurationSeconds * 20, amplifier, true, true, true));
            arena.sendLocalized(
                player,
                "drop.effect",
                "effect", prettify(effectType.getKey().getKey()),
                "seconds", rolledDurationSeconds,
                "level", amplifier + 1
            );
        }
    }

    private record MobDrop(double weight, EntityType entityType, AmountRange amount) implements DropAction {

        @Override
        public String key() {
            return "mob:" + entityType.name();
        }

        @Override
        public void apply(Player player, Arena arena) {
            int count = amount.roll();
            Material spawnEggMaterial = Material.matchMaterial(entityType.name() + "_SPAWN_EGG");
            boolean giveSpawnEgg = spawnEggMaterial != null && ThreadLocalRandom.current().nextBoolean();

            if (giveSpawnEgg) {
                giveOrDrop(player, arena, new ItemStack(spawnEggMaterial), count);
                arena.sendLocalized(player, "drop.spawn_egg", "mob", prettify(entityType.name()), "amount", count);
                return;
            }

            for (int i = 0; i < count; i++) {
                Location spawnLocation = arena.getMobSpawnLocation(player.getLocation());
                spawnLocation.getWorld().spawnEntity(spawnLocation, entityType);
            }
            arena.sendLocalized(player, "drop.mob_spawned", "mob", prettify(entityType.name()), "amount", count);
        }
    }

    private record AmountRange(int min, int max) {

        private static AmountRange parse(Object raw) {
            if (raw instanceof Number number) {
                int value = Math.max(1, number.intValue());
                return new AmountRange(value, value);
            }
            if (raw instanceof String string) {
                String trimmed = string.trim();
                if (trimmed.contains("-")) {
                    String[] parts = trimmed.split("-", 2);
                    try {
                        int left = Math.max(1, Integer.parseInt(parts[0].trim()));
                        int right = Math.max(1, Integer.parseInt(parts[1].trim()));
                        return new AmountRange(Math.min(left, right), Math.max(left, right));
                    } catch (NumberFormatException ignored) {
                        return new AmountRange(1, 1);
                    }
                }
                try {
                    int value = Math.max(1, Integer.parseInt(trimmed));
                    return new AmountRange(value, value);
                } catch (NumberFormatException ignored) {
                    return new AmountRange(1, 1);
                }
            }
            return new AmountRange(1, 1);
        }

        private int roll() {
            return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        }
    }

}
