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
@Mixin(targets = {
    "net.mcreator.wehateyou.DoorKnockEvent",
    "net.mcreator.wehateyou.FakeMultiplayer",
    "net.mcreator.wehateyou.GhostChatEvent",
    "net.mcreator.wehateyou.HeStalkerEvent",
    "net.mcreator.wehateyou.UnknownStalkerEvent",
    "net.mcreator.wehateyou.WatchEvent",
    "net.mcreator.wehateyou.EntityBehavior",
    "net.mcreator.wehateyou.Error00x0InterfaceLogic",
    "net.mcreator.wehateyou.RedactedLogicEvent",
    "net.mcreator.wehateyou.procedures.ChasevoidProcedure",
    "net.mcreator.wehateyou.procedures.MidnightMessageEventProcedure",
    "net.mcreator.wehateyou.procedures.RandomanvilsProcedure",
    "net.mcreator.wehateyou.procedures.TheUnknownSpawnProcedure"
})
public abstract class WeHateYouPlayerTickMixin {

    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tlmpersonal$skipProtectedPlayerTickEvents(PlayerTickEvent.Post event, CallbackInfo ci) {
        if (event.getEntity() != null && Touhoulittlemaidpersonaldimension.shouldSuppressWeHateYouEffects(event.getEntity())) {
            ci.cancel();
        }
    }
}
