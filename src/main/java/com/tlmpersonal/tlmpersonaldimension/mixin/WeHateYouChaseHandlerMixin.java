package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.mcreator.wehateyou.init.WeHateYouModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.wehateyou.ChaseHandler")
public abstract class WeHateYouChaseHandlerMixin {

    @Shadow(remap = false)
    private static boolean countdownActive;

    @Shadow(remap = false)
    private static boolean chaseActive;

    @Invoker("stopMusic")
    private static void tlmpersonal$invokeStopMusic(ServerPlayer player, SoundEvent sound) {
        throw new AssertionError();
    }

    @Invoker("resetState")
    private static void tlmpersonal$invokeResetState() {
        throw new AssertionError();
    }

    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tlmpersonal$skipProtectedChase(PlayerTickEvent.Post event, CallbackInfo ci) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !Touhoulittlemaidpersonaldimension.shouldSuppressWeHateYouEffects(
                player.serverLevel(), player.blockPosition(), player)) {
            return;
        }

        if (countdownActive) {
            tlmpersonal$invokeStopMusic(player, WeHateYouModSounds.COUNTDOWN.get());
        }
        if (chaseActive) {
            tlmpersonal$invokeStopMusic(player, WeHateYouModSounds.CHASEAFTERCOUNTDOWN.get());
            player.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
        }
        if (countdownActive || chaseActive) {
            tlmpersonal$invokeResetState();
        }

        ci.cancel();
    }
}
