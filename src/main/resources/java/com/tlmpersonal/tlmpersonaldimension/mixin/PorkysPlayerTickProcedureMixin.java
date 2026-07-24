package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "net.mcreator.porkyslegacy_eoc.procedures.PlayerTickProcedure",
    "net.mcreator.porkyslegacy_eoc.procedures.AnguishPlayerTickRenderProcedure",
    "net.mcreator.porkyslegacy_eoc.procedures.CorruptedValleyFogCodeProcedure"
})
public abstract class PorkysPlayerTickProcedureMixin {

    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tlmpersonal$suppressPorkysPlayerTick(PlayerTickEvent.Post event, CallbackInfo ci) {
        Player player = event.getEntity();
        if (Touhoulittlemaidpersonaldimension.shouldSuppressPorkysFear(player)) {
            ci.cancel();
        }
    }
}
