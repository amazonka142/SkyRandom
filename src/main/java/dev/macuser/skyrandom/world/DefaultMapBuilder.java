package dev.macuser.skyrandom.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class DefaultMapBuilder {

    private static final int DEMO_REGION_MIN_X = -40;
    private static final int DEMO_REGION_MAX_X = 350;
    private static final int DEMO_REGION_MAX_Y = 130;
    private static final int DEMO_REGION_MIN_Z = -40;
    private static final int DEMO_REGION_MAX_Z = 40;
    private static final int THRONE_CENTER_X = 90;
    private static final int THRONE_CENTER_Z = 0;
    private static final int THRONE_PLATFORM_Y = 70;
    private static final int WRECKAGE_CENTER_X = 190;
    private static final int WRECKAGE_CENTER_Z = 0;
    private static final int WRECKAGE_PLATFORM_Y = 70;
    private static final int RING_CENTER_X = 300;
    private static final int RING_CENTER_Z = 0;
    private static final int RING_PLATFORM_Y = 70;

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
        set(world, 0, 97, -6, Material.OAK_SIGN);
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
}
