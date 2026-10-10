package com.tlmpersonal.tlmpersonaldimension.worldgen;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
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
    protected void handleDataMarker(
            String marker,
            BlockPos pos,
            ServerLevelAccessor level,
            RandomSource random,
            BoundingBox box) {
        // No data markers in cherry_portal.nbt.
    }
}