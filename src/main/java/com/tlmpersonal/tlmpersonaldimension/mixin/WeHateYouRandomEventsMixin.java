package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.wehateyou.RandomEvents")
public abstract class WeHateYouRandomEventsMixin {

    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tlmpersonal$skipProtectedRandomEvents(PlayerTickEvent.Post event, CallbackInfo ci) {
        if (event.getEntity() instanceof ServerPlayer player
                && Touhoulittlemaidpersonaldimension.shouldSuppressWeHateYouEffects(
                player.serverLevel(), player.blockPosition(), player)) {
            ci.cancel();
        }
    }
}
