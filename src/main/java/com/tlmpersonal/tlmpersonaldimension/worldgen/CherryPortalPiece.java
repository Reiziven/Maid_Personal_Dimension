package com.tlmpersonal.tlmpersonaldimension.worldgen;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * The single template piece (cherry_portal.nbt) of a CherryPortalStructure.
 */
public class CherryPortalPiece extends TemplateStructurePiece {

    public CherryPortalPiece(
            StructureTemplateManager templateManager,
            ResourceLocation templateId,
            BlockPos pos) {
        super(
                Touhoulittlemaidpersonaldimension.CHERRY_PORTAL_PIECE.get(),
                0,
                templateManager,
                templateId,
                templateId.toString(),
                makeSettings(),
                pos
        );
    }

    /**
     * Deserialization constructor used when loading the structure piece from disk.
     */
    public CherryPortalPiece(StructureTemplateManager templateManager, CompoundTag tag) {
        super(
                Touhoulittlemaidpersonaldimension.CHERRY_PORTAL_PIECE.get(),
                tag,
                templateManager,
                id -> makeSettings()
        );
    }

    private static StructurePlaceSettings makeSettings() {
        return new StructurePlaceSettings().setIgnoreEntities(true);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot) {
        super.postProcess(level, structureManager, generator, random, box, chunkPos, pivot);
        // The template now sits one block above the terrain; fill any air/fluid gap below it so nothing floats
        // and the portal blocks always have a solid floor.
        int baseY = this.boundingBox.minY();
        for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
            for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
                for (int y = baseY - 1; y >= baseY - 6; y--) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!box.isInside(p)) break;
                    BlockState state = level.getBlockState(p);
                    if (state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced()) {
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(
            String marker,
            BlockPos pos,
            ServerLevelAccessor level,
            RandomSource random,
            BoundingBox box) {
        // No data markers in cherry_portal.nbt.
    }
}