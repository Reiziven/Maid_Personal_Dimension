package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
    "net.mcreator.soulseeker.procedures.ShowfiolProcedure",
    "net.mcreator.soulseeker.procedures.ScreameUslProcedure",
    "net.mcreator.soulseeker.procedures.ScreameUsl2Procedure",
    "net.mcreator.soulseeker.procedures.ScreameUsl3Procedure",
    "net.mcreator.soulseeker.procedures.ScreameUsl4Procedure",
    "net.mcreator.soulseeker.procedures.ScreameUsl5Procedure",
    "net.mcreator.soulseeker.procedures.ScreameUsl6Procedure",
    "net.mcreator.soulseeker.procedures.ScreameUsl7Procedure"
})
public abstract class SoulSeekerOverlayConditionMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tlmpersonal$hideProtectedSoulSeekerOverlays(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Touhoulittlemaidpersonaldimension.shouldSuppressSoulSeekerEffects(entity)) {
            cir.setReturnValue(false);
        }
    }
}
