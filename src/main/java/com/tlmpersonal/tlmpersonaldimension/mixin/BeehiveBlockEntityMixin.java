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
            // false = bee not released; it stays in the hive (no exit sound, no retry spam)
            cir.setReturnValue(false);
        }
    }
}
