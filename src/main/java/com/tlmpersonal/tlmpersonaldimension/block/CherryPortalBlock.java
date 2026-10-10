package com.tlmpersonal.tlmpersonaldimension.block;

import com.mojang.serialization.MapCodec;
import com.tlmpersonal.tlmpersonaldimension.MaidTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Flat pink portal that lies on the ground (Twilight Forest style). Players and allowed entities stepping in toggle the personal dimension. */
public class CherryPortalBlock extends Block {
    public static final MapCodec<CherryPortalBlock> CODEC = simpleCodec(CherryPortalBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);

    public CherryPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }


    /**
     * Nether-portal style survival: every portal block needs a solid floor and, on each horizontal side, either
     * another portal block or a bank block (grass/dirt). If not, it turns to air, which in turn updates its
     * portal neighbours, so the whole portal collapses.
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.UP) return state;
        boolean valid;
        if (direction == Direction.DOWN) {
            valid = neighborState.isFaceSturdy(level, neighborPos, Direction.UP);
        } else {
            valid = neighborState.is(this) || neighborState.is(BlockTags.DIRT);
        }
        return valid ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (entity instanceof ServerPlayer player) {
            if (player.isOnPortalCooldown()) {
                player.setPortalCooldown(40);
                return;
            }
            player.setPortalCooldown(40);
            MaidTeleporter.portalTeleport(player);
            return;
        }
        // Any other entity behaves like it does in a nether portal: it is moved if the dimension rules allow it.
        if (entity.isOnPortalCooldown()) {
            entity.setPortalCooldown(40);
            return;
        }
        entity.setPortalCooldown(40);
        MaidTeleporter.portalTeleportEntity(entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.4F, random.nextFloat() * 0.4F + 0.8F, false);
        }
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CHERRY_LEAVES,
                    pos.getX() + random.nextDouble(), pos.getY() + 13.0 / 16.0 + 0.1 + random.nextDouble() * 0.7, pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.02, 0.03, (random.nextDouble() - 0.5) * 0.02);
        }
    }
}
