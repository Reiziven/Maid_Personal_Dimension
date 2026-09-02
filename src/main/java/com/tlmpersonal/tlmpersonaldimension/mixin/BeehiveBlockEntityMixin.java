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

/**
 * DISABLED FOR FORGE 1.20.1 COMPATIBILITY
 * 
 * This mixin was incomplete and causing runtime crashes.
 * If beehive-specific functionality is needed in personal dimensions,
 * implement it here by hooking into BeehiveBlockEntity methods.
 * 
 * Note: The TouhouLittleMaid mod's honey collection task should work
 * without this mixin as it operates on the block entity level.
 */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin {
    // Currently no mixins implemented
    // Add functionality here if needed for personal dimension beehive behavior
}



