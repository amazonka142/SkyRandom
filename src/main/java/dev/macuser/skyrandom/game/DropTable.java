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
import org.bukkit.entity.SulfurCube;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class DropTable {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final DropTable EMPTY = new DropTable(List.of());
    private static final int REROLL_ATTEMPTS = 10;
    private static final int RECENT_HISTORY_SIZE = 3;
    private static final double RANDOM_ENCHANT_CHANCE = 0.14D;
    private static final double EXTRA_RANDOM_ENCHANT_CHANCE = 0.28D;
    private static final int MAX_RANDOM_ENCHANTS = 3;
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
    private final List<DropGroup> rareGroups;
    private final double totalWeight;
    private final double rareTotalWeight;
    private final Map<java.util.UUID, Deque<String>> recentDropsByPlayer = new HashMap<>();

    private DropTable(List<DropGroup> groups) {
        this.groups = List.copyOf(groups);
        this.rareGroups = groups.stream()
            .filter(group -> isRareGroup(group.key()))
            .toList();
        this.totalWeight = groups.stream().mapToDouble(DropGroup::weight).sum();
        this.rareTotalWeight = rareGroups.stream().mapToDouble(DropGroup::weight).sum();
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
        rollFromGroups(player, arena, groups, totalWeight);
    }

    public boolean hasRareDrops() {
        return !rareGroups.isEmpty();
    }

    public void rollRare(Player player, Arena arena) {
        if (rareGroups.isEmpty()) {
            roll(player, arena);
            return;
        }
        rollFromGroups(player, arena, rareGroups, rareTotalWeight);
    }

    private void rollFromGroups(Player player, Arena arena, List<DropGroup> sourceGroups, double sourceWeight) {
        if (sourceGroups.isEmpty() || sourceWeight <= 0.0D) {
            return;
        }

        Deque<String> recent = recentDropsByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        DropAction fallback = null;

        for (int attempt = 0; attempt < REROLL_ATTEMPTS; attempt++) {
            DropGroup group = pickWeighted(sourceGroups, sourceWeight);
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

    private static boolean isRareGroup(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("rare") || "gear".equals(normalized) || normalized.contains("lucky");
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
            case "sulfur_tnt_cube" -> parseSulfurTntCube(section, weight);
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

    private static DropAction parseSulfurTntCube(ConfigurationSection section, double weight) {
        AmountRange amount = AmountRange.parse(section.get("amount"));
        int fuseSeconds = Math.max(1, section.getInt("fuse-seconds", 6));
        return new SulfurTntCubeDrop(weight, amount, fuseSeconds * 20);
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
                Map<Enchantment, Integer> appliedEnchantments = new LinkedHashMap<>(enchantments);
                addRandomEnchantments(material, appliedEnchantments);
                for (Map.Entry<Enchantment, Integer> enchantmentEntry : appliedEnchantments.entrySet()) {
                    meta.addEnchant(enchantmentEntry.getKey(), enchantmentEntry.getValue(), true);
                }
                itemStack.setItemMeta(meta);
            }

            giveOrDrop(player, arena, itemStack, rolledAmount);
            arena.sendLocalized(player, "drop.item", "item", prettify(material.name()), "amount", rolledAmount);
        }
    }

    private static void addRandomEnchantments(Material material, Map<Enchantment, Integer> enchantments) {
        if (ThreadLocalRandom.current().nextDouble() >= RANDOM_ENCHANT_CHANCE) {
            return;
        }

        List<RandomEnchantOption> options = new ArrayList<>(randomEnchantOptions(material));
        if (options.isEmpty()) {
            return;
        }

        java.util.Collections.shuffle(options);
        int added = 0;
        for (RandomEnchantOption option : options) {
            if (added > 0 && ThreadLocalRandom.current().nextDouble() >= EXTRA_RANDOM_ENCHANT_CHANCE) {
                break;
            }
            if (added >= MAX_RANDOM_ENCHANTS) {
                break;
            }

            Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(option.key()));
            if (enchantment == null || enchantments.containsKey(enchantment)) {
                continue;
            }

            enchantments.put(enchantment, option.rollLevel());
            added++;
        }
    }

    private static List<RandomEnchantOption> randomEnchantOptions(Material material) {
        if (material == null) {
            return List.of();
        }

        String name = material.name();
        if (name.endsWith("_SWORD")) {
            return List.of(
                new RandomEnchantOption("sharpness", 1, 4),
                new RandomEnchantOption("knockback", 1, 2),
                new RandomEnchantOption("fire_aspect", 1, 2),
                new RandomEnchantOption("sweeping_edge", 1, 3),
                new RandomEnchantOption("unbreaking", 1, 3)
            );
        }
        if (name.endsWith("_PICKAXE")) {
            return List.of(
                new RandomEnchantOption("efficiency", 1, 4),
                new RandomEnchantOption("fortune", 1, 3),
                new RandomEnchantOption("unbreaking", 1, 3),
                new RandomEnchantOption("silk_touch", 1, 1)
            );
        }
        if (name.endsWith("_AXE")) {
            return List.of(
                new RandomEnchantOption("sharpness", 1, 4),
                new RandomEnchantOption("efficiency", 1, 4),
                new RandomEnchantOption("unbreaking", 1, 3),
                new RandomEnchantOption("knockback", 1, 2)
            );
        }
        if (name.endsWith("_SHOVEL")) {
            return List.of(
                new RandomEnchantOption("efficiency", 1, 4),
                new RandomEnchantOption("fortune", 1, 3),
                new RandomEnchantOption("unbreaking", 1, 3),
                new RandomEnchantOption("silk_touch", 1, 1)
            );
        }
        if (material == Material.MACE) {
            return List.of(
                new RandomEnchantOption("density", 1, 4),
                new RandomEnchantOption("breach", 1, 4),
                new RandomEnchantOption("wind_burst", 1, 2),
                new RandomEnchantOption("unbreaking", 1, 3)
            );
        }

        return List.of();
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

    private record SulfurTntCubeDrop(double weight, AmountRange amount, int fuseTicks) implements DropAction {

        @Override
        public String key() {
            return "sulfur_tnt_cube";
        }

        @Override
        public void apply(Player player, Arena arena) {
            int count = amount.roll();
            for (int i = 0; i < count; i++) {
                Location spawnLocation = arena.getMobSpawnLocation(player.getLocation());
                if (!(spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.SULFUR_CUBE) instanceof SulfurCube cube)) {
                    continue;
                }

                cube.setSize(2);
                cube.setAdult();
                cube.setAgeLock(true);
                cube.setWander(false);
                cube.getEquipment().setItem(EquipmentSlot.BODY, new ItemStack(Material.TNT), false);
                cube.getEquipment().setDropChance(EquipmentSlot.BODY, 0.0F);
                primeSulfurTntCube(cube, fuseTicks);
            }
            arena.sendLocalized(player, "drop.sulfur_tnt_cube_spawned", "amount", count);
        }
    }

    private static void primeSulfurTntCube(SulfurCube cube, int fuseTicks) {
        if (tryPrimeSulfurTntCube(cube, fuseTicks)) {
            return;
        }

        JavaPlugin plugin = JavaPlugin.getProvidingPlugin(DropTable.class);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (cube.isValid() && !cube.isDead()) {
                cube.getEquipment().setItem(EquipmentSlot.BODY, new ItemStack(Material.TNT), false);
                cube.getEquipment().setDropChance(EquipmentSlot.BODY, 0.0F);
                tryPrimeSulfurTntCube(cube, fuseTicks);
            }
        }, 1L);
    }

    private static boolean tryPrimeSulfurTntCube(SulfurCube cube, int fuseTicks) {
        if (!cube.isValid() || cube.isDead()) {
            return true;
        }
        if (!cube.canExplode()) {
            return false;
        }

        boolean ignited = cube.ignite(false);
        if (ignited && fuseTicks > 0) {
            cube.setFuseTicks(fuseTicks);
        }
        return ignited || cube.getFuseTicks() > 0;
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

    private record RandomEnchantOption(String key, int minLevel, int maxLevel) {

        private int rollLevel() {
            int min = Math.max(1, minLevel);
            int max = Math.max(min, maxLevel);
            int totalWeight = 0;
            for (int level = min; level <= max; level++) {
                totalWeight += max - level + 1;
            }

            int roll = ThreadLocalRandom.current().nextInt(totalWeight);
            for (int level = min; level <= max; level++) {
                roll -= max - level + 1;
                if (roll < 0) {
                    return level;
                }
            }
            return min;
        }
    }

}
