package com.tlmpersonal.tlmpersonaldimension.network;

import com.github.tartaricacid.touhoulittlemaid.data.PowerAttachment;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncDataPackage;
import com.github.tartaricacid.touhoulittlemaid.data.MaidNumAttachment;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.PlayerDimensionManager;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;
import java.util.UUID;

import static com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension.MODID;

public record TeleportToPersonalDimensionPacket(int maidId, boolean teleportWithMaid) implements CustomPacketPayload {
    public static final Type<TeleportToPersonalDimensionPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "teleport_to_personal_dimension"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportToPersonalDimensionPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, TeleportToPersonalDimensionPacket::maidId,
            ByteBufCodecs.BOOL, TeleportToPersonalDimensionPacket::teleportWithMaid,
            TeleportToPersonalDimensionPacket::new
        );

    @Override
    public Type<TeleportToPersonalDimensionPacket> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel currentLevel = player.serverLevel();
            Entity entity = currentLevel.getEntity(this.maidId);
            if (!(entity instanceof EntityMaid maid)) return;

            if (maid.getFavorabilityManager().getLevel() < 3) {
                player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.favorability_too_low"));
                return;
            }

            boolean isJoining = !Touhoulittlemaidpersonaldimension.isOurDimension(currentLevel.dimension());
            if (!checkCooldown(player, isJoining)) {
                return;
            }

            if (Config.TELEPORT_HAS_COST.get()) {
                double powerCost = Config.TELEPORT_COST_POWER_POINTS.get();
                int xpCost = Config.TELEPORT_COST_XP.get();

                PowerAttachment powerData = player.getData(InitDataAttachment.POWER_NUM);
                if (powerCost > 0.0 && powerData.get() < (float) powerCost) {
                    player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_power"));
                    return;
                }
                if (xpCost > 0 && player.experienceLevel < xpCost) {
                    player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_xp"));
                    return;
                }

                // Deduct costs from PLAYER power points
                if (powerCost > 0.0) {
                    powerData.min((float) powerCost);
                    syncPowerToClient(player);
                }
                if (xpCost > 0) {
                    player.giveExperienceLevels(-xpCost);
                }
            }

            ServerLevel targetDim;
            UUID ownerUUID = player.getUUID(); // Default to player as owner
            
            if (Touhoulittlemaidpersonaldimension.isOurDimension(currentLevel.dimension())) {
                targetDim = player.server.getLevel(Level.OVERWORLD);
                if (targetDim == null) return;
                String targetDimStr = targetDim.dimension().location().toString();
                if (!Config.DIMENSION_WHITELIST.get().contains(targetDimStr)) {
                    player.sendSystemMessage(Component.literal("This dimension is not whitelisted!"));
                    return;
                }
            } else {
                targetDim = PlayerDimensionManager.getOrCreatePlayerDimensionSync(player.server, ownerUUID);
            }

            if (targetDim != null) {
                if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim.dimension())) {
                    if (!Touhoulittlemaidpersonaldimension.isPlayerAllowed(player, ownerUUID, targetDim, null)) {
                        player.sendSystemMessage(Component.literal("You are not allowed in this dimension!"));
                        return;
                    }
                }

                applyCooldown(player, isJoining);
                teleportPlayerAndMaid(player, maid, targetDim);
            }
        });
    }

    private void syncPowerToClient(ServerPlayer player) {
        PowerAttachment powerData = player.getData(InitDataAttachment.POWER_NUM);
        MaidNumAttachment maidNumData = player.getData(InitDataAttachment.MAID_NUM);
        PacketDistributor.sendToPlayer(player, new SyncDataPackage(powerData.get(), maidNumData.get()));
    }

    private boolean checkCooldown(ServerPlayer player, boolean isJoining) {
        int cooldownSec = isJoining ? Config.TELEPORT_JOIN_COOLDOWN.get() : Config.TELEPORT_LEAVE_COOLDOWN.get();
        if (cooldownSec <= 0) return true;

        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(player.server.getLevel(Level.OVERWORLD));
        Long lastTeleport = isJoining ? savedData.getGlobalJoinCooldown(player.getUUID()) : savedData.getGlobalLeaveCooldown(player.getUUID());
        long now = System.currentTimeMillis();

        if (lastTeleport != null && (now - lastTeleport) < (long) cooldownSec * 1000) {
            long remaining = (cooldownSec * 1000 - (now - lastTeleport)) / 1000;
            player.sendSystemMessage(Component.literal("Teleportation (" + (isJoining ? "joining" : "leaving") + ") on cooldown! (" + remaining + "s)"));
            return false;
        }
        return true;
    }

    private void applyCooldown(ServerPlayer player, boolean isJoining) {
        int cooldownSec = isJoining ? Config.TELEPORT_JOIN_COOLDOWN.get() : Config.TELEPORT_LEAVE_COOLDOWN.get();
        if (cooldownSec > 0) {
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(player.server.getLevel(Level.OVERWORLD));
            if (isJoining) savedData.setGlobalJoinCooldown(player.getUUID(), System.currentTimeMillis());
            else savedData.setGlobalLeaveCooldown(player.getUUID(), System.currentTimeMillis());
        }
    }

    private void teleportPlayerAndMaid(ServerPlayer player, EntityMaid maid, ServerLevel targetDim) {
        Entity maidEntity = null;
        if (this.teleportWithMaid) {
            maidEntity = maid;
        }

        Touhoulittlemaidpersonaldimension.saveEntityPosition(player, player.level().dimension());
        if (maidEntity != null) {
            Touhoulittlemaidpersonaldimension.saveEntityPosition(maidEntity, player.level().dimension());
        }

        double targetX, targetY, targetZ;
        float targetYRot = player.getYRot();
        float targetXRot = player.getXRot();

        Touhoulittlemaidpersonaldimension.TeleportLocation savedPos = Touhoulittlemaidpersonaldimension.getEntityPosition(player.getUUID(), targetDim.dimension());

        if (savedPos != null) {
            targetX = savedPos.x();
            targetY = savedPos.y();
            targetZ = savedPos.z();
            targetYRot = savedPos.yRot();
            targetXRot = savedPos.xRot();
        } else {
            if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim.dimension())) {
                targetX = 0.5;
                targetZ = 0.5;
                BlockPos targetPos = new BlockPos((int)targetX, 100, (int)targetZ);
                StructurePlacer.placeSkyIsland(targetDim, targetPos);
                targetY = Touhoulittlemaidpersonaldimension.findSafeSurfaceY(targetDim, (int)targetX, (int)targetZ);
            } else {
                BlockPos spawn = targetDim.getSharedSpawnPos();
                targetX = (double)spawn.getX() + 0.5;
                targetY = spawn.getY() + 1;
                targetZ = (double)spawn.getZ() + 0.5;
            }
        }

        player.teleportTo(targetDim, targetX, targetY, targetZ, Set.of(), targetYRot, targetXRot);
        if (maidEntity != null) {
            Touhoulittlemaidpersonaldimension.TeleportLocation savedMaidPos = Touhoulittlemaidpersonaldimension.getEntityPosition(maidEntity.getUUID(), targetDim.dimension());
            
            if (savedMaidPos == null) {
                maidEntity.teleportTo(targetDim, targetX, targetY, targetZ, Set.of(), targetYRot, targetXRot);
            } else {
                maidEntity.teleportTo(targetDim, savedMaidPos.x(), savedMaidPos.y(), savedMaidPos.z(), Set.of(), savedMaidPos.yRot(), savedMaidPos.xRot());
            }
        }
    }
}
