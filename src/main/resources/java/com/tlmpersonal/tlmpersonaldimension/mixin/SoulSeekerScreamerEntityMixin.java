package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "net.mcreator.soulseeker.entity.Screamer1Entity",
    "net.mcreator.soulseeker.entity.Screamer2Entity", 
    "net.mcreator.soulseeker.entity.Screamer3Entity",
    "net.mcreator.soulseeker.entity.RandomScreamer1Entity",
    "net.mcreator.soulseeker.entity.DeathscreamerEntity"
})
public abstract class SoulSeekerScreamerEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, remap = false)
    private void tlmpersonal$suppressScreamerTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Level level = self.level();

        if (Touhoulittlemaidpersonaldimension.shouldSuppressSoulSeekerEffects(self)) {
            self.discard();
            ci.cancel();
            return;
        }

        net.minecraft.world.entity.player.Player player = level.getNearestPlayer(self, 64.0D);
        if (player != null && Touhoulittlemaidpersonaldimension.shouldSuppressSoulSeekerEffects(player)) {
            self.discard();
            ci.cancel();
        }
    }
}
