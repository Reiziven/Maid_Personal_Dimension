package com.tlmpersonal.tlmpersonaldimension.mixin;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
    "net.mcreator.soulseeker.procedures.BroadcastProcedure",
    "net.mcreator.soulseeker.procedures.CheckIsNearbyProcedure",
    "net.mcreator.soulseeker.procedures.CleaningProcedure",
    "net.mcreator.soulseeker.procedures.CmdgiveProcedure",
    "net.mcreator.soulseeker.procedures.CursedItemProcedure",
    "net.mcreator.soulseeker.procedures.CursedProcedure",
    "net.mcreator.soulseeker.procedures.Dialogue1Procedure",
    "net.mcreator.soulseeker.procedures.Dialogue2Procedure",
    "net.mcreator.soulseeker.procedures.Dialogue3Procedure",
    "net.mcreator.soulseeker.procedures.Dialogue4Procedure",
    "net.mcreator.soulseeker.procedures.Dialogue5Procedure",
    "net.mcreator.soulseeker.procedures.Dialogue6Procedure",
    "net.mcreator.soulseeker.procedures.Dialogue7Procedure",
    "net.mcreator.soulseeker.procedures.Dialogue8Procedure",
    "net.mcreator.soulseeker.procedures.Dialogue9Procedure",
    "net.mcreator.soulseeker.procedures.DropProcedure",
    "net.mcreator.soulseeker.procedures.DyingTextProcedure",
    "net.mcreator.soulseeker.procedures.EntityIsHitProcedure",
    "net.mcreator.soulseeker.procedures.FinishParalysisProcedure",
    "net.mcreator.soulseeker.procedures.FishingProcedure",
    "net.mcreator.soulseeker.procedures.Gamerule1Procedure",
    "net.mcreator.soulseeker.procedures.GetterProcedure",
    "net.mcreator.soulseeker.procedures.GiveProcedure",
    "net.mcreator.soulseeker.procedures.HeatDeathProcedure",
    "net.mcreator.soulseeker.procedures.HerobrinesShrineLogicProcedure",
    "net.mcreator.soulseeker.procedures.HollowDyingTextProcedure",
    "net.mcreator.soulseeker.procedures.IsInRangeProcedure",
    "net.mcreator.soulseeker.procedures.LookAtHimProcedure",
    "net.mcreator.soulseeker.procedures.LookersTickProcedure",
    "net.mcreator.soulseeker.procedures.Otmetka3Procedure",
    "net.mcreator.soulseeker.procedures.ParalysisProdProcedure",
    "net.mcreator.soulseeker.procedures.PlayDialogueProcedure",
    "net.mcreator.soulseeker.procedures.ScaryEventProcedure",
    "net.mcreator.soulseeker.procedures.Scream2SpawnProdProcedure",
    "net.mcreator.soulseeker.procedures.Screamer1ProdProcedure",
    "net.mcreator.soulseeker.procedures.ScreamSpawnProdProcedure",
    "net.mcreator.soulseeker.procedures.ScreamUseProcedure",
    "net.mcreator.soulseeker.procedures.ShowfiolProcedure",
    "net.mcreator.soulseeker.procedures.SitstartProcedure",
    "net.mcreator.soulseeker.procedures.SitterPriObnovlieniiTikaSushchnostiProcedure",
    "net.mcreator.soulseeker.procedures.SpawnLurker1Procedure",
    "net.mcreator.soulseeker.procedures.SpawnLurker2Procedure",
    "net.mcreator.soulseeker.procedures.SpawnLurker3Procedure",
    "net.mcreator.soulseeker.procedures.SpeedModProcedure",
    "net.mcreator.soulseeker.procedures.StartParalysis1Procedure",
    "net.mcreator.soulseeker.procedures.StartParalysisProcedure",
    "net.mcreator.soulseeker.procedures.StukachUslProcedure",
    "net.mcreator.soulseeker.procedures.SuperScrySpawningProcedure",
    "net.mcreator.soulseeker.procedures.SuperScrySpawnprodProcedure",
    "net.mcreator.soulseeker.procedures.TableDestroyProcedure",
    "net.mcreator.soulseeker.procedures.TableSpawnProcedure",
    "net.mcreator.soulseeker.procedures.TorchEventProcedure",
    "net.mcreator.soulseeker.procedures.UseKillProcedure",
    "net.mcreator.soulseeker.procedures.UseScrameProcedure",
    "net.mcreator.soulseeker.procedures.VverhProdProcedure",
    "net.mcreator.soulseeker.procedures.WalkerTickProcedure",
    "net.mcreator.soulseeker.procedures.WorldTickProcedure"
})
public abstract class SoulSeekerAllProceduresMixin {

    @Inject(method = "execute", at = @At("HEAD"), cancellable = true, remap = false)
    private static void tlmpersonal$suppressSoulSeekerProcedure(LevelAccessor world, double x, double y, double z, net.minecraft.world.entity.Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player) {
            if (Touhoulittlemaidpersonaldimension.shouldSuppressSoulSeekerEffects(player)) {
                ci.cancel();
            }
        } else if (entity != null) {
            if (Touhoulittlemaidpersonaldimension.shouldSuppressSoulSeekerEffects(entity)) {
                ci.cancel();
            }
        } else if (world != null && world instanceof Level level) {
            BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            if (Touhoulittlemaidpersonaldimension.shouldSuppressSoulSeekerEffects(level, pos, null)) {
                ci.cancel();
            }
        }
    }
}