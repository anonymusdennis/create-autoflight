package dev.createautoflight.content.navigation;

import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.ChunkPos;

/**
 * Iterates solid blocks stored in a Sable plot without touching the overworld chunk loader.
 */
public final class PlotBlockScanner {
    @FunctionalInterface
    public interface PlotBlockConsumer {
        void accept(int plotX, int plotY, int plotZ, BlockState state);
    }

    private PlotBlockScanner() {}

    public static void forEachSolidBlock(LevelPlot plot, PlotBlockConsumer consumer) {
        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) {
                continue;
            }
            scanChunk(chunk, consumer);
        }
    }

    public static void forEachSolidBlockInBox(
            LevelPlot plot,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            PlotBlockConsumer consumer
    ) {
        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) {
                continue;
            }
            ChunkPos cp = chunk.getPos();
            int chunkMaxX = cp.getMaxBlockX();
            int chunkMaxZ = cp.getMaxBlockZ();
            if (chunkMaxX < minX || cp.getMinBlockX() > maxX
                    || chunkMaxZ < minZ || cp.getMinBlockZ() > maxZ) {
                continue;
            }
            scanChunkInBox(chunk, minX, minY, minZ, maxX, maxY, maxZ, consumer);
        }
    }

    private static void scanChunk(LevelChunk chunk, PlotBlockConsumer consumer) {
        ChunkPos cp = chunk.getPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); sectionIndex++) {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int baseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (!state.isAir()) {
                            consumer.accept(baseX + x, baseY + y, baseZ + z, state);
                        }
                    }
                }
            }
        }
    }

    private static void scanChunkInBox(
            LevelChunk chunk,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            PlotBlockConsumer consumer
    ) {
        ChunkPos cp = chunk.getPos();
        int baseX = cp.getMinBlockX();
        int baseZ = cp.getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); sectionIndex++) {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            int baseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            int sectionMaxY = baseY + 15;
            if (sectionMaxY < minY || baseY > maxY) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                int worldY = baseY + y;
                if (worldY < minY || worldY > maxY) {
                    continue;
                }
                for (int z = 0; z < 16; z++) {
                    int worldZ = baseZ + z;
                    if (worldZ < minZ || worldZ > maxZ) {
                        continue;
                    }
                    for (int x = 0; x < 16; x++) {
                        int worldX = baseX + x;
                        if (worldX < minX || worldX > maxX) {
                            continue;
                        }
                        BlockState state = section.getBlockState(x, y, z);
                        if (!state.isAir()) {
                            consumer.accept(worldX, worldY, worldZ, state);
                        }
                    }
                }
            }
        }
    }

}
