package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Stops bees from leaving hives in personal dimensions when bees are not an allowed entity.
 *
 * Without this, the hive plays its "bee exit" sound and then tries to add the bee to the
 * level. Our EntityJoinLevelEvent handler cancels that join, so the bee stays inside the hive
 * and the hive retries (and replays the sound) every tick.
 * Cancelling at HEAD means no sound, no entity creation and no retry spam.
 */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin {
    @Inject(method = "releaseOccupant", at = @At("HEAD"), cancellable = true)
    private static void tlmpersonal$releaseOccupant(
            Level pLevel,
            BlockPos pPos,
            BlockState pState,
            @Coerce Object pBeeData,
            @Nullable List<Entity> pStoredBees,
            @Coerce Object pReleaseStatus,
            @Nullable BlockPos pFlowerPos,
            CallbackInfoReturnable<Boolean> cir) {

        if (pLevel.isClientSide || !(pLevel instanceof ServerLevel serverLevel)) return;
        if (!Touhoulittlemaidpersonaldimension.isOurDimension(pLevel.dimension())) return;

        // Resolve the owner so per-player allowed/blocked entity settings are honoured
        UUID ownerId = Touhoulittlemaidpersonaldimension.getOwnerUUIDFromDimensionKey(pLevel.dimension());
        if (ownerId == null) {
            ownerId = Touhoulittlemaidpersonaldimension.getOwnerUUIDFromPosition(serverLevel, pPos.getX(), pPos.getZ());
        }

        if (!Touhoulittlemaidpersonaldimension.isAllowed(EntityType.BEE, ownerId, serverLevel, null)) {
            // false = bee was not released; it simply stays in the hive
            cir.setReturnValue(false);
        }
    }
}
