package com.tlmpersonal.tlmpersonaldimension.worldgen;

import com.mojang.serialization.MapCodec;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/**
 * The Cherry Portal as a real world-gen structure, so it shows up in {@code /locate structure
 * touhoulittlemaidpersonaldimension:cherry_portal} exactly like {@code my_island} does.
 *
 * <p>Where candidates are tried is decided by the structure set (data/.../worldgen/structure_set/cherry_portals.json:
 * spacing / separation) and which biomes are valid by the biome tag (tags/worldgen/biome/has_structure/cherry_portal.json).
 * The in-game config only decides whether a candidate spot actually becomes a portal. Because the check lives here
 * (and not in a mixin), {@code /locate} and real generation always agree.
 */
public class CherryPortalStructure extends Structure {
    public static final MapCodec<CherryPortalStructure> CODEC = simpleCodec(CherryPortalStructure::new);
    private static final ResourceLocation TEMPLATE_ID = Touhoulittlemaidpersonaldimension.id("cherry_portal");

    public CherryPortalStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    /** Factory used by the registry so the StructureType lambda can be typed. */
    public static StructureType<CherryPortalStructure> createType() {
        return () -> CODEC;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        boolean enabled;
        double chance;
        try {
            enabled = Config.CHERRY_PORTAL_SPAWN_ENABLED.get();
            chance = Config.CHERRY_PORTAL_SPAWN_CHANCE.get();
        } catch (IllegalStateException e) {
            return Optional.empty(); // config not loaded yet
        }
        if (!enabled || chance <= 0.0) return Optional.empty();
        // ctx.random() is seeded per chunk + world seed, so this roll is deterministic for a given spot.
        if (chance < 100.0 && ctx.random().nextDouble() * 100.0 >= chance) return Optional.empty();

        StructureTemplate template = ctx.structureTemplateManager().getOrCreate(TEMPLATE_ID);
        Vec3i size = template.getSize();
        if (size.getX() == 0 || size.getZ() == 0) return Optional.empty();

        ChunkPos chunk = ctx.chunkPos();
        int x0 = chunk.getMinBlockX() + (16 - size.getX()) / 2;
        int z0 = chunk.getMinBlockZ() + (16 - size.getZ()) / 2;
        int x1 = x0 + size.getX() - 1;
        int z1 = z0 + size.getZ() - 1;

        // Sample the four corners plus the middle: the spot must be (nearly) flat and dry.
        int[][] samples = {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}, {(x0 + x1) / 2, (z0 + z1) / 2}};
        int minGround = Integer.MAX_VALUE;
        int maxGround = Integer.MIN_VALUE;
        for (int[] s : samples) {
            int top = ctx.chunkGenerator().getFirstOccupiedHeight(s[0], s[1],
                    Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            int ground = top - 1;
            NoiseColumn column = ctx.chunkGenerator().getBaseColumn(s[0], s[1], ctx.heightAccessor(), ctx.randomState());
            if (!column.getBlock(ground).getFluidState().isEmpty()) return Optional.empty(); // water / lava
            if (!column.getBlock(ground + 1).getFluidState().isEmpty()) return Optional.empty();
            minGround = Math.min(minGround, ground);
            maxGround = Math.max(maxGround, ground);
        }
        if (maxGround - minGround > 1) return Optional.empty();

        // Sit ON the surface: the template's bottom layer goes one block above the highest ground sample, so the
        // portal is not sunken into the terrain (CherryPortalPiece fills any gap below with dirt).
        BlockPos pos = new BlockPos(x0, maxGround + 1, z0);
        return Optional.of(new GenerationStub(pos, builder ->
                builder.addPiece(new CherryPortalPiece(ctx.structureTemplateManager(), TEMPLATE_ID, pos))));
    }

    @Override
    public StructureType<?> type() {
        return Touhoulittlemaidpersonaldimension.CHERRY_PORTAL_STRUCTURE.get();
    }
}
