package com.tlmpersonal.tlmpersonaldimension.event;

import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Twilight Forest style portal creation: dig a pool of still water (at least 2x2) whose banks are
 * grass/dirt with a flower or mushroom on top of every bank block, then throw a cake into the water.
 * The whole pool turns into Cherry Portal blocks.
 */
public final class CherryPortalCreation {
    /** The item that activates the portal (Twilight Forest uses a diamond). */
    private static boolean isActivator(ItemStack stack) {
        return stack.is(Items.CAKE);
    }

    private static final int MIN_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 64;
    /** Stop watching a thrown cake after this many ticks (60 seconds). */
    private static final int MAX_TRACK_TICKS = 1200;

    /** Thrown cakes waiting to land in water. Only touched on the server thread. */
    private static final List<ItemEntity> TRACKED = new ArrayList<>();

    private CherryPortalCreation() {
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemEntity item = event.getEntity();
        if (item.level().isClientSide) return;
        if (!Config.CHERRY_PORTAL_CREATION_ENABLED.get()) return;
        if (isActivator(item.getItem())) {
            TRACKED.add(item);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            tick(level);
        }
    }

    private static void tick(ServerLevel level) {
        if (TRACKED.isEmpty()) return;
        if (!Config.CHERRY_PORTAL_CREATION_ENABLED.get()) {
            TRACKED.clear(); // toggled off while cakes were still flying
            return;
        }
        Iterator<ItemEntity> it = TRACKED.iterator();
        while (it.hasNext()) {
            ItemEntity item = it.next();
            if (item.isRemoved() || item.tickCount > MAX_TRACK_TICKS || !isActivator(item.getItem())) {
                it.remove();
                continue;
            }
            if (item.level() != level) continue;

            BlockPos pos = item.blockPosition();
            if (!isPoolWater(level.getBlockState(pos))) {
                pos = pos.below();
                if (!isPoolWater(level.getBlockState(pos))) continue;
            }
            if (tryCreatePortal(level, pos, item)) {
                it.remove();
            }
        }
    }

    private static boolean tryCreatePortal(ServerLevel level, BlockPos start, ItemEntity catalyst) {
        if (!hasSolidFloor(level, start)) return false;

        Set<BlockPos> pool = new HashSet<>();
        Set<BlockPos> banks = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        pool.add(start);
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(dir);
                if (pool.contains(next) || banks.contains(next)) continue;

                BlockState state = level.getBlockState(next);
                if (isPoolWater(state) && hasSolidFloor(level, next)) {
                    pool.add(next);
                    queue.add(next);
                    if (pool.size() > MAX_POOL_SIZE) return false;
                } else if (state.is(BlockTags.DIRT) && isDecoration(level.getBlockState(next.above()))) {
                    banks.add(next);
                } else {
                    return false; // leak, missing flower, wrong bank block...
                }
            }
        }
        if (pool.size() < MIN_POOL_SIZE) return false;

        catalyst.getItem().shrink(1);

        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(start.getX() + 0.5, start.getY(), start.getZ() + 0.5);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);

        BlockState portal = Touhoulittlemaidpersonaldimension.CHERRY_PORTAL_BLOCK.get().defaultBlockState();
        for (BlockPos pos : pool) {
            level.setBlock(pos, portal, 2);
        }
        return true;
    }

    /** Still (source) water only, like the water block Twilight Forest requires. */
    private static boolean isPoolWater(BlockState state) {
        return state.is(Blocks.WATER) && state.getValue(LiquidBlock.LEVEL) == 0;
    }

    private static boolean isDecoration(BlockState state) {
        return state.is(BlockTags.FLOWERS) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM);
    }

    private static boolean hasSolidFloor(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
