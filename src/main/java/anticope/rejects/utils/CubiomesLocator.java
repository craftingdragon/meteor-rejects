package anticope.rejects.utils;

import dev.xpple.cubiomes.CubiomesInit;
import dev.xpple.cubiomes.Cubiomes;
import dev.xpple.cubiomes.Generator;
import dev.xpple.cubiomes.Pos;
import dev.xpple.cubiomes.StrongholdIter;
import dev.xpple.cubiomes.StructureConfig;
import net.minecraft.core.BlockPos;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/** Structure positions for versions newer than the legacy Cubiomes binding supports. */
public final class CubiomesLocator {
    private static boolean loaded;

    private CubiomesLocator() {}

    private static synchronized void load() {
        if (!loaded) {
            CubiomesInit.load();
            loaded = true;
        }
    }

    public static boolean supports(String version) {
        if (version == null) return false;
        return switch (version) {
            case "26.1", "26.1.1", "26.1.2", "26.2", "26.3",
                "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4",
                "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9",
                "1.21.10", "1.21.11" -> true;
            default -> false;
        };
    }

    public static BlockPos locate(cubitect.Cubiomes.StructureType feature, BlockPos origin, long seed, String version) {
        if (!supports(version)) throw new IllegalArgumentException("Unsupported Minecraft version: " + version);
        load();
        int mc = mcVersion(version);

        try (Arena arena = Arena.ofConfined()) {
            if (feature == cubitect.Cubiomes.StructureType.Slime_Chunk) return findSlimeChunk(seed, origin);

            MemorySegment generator = Generator.allocate(arena);
            int dimension = dimension(feature);
            Cubiomes.setupGenerator(generator, mc, dimension);
            Cubiomes.applySeed(generator, dimension, seed);

            if (feature == cubitect.Cubiomes.StructureType.Stronghold) return findStronghold(arena, generator, mc, seed, origin);

            int type = structureType(feature);
            MemorySegment config = StructureConfig.allocate(arena);
            if (Cubiomes.getStructureConfig(type, mc, config) == 0) return null;
            int regionBlocks = Byte.toUnsignedInt(StructureConfig.regionSize(config)) * 16;
            if (regionBlocks == 0) return null;
            int centerX = Math.floorDiv(origin.getX(), regionBlocks);
            int centerZ = Math.floorDiv(origin.getZ(), regionBlocks);
            MemorySegment pos = Pos.allocate(arena);

            // Search outward in region rings and return the closest viable position found.
            BlockPos nearest = null;
            long bestDistance = Long.MAX_VALUE;
            for (int radius = 0; radius <= 32; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        if (Cubiomes.getStructurePos(type, mc, seed, centerX + dx, centerZ + dz, pos) == 0) continue;
                        int x = Pos.x(pos);
                        int z = Pos.z(pos);
                        if (Cubiomes.isViableStructurePos(type, generator, x, z, 0) == 0) continue;
                        if (Cubiomes.isViableStructureTerrain(type, generator, x, z) == 0) continue;
                        long distance = squaredDistance(origin, x, z);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            nearest = new BlockPos(x, 0, z);
                        }
                    }
                }
                if (nearest != null && radius >= 2) return nearest;
            }
            return nearest;
        }
    }

    private static BlockPos findStronghold(Arena arena, MemorySegment generator, int mc, long seed, BlockPos origin) {
        MemorySegment iter = StrongholdIter.allocate(arena);
        Cubiomes.initFirstStronghold(arena, iter, mc, seed);
        BlockPos nearest = null;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < 128 && Cubiomes.nextStronghold(iter, generator) != 0; i++) {
            MemorySegment pos = StrongholdIter.pos(iter);
            int x = Pos.x(pos);
            int z = Pos.z(pos);
            long distance = squaredDistance(origin, x, z);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = new BlockPos(x, 0, z);
            }
        }
        return nearest;
    }

    private static BlockPos findSlimeChunk(long seed, BlockPos origin) {
        int cx = Math.floorDiv(origin.getX(), 16);
        int cz = Math.floorDiv(origin.getZ(), 16);
        for (int radius = 0; radius <= 64; radius++) {
            BlockPos nearest = null;
            long bestDistance = Long.MAX_VALUE;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    if (Cubiomes.isSlimeChunk(seed, cx + dx, cz + dz) == 0) continue;
                    int x = (cx + dx) * 16 + 8;
                    int z = (cz + dz) * 16 + 8;
                    long distance = squaredDistance(origin, x, z);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        nearest = new BlockPos(x, 0, z);
                    }
                }
            }
            if (nearest != null) return nearest;
        }
        return null;
    }

    private static long squaredDistance(BlockPos origin, int x, int z) {
        long dx = (long) x - origin.getX();
        long dz = (long) z - origin.getZ();
        return dx * dx + dz * dz;
    }

    private static int dimension(cubitect.Cubiomes.StructureType feature) {
        return switch (feature) {
            case Fortress, Bastion, Ruined_Portal_N -> Cubiomes.DIM_NETHER();
            case End_City, End_Gateway -> Cubiomes.DIM_END();
            default -> Cubiomes.DIM_OVERWORLD();
        };
    }

    private static int structureType(cubitect.Cubiomes.StructureType feature) {
        return switch (feature) {
            case Feature -> Cubiomes.Feature();
            case Desert_Pyramid -> Cubiomes.Desert_Pyramid();
            case Jungle_Temple -> Cubiomes.Jungle_Temple();
            case Swamp_Hut -> Cubiomes.Swamp_Hut();
            case Igloo -> Cubiomes.Igloo();
            case Village -> Cubiomes.Village();
            case Ocean_Ruin -> Cubiomes.Ocean_Ruin();
            case Shipwreck -> Cubiomes.Shipwreck();
            case Monument -> Cubiomes.Monument();
            case Mansion -> Cubiomes.Mansion();
            case Outpost -> Cubiomes.Outpost();
            case Ruined_Portal -> Cubiomes.Ruined_Portal();
            case Ruined_Portal_N -> Cubiomes.Ruined_Portal_N();
            case Ancient_City -> Cubiomes.Ancient_City();
            case Treasure -> Cubiomes.Treasure();
            case Mineshaft -> Cubiomes.Mineshaft();
            case Desert_Well -> Cubiomes.Desert_Well();
            case Geode -> Cubiomes.Geode();
            case Fortress -> Cubiomes.Fortress();
            case Bastion -> Cubiomes.Bastion();
            case End_City -> Cubiomes.End_City();
            case End_Gateway -> Cubiomes.End_Gateway();
            case Trail_Ruin -> Cubiomes.Trail_Ruins();
            case Stronghold, Slime_Chunk -> throw new IllegalArgumentException("Special structure: " + feature);
        };
    }

    private static int mcVersion(String version) {
        return switch (version) {
            case "26.3" -> Cubiomes.MC_26_3();
            case "26.2" -> Cubiomes.MC_26_2();
            case "26.1", "26.1.1", "26.1.2" -> Cubiomes.MC_26_1();
            case "1.21.11" -> Cubiomes.MC_1_21_11();
            case "1.21.9", "1.21.10" -> Cubiomes.MC_1_21_9();
            case "1.21.6", "1.21.7", "1.21.8" -> Cubiomes.MC_1_21_6();
            case "1.21.5" -> Cubiomes.MC_1_21_5();
            case "1.21.4" -> Cubiomes.MC_1_21_4();
            case "1.21.2", "1.21.3" -> Cubiomes.MC_1_21_3();
            case "1.21", "1.21.1" -> Cubiomes.MC_1_21_1();
            default -> 0;
        };
    }
}
