package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "net.mcreator.wehateyou.WorldSculptorEvent")
public abstract class WeHateYouWorldSculptorMixin {

    @Invoker("generateRandomStructure")
    private static void tlmpersonal$invokeGenerateRandomStructure(ServerLevel level, BlockPos pos, ServerPlayer player) {
        throw new AssertionError();
    }

    @Invoker("deleteDistantChunk")
    private static void tlmpersonal$invokeDeleteDistantChunk(ServerLevel level, BlockPos pos, ServerPlayer player) {
        throw new AssertionError();
    }

    @Redirect(
            method = "onServerTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/mcreator/wehateyou/WorldSculptorEvent;generateRandomStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerPlayer;)V"
            ),
            remap = false
    )
    private static void tlmpersonal$skipProtectedStructureSpawns(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!Touhoulittlemaidpersonaldimension.shouldSuppressWeHateYouEffects(level, pos, player)) {
            tlmpersonal$invokeGenerateRandomStructure(level, pos, player);
        }
    }

    @Redirect(
            method = "onServerTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/mcreator/wehateyou/WorldSculptorEvent;deleteDistantChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerPlayer;)V"
            ),
            remap = false
    )
    private static void tlmpersonal$skipProtectedChunkDeletion(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!Touhoulittlemaidpersonaldimension.shouldSuppressWeHateYouEffects(level, pos, player)) {
            tlmpersonal$invokeDeleteDistantChunk(level, pos, player);
        }
    }
}
