package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.porkyslegacy_eoc.procedures.WorldTickProcedure")
public abstract class PorkysWorldTickProcedureMixin {

    @Inject(method = "onWorldTick", at = @At("TAIL"), remap = false)
    private static void tlmpersonal$clampFearAfterPorkysTick(LevelTickEvent.Post event, CallbackInfo ci) {
        Level level = event.getLevel();
        for (Player player : level.players()) {
            if (Touhoulittlemaidpersonaldimension.shouldSuppressPorkysFear(player)) {
                Touhoulittlemaidpersonaldimension.resetPorkysFear(player);
            }
        }
    }
}
