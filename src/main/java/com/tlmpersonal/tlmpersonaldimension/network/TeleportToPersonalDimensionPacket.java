package com.tlmpersonal.tlmpersonaldimension.network;

import com.github.tartaricacid.touhoulittlemaid.capability.MaidNumCapabilityProvider;
import com.github.tartaricacid.touhoulittlemaid.capability.PowerCapabilityProvider;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncCapabilityMessage;
import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.PersonalDimensionSavedData;
import com.tlmpersonal.tlmpersonaldimension.PlayerDimensionManager;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.world.StructurePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public class TeleportToPersonalDimensionPacket {

    private final int maidId;
    private final boolean teleportWithMaid;

    public TeleportToPersonalDimensionPacket(int maidId, boolean teleportWithMaid) {
        this.maidId = maidId;
        this.teleportWithMaid = teleportWithMaid;
    }

    public static void encode(TeleportToPersonalDimensionPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.maidId);
        buf.writeBoolean(msg.teleportWithMaid);
    }

    public static TeleportToPersonalDimensionPacket decode(FriendlyByteBuf buf) {
        return new TeleportToPersonalDimensionPacket(buf.readInt(), buf.readBoolean());
    }

    public static void handle(TeleportToPersonalDimensionPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel currentLevel = player.serverLevel();
            Entity entity = currentLevel.getEntity(message.maidId);
            if (!(entity instanceof EntityMaid maid)) return;

            if (maid.getFavorabilityManager().getLevel() < 3) {
                player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.favorability_too_low"));
                return;
            }

            boolean isJoining = !Touhoulittlemaidpersonaldimension.isOurDimension(currentLevel.dimension());
            if (!checkCooldown(player, isJoining)) return;

            if (Config.TELEPORT_HAS_COST.get()) {
                double powerCost = Config.TELEPORT_COST_POWER_POINTS.get();
                int xpCost = Config.TELEPORT_COST_XP.get();

                final float[] powerRef = {0f};
                player.getCapability(PowerCapabilityProvider.POWER_CAP).ifPresent(cap -> powerRef[0] = cap.get());

                if (powerCost > 0.0 && powerRef[0] < (float) powerCost) {
                    player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_power"));
                    return;
                }
                if (xpCost > 0 && player.experienceLevel < xpCost) {
                    player.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_xp"));
                    return;
                }
                if (powerCost > 0.0) {
                    player.getCapability(PowerCapabilityProvider.POWER_CAP).ifPresent(cap -> cap.min((float) powerCost));
                    syncPowerToClient(player);
                }
                if (xpCost > 0) player.giveExperienceLevels(-xpCost);
            }

            ServerLevel targetDim;
            UUID ownerUUID = player.getUUID();

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
                if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim.dimension())
                        && !Touhoulittlemaidpersonaldimension.isPlayerAllowed(player, ownerUUID, targetDim, null)) {
                    player.sendSystemMessage(Component.literal("You are not allowed in this dimension!"));
                    return;
                }
                applyCooldown(player, isJoining);
                teleportPlayerAndMaid(player, maid, targetDim, message.teleportWithMaid);
            } else {
                player.sendSystemMessage(Component.literal("Personal dimension could not be loaded! Check server logs."));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void syncPowerToClient(ServerPlayer player) {
        final float[] pRef = {0f};
        final int[] mRef = {0};
        player.getCapability(PowerCapabilityProvider.POWER_CAP).ifPresent(c -> pRef[0] = c.get());
        player.getCapability(MaidNumCapabilityProvider.MAID_NUM_CAP).ifPresent(c -> mRef[0] = c.get());
        NetworkHandler.CHANNEL.sendTo(new SyncCapabilityMessage(pRef[0], mRef[0]),
                player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static boolean checkCooldown(ServerPlayer player, boolean isJoining) {
        int cooldownSec = isJoining ? Config.TELEPORT_JOIN_COOLDOWN.get() : Config.TELEPORT_LEAVE_COOLDOWN.get();
        if (cooldownSec <= 0) return true;
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(player.server.getLevel(Level.OVERWORLD));
        Long lastTeleport = isJoining ? savedData.getGlobalJoinCooldown(player.getUUID()) : savedData.getGlobalLeaveCooldown(player.getUUID());
        long now = System.currentTimeMillis();
        if (lastTeleport != null && (now - lastTeleport) < (long) cooldownSec * 1000) {
            long remaining = (cooldownSec * 1000 - (now - lastTeleport)) / 1000;
            player.sendSystemMessage(Component.literal("Teleportation on cooldown! (" + remaining + "s)"));
            return false;
        }
        return true;
    }

    private static void applyCooldown(ServerPlayer player, boolean isJoining) {
        int cooldownSec = isJoining ? Config.TELEPORT_JOIN_COOLDOWN.get() : Config.TELEPORT_LEAVE_COOLDOWN.get();
        if (cooldownSec > 0) {
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(player.server.getLevel(Level.OVERWORLD));
            if (isJoining) savedData.setGlobalJoinCooldown(player.getUUID(), System.currentTimeMillis());
            else savedData.setGlobalLeaveCooldown(player.getUUID(), System.currentTimeMillis());
        }
    }

    private static void teleportPlayerAndMaid(ServerPlayer player, EntityMaid maid, ServerLevel targetDim, boolean teleportWithMaid) {
        Entity maidEntity = teleportWithMaid ? maid : null;
        Touhoulittlemaidpersonaldimension.saveEntityPosition(player, player.level().dimension());
        if (maidEntity != null) Touhoulittlemaidpersonaldimension.saveEntityPosition(maidEntity, player.level().dimension());

        double targetX, targetY, targetZ;
        float targetYRot = player.getYRot(), targetXRot = player.getXRot();
        Touhoulittlemaidpersonaldimension.TeleportLocation savedPos =
                Touhoulittlemaidpersonaldimension.getEntityPosition(player.getUUID(), targetDim.dimension());

        if (savedPos != null) {
            targetX = savedPos.x(); targetY = savedPos.y(); targetZ = savedPos.z();
            targetYRot = savedPos.yRot(); targetXRot = savedPos.xRot();
        } else if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim.dimension())) {
            targetX = 0.5; targetZ = 0.5;
            StructurePlacer.tryPlaceStructure(targetDim);
            targetY = Touhoulittlemaidpersonaldimension.findSafeSurfaceY(targetDim, (int) targetX, (int) targetZ);
        } else {
            BlockPos spawn = targetDim.getSharedSpawnPos();
            targetX = spawn.getX() + 0.5; targetY = spawn.getY() + 1; targetZ = spawn.getZ() + 0.5;
        }

        player.teleportTo(targetDim, targetX, targetY, targetZ, Set.of(), targetYRot, targetXRot);
        if (maidEntity != null) {
            Touhoulittlemaidpersonaldimension.enqueueMaidTeleport(maidEntity.getUUID(), player.getUUID(), targetDim.dimension());
        }
    }
}
