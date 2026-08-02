package com.tlmpersonal.tlmpersonaldimension;

import com.tlmpersonal.tlmpersonaldimension.world.StructurePlacer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MaidTeleporter extends Item {
    private static final String TAG_OWNER_UUID = "owner_uuid";
    private static final String TAG_OWNER_NAME = "owner_name";

    public MaidTeleporter(Properties properties) {
        super(properties);
    }

    public static @Nullable UUID getOwnerUUID(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.hasUUID(TAG_OWNER_UUID)) return tag.getUUID(TAG_OWNER_UUID);
        return null;
    }

    public static @Nullable String getOwnerName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_OWNER_NAME)) return tag.getString(TAG_OWNER_NAME);
        return null;
    }

    public static void setOwnerInfo(ItemStack stack, UUID uuid, String name) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_OWNER_UUID, uuid);
        tag.putString(TAG_OWNER_NAME, name);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        String ownerName = getOwnerName(stack);
        if (ownerName != null) {
            tooltip.add(Component.translatable("tooltip.tlmpersonaldimension.owner", ownerName).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, world, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(hand));
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            UUID ownerUUID = getOwnerUUID(stack);
            if (ownerUUID == null) {
                ownerUUID = serverPlayer.getUUID();
                setOwnerInfo(stack, ownerUUID, serverPlayer.getGameProfile().getName());
            }
            boolean isJoining = !Touhoulittlemaidpersonaldimension.isOurDimension(level.dimension());
            if (!checkCooldown(serverPlayer, isJoining)) return InteractionResultHolder.fail(stack);
            if (Config.TELEPORT_HAS_COST.get()) {
                int xpCost = Config.TELEPORT_COST_XP.get();
                if (xpCost > 0 && serverPlayer.experienceLevel < xpCost) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_xp"));
                    return InteractionResultHolder.fail(stack);
                }
                if (xpCost > 0) serverPlayer.giveExperienceLevels(-xpCost);
            }
            ServerLevel targetDim;
            if (Touhoulittlemaidpersonaldimension.isOurDimension(level.dimension())) {
                targetDim = serverPlayer.server.getLevel(Level.OVERWORLD);
            } else {
                targetDim = PlayerDimensionManager.getOrCreatePlayerDimensionSync(serverPlayer.server, ownerUUID);
            }
            if (targetDim != null) {
                if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim.dimension())
                        && !Touhoulittlemaidpersonaldimension.isPlayerAllowed(serverPlayer, ownerUUID, targetDim, null)) {
                    serverPlayer.sendSystemMessage(Component.literal("You are not allowed in this dimension!"));
                    return InteractionResultHolder.fail(stack);
                }
                applyCooldown(serverPlayer, isJoining);
                teleportEntity(serverPlayer, targetDim, serverPlayer);
                return InteractionResultHolder.success(stack);
            } else {
                serverPlayer.sendSystemMessage(Component.literal("Personal dimension could not be loaded! Check server logs."));
            }
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!Config.ALLOW_ENTITY_TELEPORT.get()) return InteractionResult.PASS;
        if (!player.level().isClientSide && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            UUID ownerUUID = getOwnerUUID(stack);
            if (ownerUUID == null) {
                ownerUUID = serverPlayer.getUUID();
                setOwnerInfo(stack, ownerUUID, serverPlayer.getGameProfile().getName());
            }
            if (!Touhoulittlemaidpersonaldimension.isMaidTeleporterAllowed(target)) {
                serverPlayer.sendSystemMessage(Component.literal("This entity cannot be teleported with the maid teleporter!"));
                return InteractionResult.FAIL;
            }
            boolean isJoining = !Touhoulittlemaidpersonaldimension.isOurDimension(target.level().dimension());
            if (!checkCooldown(serverPlayer, isJoining)) return InteractionResult.FAIL;
            if (Config.TELEPORT_HAS_COST.get()) {
                int xpCost = Config.TELEPORT_COST_XP.get();
                if (xpCost > 0 && serverPlayer.experienceLevel < xpCost) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_xp"));
                    return InteractionResult.FAIL;
                }
                if (xpCost > 0) serverPlayer.giveExperienceLevels(-xpCost);
            }
            ServerLevel targetDim;
            if (Touhoulittlemaidpersonaldimension.isOurDimension(target.level().dimension())) {
                targetDim = serverPlayer.server.getLevel(Level.OVERWORLD);
            } else {
                targetDim = PlayerDimensionManager.getOrCreatePlayerDimensionSync(serverPlayer.server, ownerUUID);
            }
            if (targetDim != null) {
                if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim.dimension())) {
                    if (!Touhoulittlemaidpersonaldimension.isAllowed(target, ownerUUID, targetDim, null)) {
                        serverPlayer.sendSystemMessage(Component.literal("This entity is not allowed in the personal dimension!"));
                        return InteractionResult.FAIL;
                    }
                    if (!Touhoulittlemaidpersonaldimension.isPlayerAllowed(serverPlayer, ownerUUID, targetDim, null)) {
                        serverPlayer.sendSystemMessage(Component.literal("You are not allowed to send entities to this dimension!"));
                        return InteractionResult.FAIL;
                    }
                }
                applyCooldown(serverPlayer, isJoining);
                teleportEntity(target, targetDim, serverPlayer);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private boolean checkCooldown(ServerPlayer player, boolean isJoining) {
        int cooldownSec = isJoining ? Config.TELEPORT_JOIN_COOLDOWN.get() : Config.TELEPORT_LEAVE_COOLDOWN.get();
        if (cooldownSec <= 0) return true;
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(player.serverLevel());
        Long last = isJoining ? savedData.getGlobalJoinCooldown(player.getUUID()) : savedData.getGlobalLeaveCooldown(player.getUUID());
        long now = System.currentTimeMillis();
        if (last != null && (now - last) < (long) cooldownSec * 1000) {
            long remaining = (cooldownSec * 1000 - (now - last)) / 1000;
            player.sendSystemMessage(Component.literal("Teleportation on cooldown! (" + remaining + "s)"));
            return false;
        }
        return true;
    }

    private void applyCooldown(ServerPlayer player, boolean isJoining) {
        int cooldownSec = isJoining ? Config.TELEPORT_JOIN_COOLDOWN.get() : Config.TELEPORT_LEAVE_COOLDOWN.get();
        if (cooldownSec <= 0) return;
        PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(player.serverLevel());
        if (isJoining) savedData.setGlobalJoinCooldown(player.getUUID(), System.currentTimeMillis());
        else savedData.setGlobalLeaveCooldown(player.getUUID(), System.currentTimeMillis());
    }

    private void teleportEntity(Entity entity, ServerLevel targetLevel, Player sourcePlayer) {
        ResourceKey<Level> currentDim = entity.level().dimension();
        ResourceKey<Level> targetDim = targetLevel.dimension();
        Touhoulittlemaidpersonaldimension.saveEntityPosition(entity, currentDim);
        double targetX, targetY, targetZ;
        float targetYRot = entity.getYRot(), targetXRot = entity.getXRot();
        Touhoulittlemaidpersonaldimension.TeleportLocation savedPos =
                Touhoulittlemaidpersonaldimension.getEntityPosition(entity.getUUID(), targetDim);
        if (savedPos == null && !(entity instanceof Player))
            savedPos = Touhoulittlemaidpersonaldimension.getEntityPosition(sourcePlayer.getUUID(), targetDim);
        if (savedPos != null) {
            targetX = savedPos.x(); targetY = savedPos.y(); targetZ = savedPos.z();
            targetYRot = savedPos.yRot(); targetXRot = savedPos.xRot();
        } else if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim)) {
            targetX = 0.5; targetZ = 0.5;
            StructurePlacer.tryPlaceStructure(targetLevel);
            targetY = Touhoulittlemaidpersonaldimension.findSafeSurfaceY(targetLevel, 0, 0);
        } else {
            BlockPos spawn = targetLevel.getSharedSpawnPos();
            BlockPos safe = findSafeSpot(targetLevel, spawn);
            targetX = safe.getX() + 0.5; targetY = safe.getY(); targetZ = safe.getZ() + 0.5;
        }
        if (entity instanceof ServerPlayer sp) sp.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), targetYRot, targetXRot);
        else entity.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), targetYRot, targetXRot);
    }

    private BlockPos findSafeSpot(ServerLevel level, BlockPos start) {
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start);
        if (isSafe(level, ground.above())) return ground.above();
        for (int y = ground.getY(); y <= level.getMaxBuildHeight(); y++) {
            BlockPos c = new BlockPos(start.getX(), y, start.getZ());
            if (isSafe(level, c)) return c;
        }
        BlockPos fallback = new BlockPos(start.getX(), 64, start.getZ());
        level.setBlockAndUpdate(fallback, Blocks.COBBLESTONE.defaultBlockState());
        return fallback.above();
    }

    private boolean isSafe(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).blocksMotion();
    }
}
