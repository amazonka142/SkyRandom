package dev.macuser.skyrandom.world;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.Orientable;

public final class DefaultMapBuilder {

    private static final int DEMO_REGION_MIN_X = -40;
    private static final int DEMO_REGION_MAX_X = 810;
    private static final int DEMO_REGION_MAX_Y = 130;
    private static final int DEMO_REGION_MIN_Z = -60;
    private static final int DEMO_REGION_MAX_Z = 60;
    private static final int THRONE_CENTER_X = 90;
    private static final int THRONE_CENTER_Z = 0;
    private static final int THRONE_PLATFORM_Y = 70;
    private static final int WRECKAGE_CENTER_X = 190;
    private static final int WRECKAGE_CENTER_Z = 0;
    private static final int WRECKAGE_PLATFORM_Y = 70;
    private static final int RING_CENTER_X = 300;
    private static final int RING_CENTER_Z = 0;
    private static final int RING_PLATFORM_Y = 70;
    private static final int FROZEN_CENTER_X = 430;
    private static final int FROZEN_CENTER_Z = 0;
    private static final int FROZEN_PLATFORM_Y = 70;
    private static final int FROZEN_SPAWN_PAD_Y = 72;
    private static final double FROZEN_RADIUS_X = 46.0D;
    private static final double FROZEN_RADIUS_Z = 31.0D;
    private static final int CRYSTAL_CENTER_X = 570;
    private static final int CRYSTAL_CENTER_Z = 0;
    private static final int CRYSTAL_MIN_X = CRYSTAL_CENTER_X - 52;
    private static final int CRYSTAL_MAX_X = CRYSTAL_CENTER_X + 52;
    private static final int CRYSTAL_MIN_Y = 63;
    private static final int CRYSTAL_MAX_Y = 80;
    private static final int CRYSTAL_MIN_Z = CRYSTAL_CENTER_Z - 36;
    private static final int CRYSTAL_MAX_Z = CRYSTAL_CENTER_Z + 36;
    private static final int TRIAL_CENTER_X = 730;
    private static final int TRIAL_CENTER_Z = 0;
    private static final int TRIAL_MIN_X = TRIAL_CENTER_X - 32;
    private static final int TRIAL_MAX_X = TRIAL_CENTER_X + 32;
    private static final int TRIAL_MIN_Y = 62;
    private static final int TRIAL_MAX_Y = 84;
    private static final int TRIAL_MIN_Z = TRIAL_CENTER_Z - 22;
    private static final int TRIAL_MAX_Z = TRIAL_CENTER_Z + 22;
    private static final int TRIAL_FLOOR_Y = 69;
    private static final int TRIAL_LOWER_FLOOR_Y = 65;
    private static final int TRIAL_UPPER_FLOOR_Y = 75;

    private static final Material[] WRECKAGE_SURFACE = {
        Material.STONE_BRICKS,
        Material.CRACKED_STONE_BRICKS,
        Material.MOSSY_STONE_BRICKS,
        Material.ANDESITE,
        Material.TUFF_BRICKS,
        Material.POLISHED_ANDESITE
    };
    private static final Material[] WRECKAGE_FILL = {
        Material.COBBLESTONE,
        Material.MOSSY_COBBLESTONE,
        Material.ANDESITE,
        Material.TUFF,
        Material.STONE
    };
    private static final Material[] WRECKAGE_CORE = {
        Material.DEEPSLATE_TILES,
        Material.POLISHED_DEEPSLATE,
        Material.COBBLED_DEEPSLATE,
        Material.TUFF_BRICKS
    };
    private static final Material[] RING_SURFACE = {
        Material.CUT_COPPER,
        Material.EXPOSED_CUT_COPPER,
        Material.WEATHERED_CUT_COPPER,
        Material.DARK_PRISMARINE,
        Material.PRISMARINE_BRICKS
    };
    private static final Material[] RING_FILL = {
        Material.TUFF_BRICKS,
        Material.POLISHED_ANDESITE,
        Material.POLISHED_DEEPSLATE,
        Material.DEEPSLATE_TILES
    };
    private static final Material[] FROZEN_CORE = {
        Material.PACKED_ICE,
        Material.PACKED_ICE,
        Material.ICE,
        Material.BLUE_ICE
    };
    private static final Material[] FROZEN_ROCK = {
        Material.STONE,
        Material.ANDESITE,
        Material.DEEPSLATE,
        Material.TUFF
    };
    private static final Material[] CRYSTAL_STONE = {
        Material.DEEPSLATE,
        Material.COBBLED_DEEPSLATE,
        Material.POLISHED_DEEPSLATE,
        Material.DEEPSLATE_BRICKS,
        Material.DEEPSLATE_TILES,
        Material.CHISELED_DEEPSLATE
    };
    private static final Material[] CRYSTAL_AMETHYST = {
        Material.AMETHYST_BLOCK,
        Material.AMETHYST_BLOCK,
        Material.BUDDING_AMETHYST
    };
    private static final Material[] TRIAL_TUFF = {
        Material.TUFF_BRICKS,
        Material.TUFF_BRICKS,
        Material.CHISELED_TUFF_BRICKS,
        Material.POLISHED_TUFF
    };
    private static final Material[] TRIAL_FLOOR = {
        Material.OXIDIZED_CUT_COPPER,
        Material.OXIDIZED_CUT_COPPER,
        Material.OXIDIZED_COPPER,
        Material.WEATHERED_CUT_COPPER
    };
    private static final Material[] TRIAL_COPPER = {
        Material.CUT_COPPER,
        Material.EXPOSED_CUT_COPPER,
        Material.WEATHERED_CUT_COPPER
    };

    private static final String[] WRECKAGE_SPAWN_TOP = {
        ".........",
        "..###....",
        ".######..",
        "########.",
        "########.",
        ".#######.",
        "..#####..",
        "...###...",
        "........."
    };
    private static final String[] WRECKAGE_SPAWN_FILL = {
        ".........",
        "...##....",
        "..####...",
        ".######..",
        ".#####...",
        "..####...",
        "...##....",
        ".........",
        "........."
    };
    private static final String[] WRECKAGE_SPAWN_CORE = {
        ".........",
        ".........",
        "...##....",
        "..####...",
        "..###....",
        "...##....",
        ".........",
        ".........",
        "........."
    };
    private static final String[] WRECKAGE_CORE_TOP = {
        ".............",
        "....####.....",
        "...######....",
        "..########...",
        ".##########..",
        ".####..####..",
        "#####..#####.",
        ".##########..",
        "..########...",
        "...######....",
        "....####.....",
        ".....##......",
        "............."
    };
    private static final String[] WRECKAGE_CORE_FILL = {
        "...........",
        "...####....",
        "..######...",
        ".########..",
        ".###..###..",
        "####..####.",
        ".########..",
        "..######...",
        "...####....",
        "....##.....",
        "..........."
    };
    private static final String[] WRECKAGE_CORE_BASE = {
        ".........",
        "...###...",
        "..#####..",
        ".###.###.",
        ".##...##.",
        ".###.###.",
        "..#####..",
        "...###...",
        "........."
    };
    private static final String[] WRECKAGE_FRAGMENT_TOP = {
        "..###..",
        ".#####.",
        "#######",
        ".#####.",
        "...##.."
    };
    private static final String[] WRECKAGE_FRAGMENT_FILL = {
        "...#...",
        "..###..",
        ".#####.",
        "..###..",
        "...#..."
    };
    private static final String[] WRECKAGE_FRAGMENT_CORE = {
        ".......",
        "...#...",
        "..###..",
        "...#...",
        "......."
    };
    private static final String[] WRECKAGE_SHARD_TOP = {
        ".#.",
        "###",
        ".#."
    };

    private DefaultMapBuilder() {
    }

    public static void ensureBuilt(World world) {
        clearDemoRegion(world);
        buildLobby(world);
        buildPillarsArena(world);
        buildCentralThroneArena(world);
        buildWreckageArena(world);
        buildRingArena(world);
        buildFrozenPeaksArena(world);
        buildCrystalCavernArena(world);
        buildTrialChamberArena(world);
        world.setSpawnLocation(new Location(world, 0.5D, 97.0D, 0.5D, 180.0F, 0.0F));
    }

    private static void clearDemoRegion(World world) {
        fill(
            world,
            DEMO_REGION_MIN_X, world.getMinHeight(), DEMO_REGION_MIN_Z,
            DEMO_REGION_MAX_X, DEMO_REGION_MAX_Y, DEMO_REGION_MAX_Z,
            Material.AIR
        );
    }

    private static void buildLobby(World world) {
        buildRingPlatform(world, 0, 96, 0, 9, 4, Material.QUARTZ_BLOCK, Material.LIGHT_BLUE_STAINED_GLASS);
        buildRoundPlatform(world, 0, 95, 0, 7, Material.GLASS);
        buildRoundPlatform(world, 0, 94, 0, 3, Material.DEEPSLATE_TILES);
        fill(world, -1, 84, -1, 1, 95, 1, Material.LIGHT_BLUE_STAINED_GLASS);

        fill(world, -2, 96, -2, 2, 96, 2, Material.SMOOTH_QUARTZ);
        set(world, 0, 96, 0, Material.SEA_LANTERN);
        set(world, -2, 96, 0, Material.SEA_LANTERN);
        set(world, 2, 96, 0, Material.SEA_LANTERN);
        set(world, 0, 96, -2, Material.SEA_LANTERN);
        set(world, 0, 96, 2, Material.SEA_LANTERN);

        fill(world, -1, 96, -10, 1, 96, -7, Material.GLASS);
        fill(world, -1, 96, 7, 1, 96, 10, Material.GLASS);
        fill(world, -10, 96, -1, -7, 96, 1, Material.GLASS);
        fill(world, 7, 96, -1, 10, 96, 1, Material.GLASS);
        set(world, 0, 96, -10, Material.SEA_LANTERN);
        set(world, 0, 96, 10, Material.SEA_LANTERN);
        set(world, -10, 96, 0, Material.SEA_LANTERN);
        set(world, 10, 96, 0, Material.SEA_LANTERN);

        buildLobbyPillar(world, -7, -7);
        buildLobbyPillar(world, 7, -7);
        buildLobbyPillar(world, -7, 7);
        buildLobbyPillar(world, 7, 7);

        buildLobbyRailing(world);
        buildLobbySafetyBarriers(world);

        fill(world, -1, 96, -6, 1, 96, -5, Material.SMOOTH_QUARTZ);
    }

    private static void buildPillarsArena(World world) {
        buildPillar(world, -12, 59, -12, 70);
        buildPillar(world, 12, 59, -12, 70);
        buildPillar(world, 12, 59, 12, 70);
        buildPillar(world, -12, 59, 12, 70);

        buildStartingPad(world, -12, 70, -12);
        buildStartingPad(world, 12, 70, -12);
        buildStartingPad(world, 12, 70, 12);
        buildStartingPad(world, -12, 70, 12);
    }

    private static void buildCentralThroneArena(World world) {
        buildRoundPlatform(world, THRONE_CENTER_X, THRONE_PLATFORM_Y, THRONE_CENTER_Z, 8, Material.POLISHED_ANDESITE);
        buildRoundPlatform(world, THRONE_CENTER_X, THRONE_PLATFORM_Y - 1, THRONE_CENTER_Z, 6, Material.SMOOTH_STONE);
        buildRoundPlatform(world, THRONE_CENTER_X, THRONE_PLATFORM_Y - 2, THRONE_CENTER_Z, 4, Material.DEEPSLATE_TILES);
        fill(
            world,
            THRONE_CENTER_X - 2, 60, THRONE_CENTER_Z - 2,
            THRONE_CENTER_X + 2, THRONE_PLATFORM_Y - 1, THRONE_CENTER_Z + 2,
            Material.DEEPSLATE_TILES
        );

        buildMiniIsland(world, THRONE_CENTER_X, THRONE_PLATFORM_Y, -18);
        buildMiniIsland(world, THRONE_CENTER_X + 18, THRONE_PLATFORM_Y, THRONE_CENTER_Z);
        buildMiniIsland(world, THRONE_CENTER_X, THRONE_PLATFORM_Y, 18);
        buildMiniIsland(world, THRONE_CENTER_X - 18, THRONE_PLATFORM_Y, THRONE_CENTER_Z);
    }

    private static void buildWreckageArena(World world) {
        buildWreckageSpawnIsland(world, 167, WRECKAGE_PLATFORM_Y, -18, 0);
        buildWreckageSpawnIsland(world, 213, WRECKAGE_PLATFORM_Y, -18, 1);
        buildWreckageSpawnIsland(world, 213, WRECKAGE_PLATFORM_Y, 18, 2);
        buildWreckageSpawnIsland(world, 167, WRECKAGE_PLATFORM_Y, 18, 3);

        buildWreckageCore(world, WRECKAGE_CENTER_X, WRECKAGE_PLATFORM_Y, WRECKAGE_CENTER_Z);

        buildWreckageFragment(world, 178, 69, -12, 0);
        buildWreckageFragment(world, 184, 71, -6, 1);
        buildWreckageFragment(world, 202, 70, -10, 2);
        buildWreckageFragment(world, 206, 72, -2, 3);
        buildWreckageFragment(world, 178, 70, 12, 1);
        buildWreckageFragment(world, 184, 68, 5, 0);
        buildWreckageFragment(world, 201, 69, 10, 3);
        buildWreckageFragment(world, 205, 71, 3, 2);
        buildWreckageFragment(world, 190, 68, -15, 0);
        buildWreckageFragment(world, 190, 68, 15, 2);
        buildWreckageFragment(world, 174, 72, 0, 1);
        buildWreckageFragment(world, 206, 72, 0, 3);

        buildShard(world, 181, 73, -1);
        buildShard(world, 198, 73, 4);
        buildShard(world, 192, 74, -6);
        buildShard(world, 187, 67, 10);
        buildShard(world, 196, 67, -14);
    }

    private static void buildRingArena(World world) {
        buildRingMajorIsland(world, RING_CENTER_X, RING_PLATFORM_Y, -20, true);
        buildRingMajorIsland(world, RING_CENTER_X + 20, RING_PLATFORM_Y, RING_CENTER_Z, false);
        buildRingMajorIsland(world, RING_CENTER_X, RING_PLATFORM_Y, 20, true);
        buildRingMajorIsland(world, RING_CENTER_X - 20, RING_PLATFORM_Y, RING_CENTER_Z, false);

        buildRingConnectorNode(world, RING_CENTER_X + 14, RING_PLATFORM_Y, -14);
        buildRingConnectorNode(world, RING_CENTER_X + 14, RING_PLATFORM_Y, 14);
        buildRingConnectorNode(world, RING_CENTER_X - 14, RING_PLATFORM_Y, 14);
        buildRingConnectorNode(world, RING_CENTER_X - 14, RING_PLATFORM_Y, -14);

        buildRingInwardSpur(world, RING_CENTER_X, RING_PLATFORM_Y, -13);
        buildRingInwardSpur(world, RING_CENTER_X + 13, RING_PLATFORM_Y, RING_CENTER_Z);
        buildRingInwardSpur(world, RING_CENTER_X, RING_PLATFORM_Y, 13);
        buildRingInwardSpur(world, RING_CENTER_X - 13, RING_PLATFORM_Y, RING_CENTER_Z);
        buildRingInwardSpur(world, RING_CENTER_X + 10, RING_PLATFORM_Y, -10);
        buildRingInwardSpur(world, RING_CENTER_X + 10, RING_PLATFORM_Y, 10);
        buildRingInwardSpur(world, RING_CENTER_X - 10, RING_PLATFORM_Y, 10);
        buildRingInwardSpur(world, RING_CENTER_X - 10, RING_PLATFORM_Y, -10);

        buildRingBridge(world, RING_CENTER_X, RING_PLATFORM_Y, -20, RING_CENTER_X + 14, -14);
        buildRingBridge(world, RING_CENTER_X + 14, RING_PLATFORM_Y, -14, RING_CENTER_X + 20, 0);
        buildRingBridge(world, RING_CENTER_X + 20, RING_PLATFORM_Y, 0, RING_CENTER_X + 14, 14);
        buildRingBridge(world, RING_CENTER_X + 14, RING_PLATFORM_Y, 14, RING_CENTER_X, 20);
        buildRingBridge(world, RING_CENTER_X, RING_PLATFORM_Y, 20, RING_CENTER_X - 14, 14);
        buildRingBridge(world, RING_CENTER_X - 14, RING_PLATFORM_Y, 14, RING_CENTER_X - 20, 0);
        buildRingBridge(world, RING_CENTER_X - 20, RING_PLATFORM_Y, 0, RING_CENTER_X - 14, -14);
        buildRingBridge(world, RING_CENTER_X - 14, RING_PLATFORM_Y, -14, RING_CENTER_X, -20);

        buildRingDebris(world, RING_CENTER_X, 68, -31);
        buildRingDebris(world, RING_CENTER_X + 31, 68, RING_CENTER_Z);
        buildRingDebris(world, RING_CENTER_X, 68, 31);
        buildRingDebris(world, RING_CENTER_X - 31, 68, RING_CENTER_Z);
        buildRingDebris(world, RING_CENTER_X + 24, 66, -24);
        buildRingDebris(world, RING_CENTER_X + 24, 66, 24);
        buildRingDebris(world, RING_CENTER_X - 24, 66, 24);
        buildRingDebris(world, RING_CENTER_X - 24, 66, -24);
    }

    private static void buildFrozenPeaksArena(World world) {
        buildFrozenIslandTerrain(world);

        buildFrozenIcePatch(world, FROZEN_CENTER_X - 31, FROZEN_CENTER_Z - 16, 5, 3);
        buildFrozenIcePatch(world, FROZEN_CENTER_X + 30, FROZEN_CENTER_Z + 13, 4, 3);
        buildFrozenIcePatch(world, FROZEN_CENTER_X + 20, FROZEN_CENTER_Z - 23, 3, 2);
        buildFrozenIcePatch(world, FROZEN_CENTER_X - 8, FROZEN_CENTER_Z + 23, 4, 2);

        buildFrozenSpawnPad(world, FROZEN_CENTER_X - 28, FROZEN_CENTER_Z - 23);
        buildFrozenSpawnPad(world, FROZEN_CENTER_X + 28, FROZEN_CENTER_Z - 23);
        buildFrozenSpawnPad(world, FROZEN_CENTER_X + 28, FROZEN_CENTER_Z + 23);
        buildFrozenSpawnPad(world, FROZEN_CENTER_X - 28, FROZEN_CENTER_Z + 23);

        buildFrozenDeadTree(world, FROZEN_CENTER_X - 39, FROZEN_CENTER_Z - 18, 6, 0);
        buildFrozenDeadTree(world, FROZEN_CENTER_X - 31, FROZEN_CENTER_Z + 8, 7, 1);
        buildFrozenDeadTree(world, FROZEN_CENTER_X - 18, FROZEN_CENTER_Z - 5, 8, 2);
        buildFrozenDeadTree(world, FROZEN_CENTER_X - 15, FROZEN_CENTER_Z + 18, 5, 3);
        buildFrozenDeadTree(world, FROZEN_CENTER_X - 3, FROZEN_CENTER_Z + 26, 6, 4);
        buildFrozenDeadTree(world, FROZEN_CENTER_X + 12, FROZEN_CENTER_Z + 18, 8, 5);
        buildFrozenDeadTree(world, FROZEN_CENTER_X + 24, FROZEN_CENTER_Z - 20, 6, 6);
        buildFrozenDeadTree(world, FROZEN_CENTER_X + 34, FROZEN_CENTER_Z + 2, 7, 7);
        buildFrozenDeadTree(world, FROZEN_CENTER_X + 41, FROZEN_CENTER_Z + 20, 5, 8);
    }

    private static void buildFrozenIslandTerrain(World world) {
        for (int x = FROZEN_CENTER_X - 50; x <= FROZEN_CENTER_X + 50; x++) {
            for (int z = FROZEN_CENTER_Z - 36; z <= FROZEN_CENTER_Z + 36; z++) {
                int topY = frozenSurfaceY(x, z);
                if (topY == Integer.MIN_VALUE) {
                    continue;
                }

                double normalized = frozenNormalizedDistance(x, z);
                int bottomY = (int) Math.round(48.0D + (Math.min(1.15D, normalized) * 12.0D));
                bottomY = Math.min(bottomY, topY - 4);

                for (int y = bottomY; y <= topY; y++) {
                    set(world, x, y, z, pickFrozenColumnMaterial(x, y, z, topY, bottomY));
                }

                if (shouldPlaceSnowLayer(x, z, topY)) {
                    set(world, x, topY + 1, z, Material.SNOW);
                }
                if (isFrozenEdgeColumn(normalized, x, z)) {
                    buildFrozenIcicle(world, x, bottomY - 1, z);
                }
            }
        }
    }

    private static Material pickFrozenColumnMaterial(int x, int y, int z, int topY, int bottomY) {
        if (y == topY) {
            if (isFrozenRockStreak(x, z, topY)) {
                return pickMaterial(FROZEN_ROCK, x, y, z);
            }
            if (topY <= FROZEN_PLATFORM_Y + 1 && Math.floorMod((x * 17) + (z * 11), 23) == 0) {
                return Material.PACKED_ICE;
            }
            return Material.SNOW_BLOCK;
        }

        if (y >= topY - 3) {
            return topY > FROZEN_PLATFORM_Y + 13 && Math.floorMod((x * 13) + (z * 7) + y, 6) == 0
                ? pickMaterial(FROZEN_ROCK, x, y, z)
                : Material.SNOW_BLOCK;
        }
        if (y <= bottomY + 2) {
            return pickMaterial(FROZEN_CORE, x, y, z);
        }
        if (topY > FROZEN_PLATFORM_Y + 18 && y >= topY - 12) {
            return pickMaterial(FROZEN_ROCK, x, y, z);
        }
        return pickMaterial(FROZEN_CORE, x, y, z);
    }

    private static int frozenSurfaceY(int x, int z) {
        double normalized = frozenNormalizedDistance(x, z);
        double edgeNoise = 0.08D * Math.sin((x + z) * 0.27D)
            + 0.06D * Math.cos((x - (z * 2)) * 0.19D)
            + 0.04D * Math.sin(z * 0.51D);
        if (normalized > 1.0D + edgeNoise) {
            return Integer.MIN_VALUE;
        }

        double edgeLift = Math.max(0.0D, 1.0D - normalized) * 3.2D;
        double roll = Math.sin((x - FROZEN_CENTER_X) * 0.21D) * 1.2D
            + Math.cos((z - FROZEN_CENTER_Z) * 0.24D) * 1.0D;
        double mountain = frozenPeak(x, z, FROZEN_CENTER_X + 7, FROZEN_CENTER_Z - 2, 11.0D, 8.5D, 45.0D)
            + frozenPeak(x, z, FROZEN_CENTER_X - 9, FROZEN_CENTER_Z - 7, 9.0D, 10.5D, 31.0D)
            + frozenPeak(x, z, FROZEN_CENTER_X + 1, FROZEN_CENTER_Z - 4, 21.0D, 6.0D, 11.0D);
        int y = (int) Math.round(FROZEN_PLATFORM_Y + edgeLift + roll + mountain);
        return Math.max(64, Math.min(122, y));
    }

    private static double frozenNormalizedDistance(int x, int z) {
        double dx = (x - FROZEN_CENTER_X) / FROZEN_RADIUS_X;
        double dz = (z - FROZEN_CENTER_Z) / FROZEN_RADIUS_Z;
        return (dx * dx) + (dz * dz);
    }

    private static double frozenPeak(
        int x,
        int z,
        int peakX,
        int peakZ,
        double radiusX,
        double radiusZ,
        double height
    ) {
        double dx = (x - peakX) / radiusX;
        double dz = (z - peakZ) / radiusZ;
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        if (distance >= 1.0D) {
            return 0.0D;
        }
        double slope = 1.0D - distance;
        return Math.pow(slope, 2.0D) * height;
    }

    private static boolean isFrozenRockStreak(int x, int z, int topY) {
        if (topY < FROZEN_PLATFORM_Y + 13) {
            return false;
        }
        int pattern = Math.floorMod((x * 31) + (z * 17), 19);
        return pattern <= 2 || (topY > FROZEN_PLATFORM_Y + 28 && pattern <= 5);
    }

    private static boolean shouldPlaceSnowLayer(int x, int z, int topY) {
        if (topY > FROZEN_PLATFORM_Y + 18 && isFrozenRockStreak(x, z, topY)) {
            return false;
        }
        return Math.floorMod((x * 19) + (z * 23), 5) != 0;
    }

    private static boolean isFrozenEdgeColumn(double normalized, int x, int z) {
        return normalized > 0.78D && Math.floorMod((x * 7) + (z * 13), 11) == 0;
    }

    private static void buildFrozenIcicle(World world, int x, int startY, int z) {
        int length = 2 + Math.floorMod((x * 5) + (z * 3), 5);
        for (int y = startY; y > startY - length; y--) {
            set(world, x, y, z, pickMaterial(FROZEN_CORE, x, y, z));
        }
    }

    private static void buildFrozenIcePatch(World world, int centerX, int centerZ, int radiusX, int radiusZ) {
        double radiusXSquared = radiusX * radiusX;
        double radiusZSquared = radiusZ * radiusZ;
        for (int x = centerX - radiusX; x <= centerX + radiusX; x++) {
            for (int z = centerZ - radiusZ; z <= centerZ + radiusZ; z++) {
                double normalized = ((x - centerX) * (x - centerX)) / radiusXSquared
                    + ((z - centerZ) * (z - centerZ)) / radiusZSquared;
                if (normalized > 1.0D) {
                    continue;
                }

                int y = frozenSurfaceY(x, z);
                if (y == Integer.MIN_VALUE || y > FROZEN_PLATFORM_Y + 6) {
                    continue;
                }
                set(world, x, y, z, normalized < 0.35D ? Material.BLUE_ICE : Material.PACKED_ICE);
                set(world, x, y + 1, z, Material.AIR);
            }
        }
    }

    private static void buildFrozenSpawnPad(World world, int centerX, int centerZ) {
        int supportY = frozenSurfaceY(centerX, centerZ);
        if (supportY == Integer.MIN_VALUE) {
            supportY = FROZEN_PLATFORM_Y;
        }
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                int surfaceY = frozenSurfaceY(x, z);
                int columnTop = surfaceY == Integer.MIN_VALUE ? supportY : Math.max(supportY, surfaceY);
                for (int y = Math.min(columnTop, FROZEN_SPAWN_PAD_Y); y <= FROZEN_SPAWN_PAD_Y; y++) {
                    set(world, x, y, z, y == FROZEN_SPAWN_PAD_Y ? Material.SNOW_BLOCK : Material.PACKED_ICE);
                }
                fill(world, x, FROZEN_SPAWN_PAD_Y + 1, z, x, FROZEN_SPAWN_PAD_Y + 5, z, Material.AIR);
            }
        }
        set(world, centerX, FROZEN_SPAWN_PAD_Y, centerZ, Material.BEDROCK);
    }

    private static void buildFrozenDeadTree(World world, int x, int z, int height, int variant) {
        int groundY = frozenSurfaceY(x, z);
        if (groundY == Integer.MIN_VALUE) {
            return;
        }

        int baseY = groundY + 1;
        setLog(world, x, baseY - 1, z, Material.SPRUCE_LOG, Axis.Y);
        for (int y = baseY; y < baseY + height; y++) {
            setLog(world, x, y, z, Material.SPRUCE_LOG, Axis.Y);
        }
        setLog(world, x, baseY + height, z, Material.SPRUCE_LOG, Axis.Y);

        buildFrozenBranch(world, x, baseY + height - 1, z, variant % 4, 3);
        buildFrozenBranch(world, x, baseY + height - 2, z, (variant + 1) % 4, 2);
        buildFrozenBranch(world, x, baseY + Math.max(2, height - 3), z, (variant + 3) % 4, 2);
        if (height > 6) {
            buildFrozenBranch(world, x, baseY + 3, z, (variant + 2) % 4, 1);
        }
    }

    private static void buildFrozenBranch(World world, int x, int y, int z, int direction, int length) {
        int dx = direction == 1 ? 1 : direction == 3 ? -1 : 0;
        int dz = direction == 0 ? -1 : direction == 2 ? 1 : 0;
        Axis axis = dx == 0 ? Axis.Z : Axis.X;
        for (int step = 1; step <= length; step++) {
            int branchY = y + (step >= 3 ? 1 : 0);
            setLog(world, x + (dx * step), branchY, z + (dz * step), Material.SPRUCE_LOG, axis);
            if (step == length) {
                setLog(world, x + (dx * step), branchY + 1, z + (dz * step), Material.SPRUCE_LOG, Axis.Y);
            }
        }
    }

    private static void buildCrystalCavernArena(World world) {
        buildCrystalCavernShell(world);

        buildCrystalRoom(world, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 14, 10, 4);
        buildCrystalRoom(world, CRYSTAL_CENTER_X - 28, 70, CRYSTAL_CENTER_Z - 21, 7, 5, 4);
        buildCrystalRoom(world, CRYSTAL_CENTER_X + 28, 70, CRYSTAL_CENTER_Z - 21, 7, 5, 4);
        buildCrystalRoom(world, CRYSTAL_CENTER_X + 28, 70, CRYSTAL_CENTER_Z + 21, 7, 5, 4);
        buildCrystalRoom(world, CRYSTAL_CENTER_X - 28, 70, CRYSTAL_CENTER_Z + 21, 7, 5, 4);
        buildCrystalRoom(world, CRYSTAL_CENTER_X - 10, 67, CRYSTAL_CENTER_Z - 5, 8, 6, 4);
        buildCrystalRoom(world, CRYSTAL_CENTER_X + 12, 73, CRYSTAL_CENTER_Z + 10, 8, 5, 3);
        buildCrystalRoom(world, CRYSTAL_CENTER_X + 1, 70, CRYSTAL_CENTER_Z + 29, 9, 4, 3);

        buildCrystalTunnel(world, CRYSTAL_CENTER_X - 28, 70, CRYSTAL_CENTER_Z - 21, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 2, 3);
        buildCrystalTunnel(world, CRYSTAL_CENTER_X + 28, 70, CRYSTAL_CENTER_Z - 21, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 2, 3);
        buildCrystalTunnel(world, CRYSTAL_CENTER_X + 28, 70, CRYSTAL_CENTER_Z + 21, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 2, 3);
        buildCrystalTunnel(world, CRYSTAL_CENTER_X - 28, 70, CRYSTAL_CENTER_Z + 21, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 2, 3);
        buildCrystalTunnel(world, CRYSTAL_CENTER_X - 4, 70, CRYSTAL_CENTER_Z - 2, CRYSTAL_CENTER_X - 10, 67, CRYSTAL_CENTER_Z - 5, 2, 3);
        buildCrystalTunnel(world, CRYSTAL_CENTER_X + 6, 70, CRYSTAL_CENTER_Z + 2, CRYSTAL_CENTER_X + 12, 73, CRYSTAL_CENTER_Z + 10, 2, 3);
        buildCrystalTunnel(world, CRYSTAL_CENTER_X - 10, 67, CRYSTAL_CENTER_Z - 5, CRYSTAL_CENTER_X - 25, 70, CRYSTAL_CENTER_Z + 4, 2, 3);
        buildCrystalTunnel(world, CRYSTAL_CENTER_X + 12, 73, CRYSTAL_CENTER_Z + 10, CRYSTAL_CENTER_X + 26, 70, CRYSTAL_CENTER_Z + 3, 2, 3);
        buildCrystalTunnel(world, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z + 8, CRYSTAL_CENTER_X + 1, 70, CRYSTAL_CENTER_Z + 29, 2, 3);

        buildCrystalSpawnPad(world, CRYSTAL_CENTER_X - 28, 70, CRYSTAL_CENTER_Z - 21);
        buildCrystalSpawnPad(world, CRYSTAL_CENTER_X + 28, 70, CRYSTAL_CENTER_Z - 21);
        buildCrystalSpawnPad(world, CRYSTAL_CENTER_X + 28, 70, CRYSTAL_CENTER_Z + 21);
        buildCrystalSpawnPad(world, CRYSTAL_CENTER_X - 28, 70, CRYSTAL_CENTER_Z + 21);

        buildCrystalGrowth(world, CRYSTAL_CENTER_X - 4, 70, CRYSTAL_CENTER_Z + 5, 2);
        buildCrystalGrowth(world, CRYSTAL_CENTER_X + 6, 70, CRYSTAL_CENTER_Z - 4, 2);
        buildCrystalGrowth(world, CRYSTAL_CENTER_X - 12, 67, CRYSTAL_CENTER_Z - 7, 2);
        buildCrystalGrowth(world, CRYSTAL_CENTER_X - 5, 67, CRYSTAL_CENTER_Z - 1, 1);
        buildCrystalGrowth(world, CRYSTAL_CENTER_X + 13, 73, CRYSTAL_CENTER_Z + 9, 1);
        buildCrystalGrowth(world, CRYSTAL_CENTER_X + 18, 73, CRYSTAL_CENTER_Z + 12, 1);
        buildCrystalGrowth(world, CRYSTAL_CENTER_X + 2, 70, CRYSTAL_CENTER_Z + 29, 2);
        buildCrystalGrowth(world, CRYSTAL_CENTER_X - 34, 70, CRYSTAL_CENTER_Z + 19, 1);
        buildCrystalGrowth(world, CRYSTAL_CENTER_X + 34, 70, CRYSTAL_CENTER_Z - 22, 1);

        buildCrystalSafetyRoutes(world);

        buildCrystalTorch(world, CRYSTAL_CENTER_X - 31, 70, CRYSTAL_CENTER_Z - 19);
        buildCrystalTorch(world, CRYSTAL_CENTER_X + 31, 70, CRYSTAL_CENTER_Z - 19);
        buildCrystalTorch(world, CRYSTAL_CENTER_X + 31, 70, CRYSTAL_CENTER_Z + 19);
        buildCrystalTorch(world, CRYSTAL_CENTER_X - 31, 70, CRYSTAL_CENTER_Z + 19);
        buildCrystalTorch(world, CRYSTAL_CENTER_X - 12, 70, CRYSTAL_CENTER_Z - 10);
        buildCrystalTorch(world, CRYSTAL_CENTER_X + 12, 70, CRYSTAL_CENTER_Z - 10);
        buildCrystalTorch(world, CRYSTAL_CENTER_X - 13, 67, CRYSTAL_CENTER_Z - 2);
        buildCrystalTorch(world, CRYSTAL_CENTER_X + 11, 73, CRYSTAL_CENTER_Z + 13);
        buildCrystalTorch(world, CRYSTAL_CENTER_X + 1, 70, CRYSTAL_CENTER_Z + 24);
    }

    private static void buildCrystalCavernShell(World world) {
        for (int x = CRYSTAL_MIN_X; x <= CRYSTAL_MAX_X; x++) {
            for (int y = CRYSTAL_MIN_Y; y <= CRYSTAL_MAX_Y; y++) {
                for (int z = CRYSTAL_MIN_Z; z <= CRYSTAL_MAX_Z; z++) {
                    set(world, x, y, z, pickCrystalStone(x, y, z));
                }
            }
        }
    }

    private static void buildCrystalRoom(
        World world,
        int centerX,
        int playerY,
        int centerZ,
        int radiusX,
        int radiusZ,
        int height
    ) {
        double radiusXSquared = radiusX * radiusX;
        double radiusZSquared = radiusZ * radiusZ;
        for (int x = centerX - radiusX; x <= centerX + radiusX; x++) {
            for (int z = centerZ - radiusZ; z <= centerZ + radiusZ; z++) {
                double normalized = ((x - centerX) * (x - centerX)) / radiusXSquared
                    + ((z - centerZ) * (z - centerZ)) / radiusZSquared;
                double edgeNoise = 0.10D * Math.sin((x * 0.31D) + (z * 0.17D));
                if (normalized > 1.0D + edgeNoise) {
                    continue;
                }
                carveCrystalColumn(world, x, playerY, z, height);
            }
        }
    }

    private static void buildCrystalTunnel(
        World world,
        int startX,
        int startY,
        int startZ,
        int endX,
        int endY,
        int endZ,
        int radius,
        int height
    ) {
        int distance = Math.max(Math.max(Math.abs(endX - startX), Math.abs(endZ - startZ)), Math.abs(endY - startY));
        int steps = Math.max(1, distance * 2);
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            int x = (int) Math.round(startX + ((endX - startX) * progress));
            int y = (int) Math.round(startY + ((endY - startY) * progress));
            int z = (int) Math.round(startZ + ((endZ - startZ) * progress));
            buildCrystalTunnelSlice(world, x, y, z, radius, height);
        }
    }

    private static void buildCrystalTunnelSlice(World world, int centerX, int playerY, int centerZ, int radius, int height) {
        int radiusSquared = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if ((dx * dx) + (dz * dz) > radiusSquared + 1) {
                    continue;
                }
                carveCrystalColumn(world, x, playerY, z, height);
            }
        }
    }

    private static void carveCrystalColumn(World world, int x, int playerY, int z, int height) {
        set(world, x, playerY - 1, z, pickCrystalStone(x, playerY - 1, z));
        for (int y = playerY; y < playerY + height; y++) {
            set(world, x, y, z, Material.AIR);
        }
        set(world, x, playerY + height, z, pickCrystalStone(x, playerY + height, z));
    }

    private static void buildCrystalSpawnPad(World world, int centerX, int playerY, int centerZ) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                set(world, x, playerY - 1, z, Material.POLISHED_DEEPSLATE);
                fill(world, x, playerY, z, x, playerY + 3, z, Material.AIR);
            }
        }
        set(world, centerX, playerY - 1, centerZ, Material.REINFORCED_DEEPSLATE);
    }

    private static void buildCrystalGrowth(World world, int x, int playerY, int z, int height) {
        for (int y = playerY; y < playerY + height; y++) {
            set(world, x, y, z, Material.AMETHYST_BLOCK);
        }
        setFacing(world, x, playerY + height, z, Material.AMETHYST_CLUSTER, BlockFace.UP);
        setFacing(world, x + 1, playerY + 1, z, Material.SMALL_AMETHYST_BUD, BlockFace.EAST);
        setFacing(world, x - 1, playerY + 1, z, Material.MEDIUM_AMETHYST_BUD, BlockFace.WEST);
        if (height > 1) {
            setFacing(world, x, playerY + 1, z + 1, Material.LARGE_AMETHYST_BUD, BlockFace.SOUTH);
        }
    }

    private static void buildCrystalTorch(World world, int x, int playerY, int z) {
        set(world, x, playerY, z, Material.TORCH);
    }

    private static void buildCrystalSafetyRoutes(World world) {
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X - 28, 70, CRYSTAL_CENTER_Z - 21, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 4, 4);
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X + 28, 70, CRYSTAL_CENTER_Z - 21, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 4, 4);
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X + 28, 70, CRYSTAL_CENTER_Z + 21, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 4, 4);
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X - 28, 70, CRYSTAL_CENTER_Z + 21, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z, 4, 4);
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X, 70, CRYSTAL_CENTER_Z + 8, CRYSTAL_CENTER_X + 1, 70, CRYSTAL_CENTER_Z + 29, 3, 4);
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X - 2, 70, CRYSTAL_CENTER_Z - 1, CRYSTAL_CENTER_X - 10, 70, CRYSTAL_CENTER_Z - 5, 3, 4);
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X + 3, 70, CRYSTAL_CENTER_Z + 2, CRYSTAL_CENTER_X + 12, 70, CRYSTAL_CENTER_Z + 10, 3, 4);
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X - 10, 70, CRYSTAL_CENTER_Z - 5, CRYSTAL_CENTER_X - 25, 70, CRYSTAL_CENTER_Z + 4, 3, 4);
        buildCrystalSafeTunnel(world, CRYSTAL_CENTER_X + 12, 70, CRYSTAL_CENTER_Z + 10, CRYSTAL_CENTER_X + 26, 70, CRYSTAL_CENTER_Z + 3, 3, 4);
    }

    private static void buildCrystalSafeTunnel(
        World world,
        int startX,
        int startY,
        int startZ,
        int endX,
        int endY,
        int endZ,
        int radius,
        int height
    ) {
        int distance = Math.max(Math.max(Math.abs(endX - startX), Math.abs(endZ - startZ)), Math.abs(endY - startY));
        int steps = Math.max(1, distance * 3);
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            int x = (int) Math.round(startX + ((endX - startX) * progress));
            int y = (int) Math.round(startY + ((endY - startY) * progress));
            int z = (int) Math.round(startZ + ((endZ - startZ) * progress));
            buildCrystalSafeTunnelSlice(world, x, y, z, radius, height);
        }
    }

    private static void buildCrystalSafeTunnelSlice(World world, int centerX, int playerY, int centerZ, int radius, int height) {
        int radiusSquared = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if ((dx * dx) + (dz * dz) > radiusSquared + 1) {
                    continue;
                }
                carveCrystalSafeColumn(world, x, playerY, z, height);
            }
        }
    }

    private static void carveCrystalSafeColumn(World world, int x, int playerY, int z, int height) {
        set(world, x, playerY - 1, z, Material.POLISHED_DEEPSLATE);
        for (int y = playerY; y < playerY + height; y++) {
            set(world, x, y, z, Material.AIR);
        }
    }

    private static Material pickCrystalStone(int x, int y, int z) {
        long hash = (x * 120_037L) ^ (y * 7_919L) ^ (z * 65_537L);
        if (Math.floorMod(hash, 10L) == 0L) {
            return pickMaterial(CRYSTAL_AMETHYST, x, y, z);
        }
        return pickMaterial(CRYSTAL_STONE, x, y, z);
    }

    private static void buildTrialChamberArena(World world) {
        buildTrialChamberShell(world);
        buildTrialChamberFloor(world);
        buildTrialWallBands(world);
        buildTrialUpperPlatform(world);
        buildTrialLowerPit(world);
        buildTrialSideDetails(world);
        buildTrialPillars(world);
        buildTrialCentralGrates(world);
        buildTrialDecor(world);

        buildTrialSpawnPad(world, TRIAL_CENTER_X - 14, TRIAL_CENTER_Z - 16);
        buildTrialSpawnPad(world, TRIAL_CENTER_X + 14, TRIAL_CENTER_Z - 16);
        buildTrialSpawnPad(world, TRIAL_CENTER_X + 28, TRIAL_CENTER_Z + 2);
        buildTrialSpawnPad(world, TRIAL_CENTER_X - 17, TRIAL_CENTER_Z + 13);
    }

    private static void buildTrialChamberShell(World world) {
        fill(world, TRIAL_MIN_X, TRIAL_MIN_Y, TRIAL_MIN_Z, TRIAL_MAX_X, TRIAL_MAX_Y, TRIAL_MAX_Z, Material.TUFF_BRICKS);
        fill(world, TRIAL_MIN_X + 1, TRIAL_FLOOR_Y, TRIAL_MIN_Z + 1, TRIAL_MAX_X - 1, TRIAL_MAX_Y - 2, TRIAL_MAX_Z - 1, Material.AIR);

        for (int y = TRIAL_FLOOR_Y + 1; y <= TRIAL_MAX_Y - 2; y++) {
            for (int x = TRIAL_MIN_X + 1; x <= TRIAL_MAX_X - 1; x++) {
                set(world, x, y, TRIAL_MIN_Z + 1, pickMaterial(TRIAL_TUFF, x, y, TRIAL_MIN_Z + 1));
                set(world, x, y, TRIAL_MAX_Z - 1, pickMaterial(TRIAL_TUFF, x, y, TRIAL_MAX_Z - 1));
            }
            for (int z = TRIAL_MIN_Z + 1; z <= TRIAL_MAX_Z - 1; z++) {
                set(world, TRIAL_MIN_X + 1, y, z, pickMaterial(TRIAL_TUFF, TRIAL_MIN_X + 1, y, z));
                set(world, TRIAL_MAX_X - 1, y, z, pickMaterial(TRIAL_TUFF, TRIAL_MAX_X - 1, y, z));
            }
        }

        for (int x = TRIAL_MIN_X; x <= TRIAL_MAX_X; x++) {
            for (int z = TRIAL_MIN_Z; z <= TRIAL_MAX_Z; z++) {
                set(world, x, TRIAL_MAX_Y - 1, z, pickMaterial(TRIAL_TUFF, x, TRIAL_MAX_Y - 1, z));
                set(world, x, TRIAL_MAX_Y, z, Material.TUFF_BRICKS);
            }
        }

        fill(world, TRIAL_MIN_X + 2, TRIAL_MAX_Y - 2, TRIAL_MIN_Z + 2, TRIAL_MAX_X - 2, TRIAL_MAX_Y - 2, TRIAL_MIN_Z + 3, Material.POLISHED_TUFF);
        fill(world, TRIAL_MIN_X + 2, TRIAL_MAX_Y - 2, TRIAL_MAX_Z - 3, TRIAL_MAX_X - 2, TRIAL_MAX_Y - 2, TRIAL_MAX_Z - 2, Material.POLISHED_TUFF);
        fill(world, TRIAL_MIN_X + 2, TRIAL_MAX_Y - 2, TRIAL_MIN_Z + 2, TRIAL_MIN_X + 3, TRIAL_MAX_Y - 2, TRIAL_MAX_Z - 2, Material.POLISHED_TUFF);
        fill(world, TRIAL_MAX_X - 3, TRIAL_MAX_Y - 2, TRIAL_MIN_Z + 2, TRIAL_MAX_X - 2, TRIAL_MAX_Y - 2, TRIAL_MAX_Z - 2, Material.POLISHED_TUFF);
    }

    private static void buildTrialChamberFloor(World world) {
        for (int x = TRIAL_MIN_X + 1; x <= TRIAL_MAX_X - 1; x++) {
            for (int z = TRIAL_MIN_Z + 1; z <= TRIAL_MAX_Z - 1; z++) {
                boolean copperLine = Math.floorMod(x - TRIAL_CENTER_X, 8) == 0 || Math.floorMod(z - TRIAL_CENTER_Z, 7) == 0;
                boolean darkTile = Math.floorMod((x - TRIAL_CENTER_X) * 5 + (z - TRIAL_CENTER_Z) * 3, 19) == 0;
                Material material = copperLine
                    ? pickMaterial(TRIAL_COPPER, x, TRIAL_FLOOR_Y, z)
                    : darkTile ? Material.OXIDIZED_COPPER : pickMaterial(TRIAL_FLOOR, x, TRIAL_FLOOR_Y, z);
                set(world, x, TRIAL_FLOOR_Y, z, material);
            }
        }

        fill(world, TRIAL_MIN_X + 1, TRIAL_FLOOR_Y, TRIAL_MIN_Z + 1, TRIAL_MAX_X - 1, TRIAL_FLOOR_Y, TRIAL_MIN_Z + 3, Material.CUT_COPPER);
        fill(world, TRIAL_MIN_X + 1, TRIAL_FLOOR_Y, TRIAL_MAX_Z - 3, TRIAL_MAX_X - 1, TRIAL_FLOOR_Y, TRIAL_MAX_Z - 1, Material.CUT_COPPER);
        fill(world, TRIAL_MIN_X + 1, TRIAL_FLOOR_Y, TRIAL_MIN_Z + 1, TRIAL_MIN_X + 3, TRIAL_FLOOR_Y, TRIAL_MAX_Z - 1, Material.CUT_COPPER);
        fill(world, TRIAL_MAX_X - 3, TRIAL_FLOOR_Y, TRIAL_MIN_Z + 1, TRIAL_MAX_X - 1, TRIAL_FLOOR_Y, TRIAL_MAX_Z - 1, Material.CUT_COPPER);
    }

    private static void buildTrialWallBands(World world) {
        for (int x = TRIAL_MIN_X + 1; x <= TRIAL_MAX_X - 1; x++) {
            set(world, x, 72, TRIAL_MIN_Z + 1, pickMaterial(TRIAL_COPPER, x, 72, TRIAL_MIN_Z + 1));
            set(world, x, 73, TRIAL_MIN_Z + 1, pickMaterial(TRIAL_COPPER, x, 73, TRIAL_MIN_Z + 1));
            set(world, x, 78, TRIAL_MIN_Z + 1, pickMaterial(TRIAL_COPPER, x, 78, TRIAL_MIN_Z + 1));
            set(world, x, 72, TRIAL_MAX_Z - 1, pickMaterial(TRIAL_COPPER, x, 72, TRIAL_MAX_Z - 1));
            set(world, x, 73, TRIAL_MAX_Z - 1, pickMaterial(TRIAL_COPPER, x, 73, TRIAL_MAX_Z - 1));
            set(world, x, 78, TRIAL_MAX_Z - 1, pickMaterial(TRIAL_COPPER, x, 78, TRIAL_MAX_Z - 1));
            if (Math.floorMod(x - TRIAL_CENTER_X, 16) == 0) {
                setLitCopperBulb(world, x, 74, TRIAL_MIN_Z + 1);
                setLitCopperBulb(world, x, 74, TRIAL_MAX_Z - 1);
                setLitCopperBulb(world, x, 80, TRIAL_MIN_Z + 1);
                setLitCopperBulb(world, x, 80, TRIAL_MAX_Z - 1);
            }
        }
        for (int z = TRIAL_MIN_Z + 1; z <= TRIAL_MAX_Z - 1; z++) {
            set(world, TRIAL_MIN_X + 1, 72, z, pickMaterial(TRIAL_COPPER, TRIAL_MIN_X + 1, 72, z));
            set(world, TRIAL_MIN_X + 1, 73, z, pickMaterial(TRIAL_COPPER, TRIAL_MIN_X + 1, 73, z));
            set(world, TRIAL_MIN_X + 1, 78, z, pickMaterial(TRIAL_COPPER, TRIAL_MIN_X + 1, 78, z));
            set(world, TRIAL_MAX_X - 1, 72, z, pickMaterial(TRIAL_COPPER, TRIAL_MAX_X - 1, 72, z));
            set(world, TRIAL_MAX_X - 1, 73, z, pickMaterial(TRIAL_COPPER, TRIAL_MAX_X - 1, 73, z));
            set(world, TRIAL_MAX_X - 1, 78, z, pickMaterial(TRIAL_COPPER, TRIAL_MAX_X - 1, 78, z));
            if (Math.floorMod(z - TRIAL_CENTER_Z, 12) == 0) {
                setLitCopperBulb(world, TRIAL_MIN_X + 1, 74, z);
                setLitCopperBulb(world, TRIAL_MAX_X - 1, 74, z);
                setLitCopperBulb(world, TRIAL_MIN_X + 1, 80, z);
                setLitCopperBulb(world, TRIAL_MAX_X - 1, 80, z);
            }
        }

        fill(world, TRIAL_MIN_X + 2, TRIAL_FLOOR_Y + 1, TRIAL_MIN_Z + 2, TRIAL_MIN_X + 4, TRIAL_MAX_Y - 2, TRIAL_MIN_Z + 4, Material.TUFF_BRICKS);
        fill(world, TRIAL_MAX_X - 4, TRIAL_FLOOR_Y + 1, TRIAL_MIN_Z + 2, TRIAL_MAX_X - 2, TRIAL_MAX_Y - 2, TRIAL_MIN_Z + 4, Material.TUFF_BRICKS);
        fill(world, TRIAL_MIN_X + 2, TRIAL_FLOOR_Y + 1, TRIAL_MAX_Z - 4, TRIAL_MIN_X + 4, TRIAL_MAX_Y - 2, TRIAL_MAX_Z - 2, Material.TUFF_BRICKS);
        fill(world, TRIAL_MAX_X - 4, TRIAL_FLOOR_Y + 1, TRIAL_MAX_Z - 4, TRIAL_MAX_X - 2, TRIAL_MAX_Y - 2, TRIAL_MAX_Z - 2, Material.TUFF_BRICKS);
    }

    private static void buildTrialLowerPit(World world) {
        fill(world, TRIAL_CENTER_X + 3, TRIAL_LOWER_FLOOR_Y, TRIAL_CENTER_Z + 4, TRIAL_CENTER_X + 22, TRIAL_LOWER_FLOOR_Y, TRIAL_CENTER_Z + 15, Material.OXIDIZED_CUT_COPPER);
        fill(world, TRIAL_CENTER_X + 5, TRIAL_LOWER_FLOOR_Y + 1, TRIAL_CENTER_Z + 6, TRIAL_CENTER_X + 20, TRIAL_FLOOR_Y + 3, TRIAL_CENTER_Z + 13, Material.AIR);
        buildTrialRim(world, TRIAL_CENTER_X + 3, TRIAL_CENTER_X + 22, TRIAL_CENTER_Z + 4, TRIAL_CENTER_Z + 15, TRIAL_FLOOR_Y, Material.CUT_COPPER);

        for (int step = 0; step < 4; step++) {
            int y = TRIAL_FLOOR_Y - step;
            fill(world, TRIAL_CENTER_X + 1 + step, y, TRIAL_CENTER_Z + 7, TRIAL_CENTER_X + 1 + step, y, TRIAL_CENTER_Z + 12, Material.CUT_COPPER);
            fill(world, TRIAL_CENTER_X + 1 + step, y + 1, TRIAL_CENTER_Z + 7, TRIAL_CENTER_X + 1 + step, y + 4, TRIAL_CENTER_Z + 12, Material.AIR);
        }

        fill(world, TRIAL_CENTER_X + 8, TRIAL_LOWER_FLOOR_Y + 1, TRIAL_CENTER_Z + 7, TRIAL_CENTER_X + 9, TRIAL_FLOOR_Y - 1, TRIAL_CENTER_Z + 8, Material.CUT_COPPER);
        fill(world, TRIAL_CENTER_X + 16, TRIAL_LOWER_FLOOR_Y + 1, TRIAL_CENTER_Z + 11, TRIAL_CENTER_X + 17, TRIAL_FLOOR_Y - 1, TRIAL_CENTER_Z + 12, Material.CUT_COPPER);
    }

    private static void buildTrialUpperPlatform(World world) {
        fill(world, TRIAL_CENTER_X - 30, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z - 20, TRIAL_CENTER_X - 18, TRIAL_UPPER_FLOOR_Y - 1, TRIAL_CENTER_Z - 9, Material.TUFF_BRICKS);
        fill(world, TRIAL_CENTER_X - 30, TRIAL_UPPER_FLOOR_Y, TRIAL_CENTER_Z - 20, TRIAL_CENTER_X - 18, TRIAL_UPPER_FLOOR_Y, TRIAL_CENTER_Z - 9, Material.CUT_COPPER);
        buildTrialRim(world, TRIAL_CENTER_X - 30, TRIAL_CENTER_X - 18, TRIAL_CENTER_Z - 20, TRIAL_CENTER_Z - 9, TRIAL_UPPER_FLOOR_Y, Material.WAXED_CUT_COPPER);
        fill(world, TRIAL_CENTER_X - 28, TRIAL_UPPER_FLOOR_Y + 1, TRIAL_CENTER_Z - 18, TRIAL_CENTER_X - 20, TRIAL_UPPER_FLOOR_Y + 3, TRIAL_CENTER_Z - 11, Material.AIR);
        set(world, TRIAL_CENTER_X - 24, TRIAL_UPPER_FLOOR_Y + 1, TRIAL_CENTER_Z - 14, Material.CHEST);

        fill(world, TRIAL_CENTER_X - 12, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z - 21, TRIAL_CENTER_X + 18, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z - 21, Material.CUT_COPPER);
        fill(world, TRIAL_CENTER_X - 10, TRIAL_FLOOR_Y + 3, TRIAL_CENTER_Z - 21, TRIAL_CENTER_X + 16, TRIAL_UPPER_FLOOR_Y, TRIAL_CENTER_Z - 21, Material.TUFF_BRICKS);
        fill(world, TRIAL_CENTER_X - 7, TRIAL_UPPER_FLOOR_Y + 1, TRIAL_CENTER_Z - 21, TRIAL_CENTER_X + 13, TRIAL_UPPER_FLOOR_Y + 3, TRIAL_CENTER_Z - 21, Material.CUT_COPPER);
        fill(world, TRIAL_CENTER_X - 4, TRIAL_UPPER_FLOOR_Y + 2, TRIAL_CENTER_Z - 21, TRIAL_CENTER_X + 6, TRIAL_UPPER_FLOOR_Y + 2, TRIAL_CENTER_Z - 21, Material.COPPER_GRATE);

        for (int step = 0; step <= 4; step++) {
            int y = TRIAL_FLOOR_Y + step;
            int z = TRIAL_CENTER_Z - 8 - step;
            fill(world, TRIAL_CENTER_X - 7 + step, y, z, TRIAL_CENTER_X + 13 - step, y, z + 1, Material.CUT_COPPER);
            fill(world, TRIAL_CENTER_X - 7 + step, y + 1, z, TRIAL_CENTER_X + 13 - step, y + 3, z + 1, Material.AIR);
        }

        fill(world, TRIAL_CENTER_X - 16, TRIAL_FLOOR_Y, TRIAL_CENTER_Z - 11, TRIAL_CENTER_X - 12, TRIAL_FLOOR_Y, TRIAL_CENTER_Z - 7, Material.CUT_COPPER);
        buildTrialRim(world, TRIAL_CENTER_X - 16, TRIAL_CENTER_X - 12, TRIAL_CENTER_Z - 11, TRIAL_CENTER_Z - 7, TRIAL_FLOOR_Y + 1, Material.CUT_COPPER);
        setLitCopperBulb(world, TRIAL_CENTER_X - 10, TRIAL_UPPER_FLOOR_Y + 1, TRIAL_CENTER_Z - 21);
        setLitCopperBulb(world, TRIAL_CENTER_X + 16, TRIAL_UPPER_FLOOR_Y + 1, TRIAL_CENTER_Z - 21);
    }

    private static void buildTrialSideDetails(World world) {
        fill(world, TRIAL_CENTER_X + 17, TRIAL_FLOOR_Y, TRIAL_CENTER_Z - 14, TRIAL_CENTER_X + 27, TRIAL_FLOOR_Y, TRIAL_CENTER_Z - 6, Material.OXIDIZED_CUT_COPPER);
        buildTrialRim(world, TRIAL_CENTER_X + 17, TRIAL_CENTER_X + 27, TRIAL_CENTER_Z - 14, TRIAL_CENTER_Z - 6, TRIAL_FLOOR_Y + 1, Material.CUT_COPPER);
        fill(world, TRIAL_CENTER_X + 19, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z - 12, TRIAL_CENTER_X + 25, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z - 8, Material.AIR);
        set(world, TRIAL_CENTER_X + 22, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z - 10, Material.VAULT);
        setLitCopperBulb(world, TRIAL_CENTER_X + 18, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z - 13);
        setLitCopperBulb(world, TRIAL_CENTER_X + 26, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z - 7);

        fill(world, TRIAL_CENTER_X - 29, TRIAL_FLOOR_Y, TRIAL_CENTER_Z - 4, TRIAL_CENTER_X - 18, TRIAL_FLOOR_Y, TRIAL_CENTER_Z + 4, Material.CUT_COPPER);
        buildTrialRim(world, TRIAL_CENTER_X - 29, TRIAL_CENTER_X - 18, TRIAL_CENTER_Z - 4, TRIAL_CENTER_Z + 4, TRIAL_FLOOR_Y + 1, Material.OXIDIZED_CUT_COPPER);
        for (int x = TRIAL_CENTER_X - 27; x <= TRIAL_CENTER_X - 20; x += 3) {
            set(world, x, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z - 2, Material.COPPER_GRATE);
            set(world, x, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 2, Material.COPPER_GRATE);
        }
        set(world, TRIAL_CENTER_X - 24, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z, Material.CHISELED_TUFF_BRICKS);

        fill(world, TRIAL_CENTER_X - 30, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 8, TRIAL_CENTER_X - 26, TRIAL_FLOOR_Y + 3, TRIAL_CENTER_Z + 16, Material.CUT_COPPER);
        fill(world, TRIAL_CENTER_X - 28, TRIAL_FLOOR_Y + 4, TRIAL_CENTER_Z + 10, TRIAL_CENTER_X - 28, TRIAL_FLOOR_Y + 5, TRIAL_CENTER_Z + 14, Material.COPPER_GRATE);
        setLitCopperBulb(world, TRIAL_CENTER_X - 26, TRIAL_FLOOR_Y + 4, TRIAL_CENTER_Z + 9);
        setLitCopperBulb(world, TRIAL_CENTER_X - 26, TRIAL_FLOOR_Y + 4, TRIAL_CENTER_Z + 15);

        fill(world, TRIAL_CENTER_X + 12, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 16, TRIAL_CENTER_X + 27, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 18, Material.CUT_COPPER);
        set(world, TRIAL_CENTER_X + 15, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z + 17, Material.COPPER_GRATE);
        set(world, TRIAL_CENTER_X + 20, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z + 17, Material.COPPER_GRATE);
        set(world, TRIAL_CENTER_X + 25, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z + 17, Material.COPPER_GRATE);
    }

    private static void buildTrialPillars(World world) {
        buildTrialPillar(world, TRIAL_CENTER_X - 10, TRIAL_CENTER_Z - 11);
        buildTrialPillar(world, TRIAL_CENTER_X + 22, TRIAL_CENTER_Z - 3);
        buildTrialPillar(world, TRIAL_CENTER_X - 21, TRIAL_CENTER_Z + 7);
        buildTrialPillar(world, TRIAL_CENTER_X + 25, TRIAL_CENTER_Z + 13);
    }

    private static void buildTrialPillar(World world, int centerX, int centerZ) {
        fill(world, centerX - 1, TRIAL_FLOOR_Y + 1, centerZ - 1, centerX + 1, TRIAL_MAX_Y - 2, centerZ + 1, Material.TUFF_BRICKS);
        fill(world, centerX - 2, 73, centerZ - 2, centerX + 2, 73, centerZ + 2, Material.CUT_COPPER);
        fill(world, centerX - 2, 79, centerZ - 2, centerX + 2, 79, centerZ + 2, Material.CUT_COPPER);
        fill(world, centerX - 2, TRIAL_UPPER_FLOOR_Y, centerZ - 2, centerX + 2, TRIAL_UPPER_FLOOR_Y, centerZ + 2, Material.CUT_COPPER);
        setLitCopperBulb(world, centerX - 2, 75, centerZ);
        setLitCopperBulb(world, centerX + 2, 75, centerZ);
        setLitCopperBulb(world, centerX, 75, centerZ - 2);
        setLitCopperBulb(world, centerX, 75, centerZ + 2);
    }

    private static void buildTrialCentralGrates(World world) {
        for (int x = TRIAL_CENTER_X - 15; x <= TRIAL_CENTER_X + 4; x++) {
            for (int z = TRIAL_CENTER_Z - 3; z <= TRIAL_CENTER_Z + 2; z++) {
                if (Math.floorMod(x + z, 2) == 0) {
                    set(world, x, TRIAL_FLOOR_Y + 1, z, Material.COPPER_GRATE);
                }
            }
        }
        fill(world, TRIAL_CENTER_X - 15, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z - 3, TRIAL_CENTER_X - 15, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z + 2, Material.COPPER_GRATE);
        fill(world, TRIAL_CENTER_X + 4, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z - 3, TRIAL_CENTER_X + 4, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z + 2, Material.COPPER_GRATE);
        fill(world, TRIAL_CENTER_X - 12, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 3, TRIAL_CENTER_X, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z + 3, Material.COPPER_GRATE);

        for (int x = TRIAL_CENTER_X - 14; x <= TRIAL_CENTER_X + 3; x += 5) {
            set(world, x, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z - 4, Material.CUT_COPPER);
            set(world, x, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z - 4, Material.COPPER_GRATE);
            set(world, x, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 4, Material.CUT_COPPER);
            set(world, x, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z + 4, Material.COPPER_GRATE);
        }
    }

    private static void buildTrialDecor(World world) {
        buildTrialSpawnerPod(world, TRIAL_CENTER_X - 28, TRIAL_CENTER_Z + 12, Material.CHISELED_TUFF_BRICKS);
        buildTrialSpawnerPod(world, TRIAL_CENTER_X + 28, TRIAL_CENTER_Z - 18, Material.CHISELED_TUFF_BRICKS);
        buildTrialSpawnerPod(world, TRIAL_CENTER_X - 27, TRIAL_CENTER_Z - 3, Material.VAULT);
        buildTrialSpawnerPod(world, TRIAL_CENTER_X + 27, TRIAL_CENTER_Z + 7, Material.VAULT);

        set(world, TRIAL_CENTER_X - 30, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 16, Material.CHEST);
        set(world, TRIAL_CENTER_X - 29, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 16, Material.CHEST);

        for (int x = TRIAL_CENTER_X - 24; x <= TRIAL_CENTER_X + 24; x += 12) {
            setLitCopperBulb(world, x, TRIAL_FLOOR_Y + 3, TRIAL_CENTER_Z - 21);
            setLitCopperBulb(world, x, TRIAL_FLOOR_Y + 7, TRIAL_CENTER_Z - 21);
        }
        for (int z = TRIAL_CENTER_Z - 12; z <= TRIAL_CENTER_Z + 12; z += 8) {
            setLitCopperBulb(world, TRIAL_MIN_X + 1, TRIAL_FLOOR_Y + 4, z);
            setLitCopperBulb(world, TRIAL_MAX_X - 1, TRIAL_FLOOR_Y + 4, z);
            set(world, TRIAL_MIN_X + 2, TRIAL_FLOOR_Y + 3, z, Material.COPPER_GRATE);
            set(world, TRIAL_MAX_X - 2, TRIAL_FLOOR_Y + 3, z, Material.COPPER_GRATE);
        }

        fill(world, TRIAL_CENTER_X - 3, TRIAL_FLOOR_Y + 1, TRIAL_CENTER_Z + 12, TRIAL_CENTER_X + 1, TRIAL_FLOOR_Y + 2, TRIAL_CENTER_Z + 18, Material.CUT_COPPER);
        fill(world, TRIAL_CENTER_X - 1, TRIAL_FLOOR_Y + 3, TRIAL_CENTER_Z + 13, TRIAL_CENTER_X - 1, TRIAL_FLOOR_Y + 4, TRIAL_CENTER_Z + 17, Material.COPPER_GRATE);
    }

    private static void buildTrialSpawnerPod(World world, int centerX, int centerZ, Material centerMaterial) {
        fill(world, centerX - 2, TRIAL_FLOOR_Y + 1, centerZ - 2, centerX + 2, TRIAL_FLOOR_Y + 1, centerZ + 2, Material.CUT_COPPER);
        set(world, centerX, TRIAL_FLOOR_Y + 2, centerZ, centerMaterial);
        setLitCopperBulb(world, centerX - 2, TRIAL_FLOOR_Y + 2, centerZ);
        setLitCopperBulb(world, centerX + 2, TRIAL_FLOOR_Y + 2, centerZ);
        set(world, centerX, TRIAL_FLOOR_Y + 2, centerZ - 2, Material.COPPER_GRATE);
        set(world, centerX, TRIAL_FLOOR_Y + 2, centerZ + 2, Material.COPPER_GRATE);
    }

    private static void buildTrialSpawnPad(World world, int centerX, int centerZ) {
        fill(world, centerX - 1, TRIAL_FLOOR_Y, centerZ - 1, centerX + 1, TRIAL_FLOOR_Y, centerZ + 1, Material.OXIDIZED_CUT_COPPER);
        set(world, centerX, TRIAL_FLOOR_Y, centerZ, Material.CUT_COPPER);
        fill(world, centerX - 1, TRIAL_FLOOR_Y + 1, centerZ - 1, centerX + 1, TRIAL_FLOOR_Y + 4, centerZ + 1, Material.AIR);
    }

    private static void buildTrialRim(World world, int minX, int maxX, int minZ, int maxZ, int y, Material material) {
        fill(world, minX, y, minZ, maxX, y, minZ, material);
        fill(world, minX, y, maxZ, maxX, y, maxZ, material);
        fill(world, minX, y, minZ, minX, y, maxZ, material);
        fill(world, maxX, y, minZ, maxX, y, maxZ, material);
    }

    private static void buildPillar(World world, int x, int fromY, int z, int toY) {
        fill(world, x, fromY, z, x, toY, z, Material.BEDROCK);
    }

    private static void buildStartingPad(World world, int centerX, int y, int centerZ) {
        fill(world, centerX - 1, y, centerZ - 1, centerX + 1, y, centerZ + 1, Material.BEDROCK);
        set(world, centerX, y + 1, centerZ, Material.AIR);
    }

    private static void buildMiniIsland(World world, int centerX, int y, int centerZ) {
        buildRoundPlatform(world, centerX, y, centerZ, 2, Material.BEDROCK);
        buildRoundPlatform(world, centerX, y - 1, centerZ, 1, Material.SMOOTH_STONE);
        fill(world, centerX, 60, centerZ, centerX, y - 1, centerZ, Material.DEEPSLATE_TILES);
        set(world, centerX, y + 1, centerZ, Material.AIR);
    }

    private static void buildWreckageSpawnIsland(World world, int centerX, int y, int centerZ, int variant) {
        buildPattern(world, centerX, y, centerZ, WRECKAGE_SPAWN_TOP, WRECKAGE_SURFACE);
        buildPattern(world, centerX, y - 1, centerZ, WRECKAGE_SPAWN_FILL, WRECKAGE_FILL);
        buildPattern(world, centerX, y - 2, centerZ, WRECKAGE_SPAWN_CORE, WRECKAGE_CORE);

        buildHangingRubble(world, centerX - 1, y - 3, centerZ + 1, 7);
        buildHangingRubble(world, centerX + 2, y - 2, centerZ - 1, 5);
        buildHangingRubble(world, centerX - 3, y - 2, centerZ - 2, 4);

        if (variant % 2 == 0) {
            buildBrokenPillar(world, centerX - 2, y + 1, centerZ + 1, 3);
            buildBrokenPillar(world, centerX + 2, y + 1, centerZ - 2, 2);
        } else {
            buildBrokenPillar(world, centerX + 1, y + 1, centerZ + 2, 3);
            buildBrokenPillar(world, centerX - 2, y + 1, centerZ - 1, 2);
        }

        set(world, centerX, y + 1, centerZ, Material.AIR);
        set(world, centerX + 1, y + 1, centerZ, Material.AIR);
        set(world, centerX, y + 1, centerZ + 1, Material.AIR);
    }

    private static void buildWreckageCore(World world, int centerX, int y, int centerZ) {
        buildPattern(world, centerX, y, centerZ, WRECKAGE_CORE_TOP, WRECKAGE_SURFACE);
        buildPattern(world, centerX, y - 1, centerZ, WRECKAGE_CORE_FILL, WRECKAGE_FILL);
        buildPattern(world, centerX, y - 2, centerZ, WRECKAGE_CORE_BASE, WRECKAGE_CORE);

        buildPattern(world, centerX - 4, y + 2, centerZ - 2, WRECKAGE_SHARD_TOP, WRECKAGE_SURFACE);
        buildPattern(world, centerX + 5, y + 1, centerZ + 3, WRECKAGE_SHARD_TOP, WRECKAGE_SURFACE);
        buildPattern(world, centerX + 1, y + 3, centerZ - 5, WRECKAGE_SHARD_TOP, WRECKAGE_FILL);

        buildBrokenPillar(world, centerX - 5, y + 1, centerZ - 1, 4);
        buildBrokenPillar(world, centerX + 4, y + 1, centerZ + 1, 3);
        buildBrokenPillar(world, centerX + 1, y + 1, centerZ + 5, 2);

        buildHangingRubble(world, centerX - 1, y - 3, centerZ, 8);
        buildHangingRubble(world, centerX + 2, y - 3, centerZ - 2, 6);
        buildHangingRubble(world, centerX - 4, y - 2, centerZ + 3, 5);
    }

    private static void buildWreckageFragment(World world, int centerX, int y, int centerZ, int variant) {
        buildPattern(world, centerX, y, centerZ, WRECKAGE_FRAGMENT_TOP, WRECKAGE_SURFACE);
        buildPattern(world, centerX, y - 1, centerZ, WRECKAGE_FRAGMENT_FILL, WRECKAGE_FILL);
        buildPattern(world, centerX, y - 2, centerZ, WRECKAGE_FRAGMENT_CORE, WRECKAGE_CORE);

        buildHangingRubble(world, centerX, y - 3, centerZ, 4 + (variant % 2));
        if (variant == 0 || variant == 2) {
            buildBrokenPillar(world, centerX - 1, y + 1, centerZ, 2);
        } else {
            buildBrokenPillar(world, centerX + 1, y + 1, centerZ - 1, 2);
        }
    }

    private static void buildShard(World world, int centerX, int y, int centerZ) {
        buildPattern(world, centerX, y, centerZ, WRECKAGE_SHARD_TOP, WRECKAGE_SURFACE);
        buildHangingRubble(world, centerX, y - 1, centerZ, 3);
    }

    private static void buildLobbyPillar(World world, int x, int z) {
        fill(world, x, 97, z, x, 100, z, Material.QUARTZ_PILLAR);
        set(world, x, 101, z, Material.SEA_LANTERN);
    }

    private static void buildRingMajorIsland(World world, int centerX, int y, int centerZ, boolean horizontal) {
        int radiusX = horizontal ? 7 : 5;
        int radiusZ = horizontal ? 5 : 7;
        buildPaletteOval(world, centerX, y, centerZ, radiusX, radiusZ, RING_SURFACE);
        buildPaletteOval(world, centerX, y - 1, centerZ, Math.max(3, radiusX - 1), Math.max(3, radiusZ - 1), RING_FILL);
        buildPaletteOval(world, centerX, y - 2, centerZ, Math.max(2, radiusX - 3), Math.max(2, radiusZ - 3), RING_FILL);

        if (horizontal) {
            buildRingSupport(world, centerX - 4, y - 3, centerZ);
            buildRingSupport(world, centerX, y - 3, centerZ);
            buildRingSupport(world, centerX + 4, y - 3, centerZ);
            set(world, centerX - 5, y, centerZ, Material.SEA_LANTERN);
            set(world, centerX + 5, y, centerZ, Material.SEA_LANTERN);
            set(world, centerX, y, centerZ - 3, Material.SEA_LANTERN);
            set(world, centerX, y, centerZ + 3, Material.SEA_LANTERN);
        } else {
            buildRingSupport(world, centerX, y - 3, centerZ - 4);
            buildRingSupport(world, centerX, y - 3, centerZ);
            buildRingSupport(world, centerX, y - 3, centerZ + 4);
            set(world, centerX, y, centerZ - 5, Material.SEA_LANTERN);
            set(world, centerX, y, centerZ + 5, Material.SEA_LANTERN);
            set(world, centerX - 3, y, centerZ, Material.SEA_LANTERN);
            set(world, centerX + 3, y, centerZ, Material.SEA_LANTERN);
        }
    }

    private static void buildRingConnectorNode(World world, int centerX, int y, int centerZ) {
        buildPaletteOval(world, centerX, y, centerZ, 4, 4, RING_SURFACE);
        buildPaletteOval(world, centerX, y - 1, centerZ, 3, 3, RING_FILL);
        buildPaletteOval(world, centerX, y - 2, centerZ, 2, 2, RING_FILL);
        buildRingSupport(world, centerX, y - 3, centerZ);
        set(world, centerX - 2, y, centerZ, Material.SEA_LANTERN);
        set(world, centerX + 2, y, centerZ, Material.SEA_LANTERN);
    }

    private static void buildRingInwardSpur(World world, int centerX, int y, int centerZ) {
        buildPaletteOval(world, centerX, y, centerZ, 2, 2, RING_SURFACE);
        buildPaletteOval(world, centerX, y - 1, centerZ, 1, 1, RING_FILL);
        buildRingSupport(world, centerX, y - 2, centerZ);
    }

    private static void buildRingBridge(World world, int startX, int y, int startZ, int endX, int endZ) {
        int steps = Math.max(Math.abs(endX - startX), Math.abs(endZ - startZ)) * 3;
        for (int step = 0; step <= steps; step++) {
            double progress = steps == 0 ? 0.0D : step / (double) steps;
            int x = (int) Math.round(startX + ((endX - startX) * progress));
            int z = (int) Math.round(startZ + ((endZ - startZ) * progress));

            buildPaletteRoundPlatform(world, x, y, z, 1, RING_SURFACE);
            set(world, x, y - 1, z, pickMaterial(RING_FILL, x, y - 1, z));
            if (step % 4 == 0) {
                buildRingSupport(world, x, y - 2, z);
            }
        }
    }

    private static void buildRingDebris(World world, int centerX, int y, int centerZ) {
        buildPaletteRoundPlatform(world, centerX, y, centerZ, 2, RING_SURFACE);
        set(world, centerX, y - 1, centerZ, pickMaterial(RING_FILL, centerX, y - 1, centerZ));
        buildRingSupport(world, centerX, y - 2, centerZ);
    }

    private static void buildRingSupport(World world, int x, int fromY, int z) {
        int baseY = 60;
        if (fromY < baseY) {
            return;
        }
        fill(world, x, baseY, z, x, fromY, z, pickMaterial(RING_FILL, x, baseY, z));
    }

    private static void buildLobbyRailing(World world) {
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                if (!isLobbyRailingBlock(x, z)) {
                    continue;
                }
                set(world, x, 97, z, Material.WHITE_STAINED_GLASS);
            }
        }
    }

    private static void buildLobbySafetyBarriers(World world) {
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                if (isLobbyEdgeBarrierBlock(x, z)) {
                    int fromY = isLobbyRailingBlock(x, z) ? 98 : 97;
                    fill(world, x, fromY, z, x, 99, z, Material.BARRIER);
                }
            }
        }

        fill(world, -11, 97, -11, 11, 98, -11, Material.BARRIER);
        fill(world, -11, 97, 11, 11, 98, 11, Material.BARRIER);
        fill(world, -11, 97, -10, -11, 98, 10, Material.BARRIER);
        fill(world, 11, 97, -10, 11, 98, 10, Material.BARRIER);
    }

    private static boolean isLobbyRailingBlock(int x, int z) {
        int distance = (x * x) + (z * z);
        boolean edge = distance >= 74 && distance <= 104;
        boolean openNorth = Math.abs(x) <= 1 && z <= -7;
        boolean openSouth = Math.abs(x) <= 1 && z >= 7;
        boolean openWest = Math.abs(z) <= 1 && x <= -7;
        boolean openEast = Math.abs(z) <= 1 && x >= 7;
        return edge && !openNorth && !openSouth && !openWest && !openEast;
    }

    private static boolean isLobbyEdgeBarrierBlock(int x, int z) {
        int distance = (x * x) + (z * z);
        boolean edgeShell = distance >= 74 && distance <= 130;
        boolean openNorth = Math.abs(x) <= 1 && z <= -7;
        boolean openSouth = Math.abs(x) <= 1 && z >= 7;
        boolean openWest = Math.abs(z) <= 1 && x <= -7;
        boolean openEast = Math.abs(z) <= 1 && x >= 7;
        return edgeShell && !openNorth && !openSouth && !openWest && !openEast;
    }

    private static void buildPattern(World world, int centerX, int y, int centerZ, String[] pattern, Material[] palette) {
        int startZ = centerZ - (pattern.length / 2);
        for (int row = 0; row < pattern.length; row++) {
            String line = pattern[row];
            int startX = centerX - (line.length() / 2);
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == '.') {
                    continue;
                }
                int x = startX + col;
                int z = startZ + row;
                set(world, x, y, z, pickMaterial(palette, x, y, z));
            }
        }
    }

    private static void buildPaletteOval(World world, int centerX, int y, int centerZ, int radiusX, int radiusZ, Material[] palette) {
        double radiusXSquared = radiusX * radiusX;
        double radiusZSquared = radiusZ * radiusZ;
        for (int x = centerX - radiusX; x <= centerX + radiusX; x++) {
            for (int z = centerZ - radiusZ; z <= centerZ + radiusZ; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                double normalizedDistance = (dx * dx) / radiusXSquared + (dz * dz) / radiusZSquared;
                if (normalizedDistance <= 1.0D) {
                    set(world, x, y, z, pickMaterial(palette, x, y, z));
                }
            }
        }
    }

    private static void buildPaletteRoundPlatform(World world, int centerX, int y, int centerZ, int radius, Material[] palette) {
        int radiusSquared = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if ((dx * dx) + (dz * dz) <= radiusSquared) {
                    set(world, x, y, z, pickMaterial(palette, x, y, z));
                }
            }
        }
    }

    private static void buildBrokenPillar(World world, int x, int fromY, int z, int height) {
        for (int y = fromY; y < fromY + height; y++) {
            set(world, x, y, z, y == fromY + height - 1 ? Material.CHISELED_STONE_BRICKS : Material.STONE_BRICK_WALL);
        }
    }

    private static void buildHangingRubble(World world, int x, int fromY, int z, int length) {
        for (int y = fromY; y > fromY - length; y--) {
            set(world, x, y, z, pickMaterial(WRECKAGE_CORE, x, y, z));
        }

        int tipY = fromY - length;
        set(world, x + 1, tipY + 1, z, Material.COBBLED_DEEPSLATE);
        set(world, x, tipY, z + 1, Material.POLISHED_DEEPSLATE);
    }

    private static Material pickMaterial(Material[] palette, int x, int y, int z) {
        long hash = (x * 73428767L) ^ (y * 912931L) ^ (z * 438289L);
        return palette[(int) Math.floorMod(hash, palette.length)];
    }

    private static void buildRingPlatform(World world, int centerX, int y, int centerZ, int outerRadius, int innerRadius, Material rimMaterial, Material centerMaterial) {
        int outerRadiusSquared = outerRadius * outerRadius;
        int innerRadiusSquared = innerRadius * innerRadius;
        for (int x = centerX - outerRadius; x <= centerX + outerRadius; x++) {
            for (int z = centerZ - outerRadius; z <= centerZ + outerRadius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                int distance = (dx * dx) + (dz * dz);
                if (distance > outerRadiusSquared) {
                    continue;
                }

                set(world, x, y, z, distance <= innerRadiusSquared ? rimMaterial : centerMaterial);
            }
        }
    }

    private static void buildRoundPlatform(World world, int centerX, int y, int centerZ, int radius, Material material) {
        int radiusSquared = radius * radius;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                int dx = x - centerX;
                int dz = z - centerZ;
                if ((dx * dx) + (dz * dz) <= radiusSquared) {
                    set(world, x, y, z, material);
                }
            }
        }
    }

    private static void fill(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    set(world, x, y, z, material);
                }
            }
        }
    }

    private static void set(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != material) {
            block.setType(material, false);
        }
    }

    private static void setLog(World world, int x, int y, int z, Material material, Axis axis) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != material) {
            block.setType(material, false);
        }
        if (block.getBlockData() instanceof Orientable data && data.getAxis() != axis) {
            data.setAxis(axis);
            block.setBlockData(data, false);
        }
    }

    private static void setFacing(World world, int x, int y, int z, Material material, BlockFace face) {
        Block block = world.getBlockAt(x, y, z);
        BlockData data = Bukkit.createBlockData(material);
        if (data instanceof Directional directional && directional.getFaces().contains(face)) {
            directional.setFacing(face);
        }
        block.setBlockData(data, false);
    }

    private static void setLitCopperBulb(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        BlockData data = Bukkit.createBlockData(Material.COPPER_BULB);
        if (data instanceof Lightable lightable) {
            lightable.setLit(true);
        }
        block.setBlockData(data, false);
    }
}
