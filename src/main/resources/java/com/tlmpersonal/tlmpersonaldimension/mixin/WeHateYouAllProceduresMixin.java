package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "net.mcreator.wehateyou.EntityBehavior",
    "net.mcreator.wehateyou.Error00x0InterfaceLogic",
    "net.mcreator.wehateyou.FakeMultiplayer",
    "net.mcreator.wehateyou.GhostChatEvent",
    "net.mcreator.wehateyou.HeStalkerEvent",
    "net.mcreator.wehateyou.HerobrineShrineEvent",
    "net.mcreator.wehateyou.ParanoiaEvents",
    "net.mcreator.wehateyou.PlayerBaseSpawnerEvent",
    "net.mcreator.wehateyou.RandomEvents",
    "net.mcreator.wehateyou.RedactedLogicEvent",
    "net.mcreator.wehateyou.SecretDialoguesEvent",
    "net.mcreator.wehateyou.UnknownStalkerEvent",
    "net.mcreator.wehateyou.User404Mechanics",
    "net.mcreator.wehateyou.WatchEvent",
    "net.mcreator.wehateyou.WorldSculptorEvent",
    "net.mcreator.wehateyou.WhisperEvent",
    "net.mcreator.wehateyou.ZombieMutationEvent",
    "net.mcreator.wehateyou.procedures.ChasevoidProcedure",
    "net.mcreator.wehateyou.procedures.HEConditionDeLectureProcedure",
    "net.mcreator.wehateyou.procedures.HollowkillProcedure",
    "net.mcreator.wehateyou.procedures.HollowspawnProcedure",
    "net.mcreator.wehateyou.procedures.MidnightMessageEventProcedure",
    "net.mcreator.wehateyou.procedures.OverrideMoonTextureProcedure",
    "net.mcreator.wehateyou.procedures.RandomanvilsProcedure",
    "net.mcreator.wehateyou.procedures.RandomthunderProcedure",
    "net.mcreator.wehateyou.procedures.SecondmoontextureProcedure",
    "net.mcreator.wehateyou.procedures.ShowGlitchscreen2Procedure",
    "net.mcreator.wehateyou.procedures.ShowGlitchscreen3Procedure",
    "net.mcreator.wehateyou.procedures.ShowGlitchscreen4Procedure",
    "net.mcreator.wehateyou.procedures.ShowGlitchscreen5Procedure",
    "net.mcreator.wehateyou.procedures.ShowGlitchscreenProcedure",
    "net.mcreator.wehateyou.procedures.TeleporttovoidProcedure",
    "net.mcreator.wehateyou.procedures.TheUnknownSpawnProcedure",
    "net.mcreator.wehateyou.procedures.ThefirstPlaybackCondition2Procedure",
    "net.mcreator.wehateyou.procedures.ThefirstPlaybackConditionProcedure",
    "net.mcreator.wehateyou.procedures.ThirdmoontextureProcedure",
    "net.mcreator.wehateyou.procedures.VOIDBLOCKEntityCollidesInTheBlockProcedure",
    "net.mcreator.wehateyou.procedures.Void1ambienceProcedure"
})
public abstract class WeHateYouAllProceduresMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tlmpersonal$suppressWeHateYouProcedure(LevelAccessor world, CallbackInfo ci) {
        if (world instanceof Level level) {
            if (Touhoulittlemaidpersonaldimension.shouldSuppressWeHateYouEffects(level, null, null)) {
                ci.cancel();
            }
        }
    }
}