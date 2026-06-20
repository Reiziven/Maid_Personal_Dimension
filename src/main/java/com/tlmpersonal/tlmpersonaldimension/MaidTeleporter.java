package com.tlmpersonal.tlmpersonaldimension;

import com.tlmpersonal.tlmpersonaldimension.world.StructurePlacer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MaidTeleporter extends Item {
    private static final String TAG_OWNER_UUID = "owner_uuid";
    private static final String TAG_OWNER_NAME = "owner_name";
    private static final String TAG_GUI_MODE = "gui_mode";
    
    public MaidTeleporter(Properties properties) {
        super(properties);
    }
    
    public static UUID getOwnerUUID(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.hasUUID(TAG_OWNER_UUID)) {
                return tag.getUUID(TAG_OWNER_UUID);
            }
        }
        return null;
    }

    public static String getOwnerName(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(TAG_OWNER_NAME)) {
                return tag.getString(TAG_OWNER_NAME);
            }
        }
        return null;
    }
    
    public static void setOwnerUUID(ItemStack stack, UUID uuid) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID(TAG_OWNER_UUID, uuid);
        });
    }

    public static void setOwnerInfo(ItemStack stack, UUID uuid, String name) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID(TAG_OWNER_UUID, uuid);
            tag.putString(TAG_OWNER_NAME, name);
        });
    }
    
    public static boolean isGuiMode(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getBoolean(TAG_GUI_MODE);
        }
        return false;
    }
    
    public static void setGuiMode(ItemStack stack, boolean guiMode) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putBoolean(TAG_GUI_MODE, guiMode);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String ownerName = getOwnerName(stack);
        if (ownerName != null) {
            tooltip.add(Component.translatable("tooltip.tlmpersonaldimension.owner", ownerName).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            UUID ownerUUID = getOwnerUUID(player.getItemInHand(hand));
            if (ownerUUID == null) {
                ownerUUID = serverPlayer.getUUID();
                setOwnerInfo(player.getItemInHand(hand), ownerUUID, serverPlayer.getGameProfile().getName());
            }

            boolean isJoining = !Touhoulittlemaidpersonaldimension.isOurDimension(level.dimension());
            if (!checkCooldown(serverPlayer, isJoining)) {
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }

            if (Config.TELEPORT_HAS_COST.get()) {
                double powerCost = Config.TELEPORT_COST_POWER_POINTS.get(); 
                int xpCost = Config.TELEPORT_COST_XP.get();
                
                if (xpCost > 0 && serverPlayer.experienceLevel < xpCost) { 
                    serverPlayer.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_xp"));
                    return InteractionResultHolder.fail(player.getItemInHand(hand));
                }
                
                if (xpCost > 0) {
                    serverPlayer.giveExperienceLevels(-xpCost); 
                }
            }
            
            ServerLevel targetDim;
            if (Touhoulittlemaidpersonaldimension.isOurDimension(level.dimension())) {
                targetDim = serverPlayer.server.getLevel(Level.OVERWORLD);
            } else {
                targetDim = PlayerDimensionManager.getOrCreatePlayerDimensionSync(serverPlayer.server, ownerUUID);
            }

            if (targetDim != null) {
                if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim.dimension())) {
                    if (!Touhoulittlemaidpersonaldimension.isPlayerAllowed(serverPlayer, ownerUUID, targetDim, null)) {
                        serverPlayer.sendSystemMessage(Component.literal("You are not allowed in this dimension!"));
                        return InteractionResultHolder.fail(player.getItemInHand(hand));
                    }
                }
                
                applyCooldown(serverPlayer, isJoining);
                teleportEntity(serverPlayer, targetDim, serverPlayer);
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
        }
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!Config.ALLOW_ENTITY_TELEPORT.get()) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            UUID ownerUUID = getOwnerUUID(stack);
            if (ownerUUID == null) {
                ownerUUID = serverPlayer.getUUID();
                setOwnerInfo(stack, ownerUUID, serverPlayer.getGameProfile().getName());
            }

            boolean isJoining = !Touhoulittlemaidpersonaldimension.isOurDimension(target.level().dimension());
            if (!checkCooldown(serverPlayer, isJoining)) {
                return InteractionResult.FAIL;
            }

            if (Config.TELEPORT_HAS_COST.get()) {
                int xpCost = Config.TELEPORT_COST_XP.get();
                if (xpCost > 0 && serverPlayer.experienceLevel < xpCost) { 
                    serverPlayer.sendSystemMessage(Component.translatable("message.tlmpersonaldimension.not_enough_xp"));
                    return InteractionResult.FAIL;
                }
                if (xpCost > 0) {
                    serverPlayer.giveExperienceLevels(-xpCost); 
                }
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
            PersonalDimensionSavedData savedData = PersonalDimensionSavedData.get(player.serverLevel());
            if (isJoining) savedData.setGlobalJoinCooldown(player.getUUID(), System.currentTimeMillis());
            else savedData.setGlobalLeaveCooldown(player.getUUID(), System.currentTimeMillis());
        }
    }

    private void teleportEntity(Entity entity, ServerLevel targetLevel, Player sourcePlayer) {
        UUID entityUuid = entity.getUUID();
        ResourceKey<Level> currentDim = entity.level().dimension();
        ResourceKey<Level> targetDim = targetLevel.dimension();

        Touhoulittlemaidpersonaldimension.saveEntityPosition(entity, currentDim);

        double targetX, targetY, targetZ;
        float targetYRot = entity.getYRot();
        float targetXRot = entity.getXRot();

        Touhoulittlemaidpersonaldimension.TeleportLocation savedTargetPos = Touhoulittlemaidpersonaldimension.getEntityPosition(entityUuid, targetDim);
        
        // Fallback for non-players: use source player's saved position if entity has none
        if (savedTargetPos == null && !(entity instanceof Player)) {
            savedTargetPos = Touhoulittlemaidpersonaldimension.getEntityPosition(sourcePlayer.getUUID(), targetDim);
        }

        if (savedTargetPos != null) {
            targetX = savedTargetPos.x();
            targetY = savedTargetPos.y();
            targetZ = savedTargetPos.z();
            targetYRot = savedTargetPos.yRot();
            targetXRot = savedTargetPos.xRot();
        } else {
            if (Touhoulittlemaidpersonaldimension.isOurDimension(targetDim)) {
                targetX = 0.5;
                targetZ = 0.5;
                StructurePlacer.tryPlaceStructure(targetLevel);
                targetY = Touhoulittlemaidpersonaldimension.findSafeSurfaceY(targetLevel, (int)targetX, (int)targetZ);
            } else {
                BlockPos spawnPos = targetLevel.getSharedSpawnPos();
                BlockPos safePos = findSafeSpot(targetLevel, spawnPos);
                targetX = safePos.getX() + 0.5;
                targetY = safePos.getY();
                targetZ = safePos.getZ() + 0.5;
            }
        }

        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), targetYRot, targetXRot);
        } else {
            entity.teleportTo(targetLevel, targetX, targetY, targetZ, Set.of(), targetYRot, targetXRot);
        }
    }

    private BlockPos findSafeSpot(ServerLevel level, BlockPos startPos) {
        if (Touhoulittlemaidpersonaldimension.isOurDimension(level.dimension())) {
            int safeY = Touhoulittlemaidpersonaldimension.findSafeSurfaceY(level, startPos.getX(), startPos.getZ());
            return new BlockPos(startPos.getX(), safeY, startPos.getZ());
        }

        BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, startPos);
        if (isSafeSpot(level, groundPos.above())) {
            return groundPos.above();
        }

        for (int y = groundPos.getY(); y <= level.getMaxBuildHeight(); y++) {
            BlockPos check = new BlockPos(startPos.getX(), y, startPos.getZ());
            if (isSafeSpot(level, check)) {
                return check;
            }
        }

        for (int y = groundPos.getY(); y >= level.getMinBuildHeight(); y--) {
            BlockPos check = new BlockPos(startPos.getX(), y, startPos.getZ());
            if (isSafeSpot(level, check)) {
                return check;
            }
        }

        BlockPos fallback = new BlockPos(startPos.getX(), 64, startPos.getZ());
        level.setBlockAndUpdate(fallback, Blocks.COBBLESTONE.defaultBlockState());
        return fallback.above();
    }

    private boolean isSafeSpot(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() &&
               level.getBlockState(pos.above()).isAir() &&
               level.getBlockState(pos.below()).blocksMotion();
    }
}
