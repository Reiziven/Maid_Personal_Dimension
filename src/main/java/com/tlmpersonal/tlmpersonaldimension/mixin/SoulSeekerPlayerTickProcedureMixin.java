package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "net.mcreator.soulseeker.procedures.ScreamUseProcedure",
    "net.mcreator.soulseeker.procedures.UseKillProcedure",
    "net.mcreator.soulseeker.procedures.VverhProdProcedure"
})
public abstract class SoulSeekerPlayerTickProcedureMixin {

    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tlmpersonal$skipProtectedSoulSeekerPlayerTick(PlayerTickEvent.Post event, CallbackInfo ci) {
        Entity entity = event.getEntity();
        if (Touhoulittlemaidpersonaldimension.shouldSuppressSoulSeekerEffects(entity)) {
            ci.cancel();
        }
    }
}
