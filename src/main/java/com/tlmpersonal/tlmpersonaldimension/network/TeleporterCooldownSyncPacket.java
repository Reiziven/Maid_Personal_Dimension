package com.tlmpersonal.tlmpersonaldimension.network;

import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionMainGui;
import com.tlmpersonal.tlmpersonaldimension.client.screen.PersonalDimensionMaidScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record TeleporterCooldownSyncPacket(long cooldownEndMs) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TeleporterCooldownSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            Touhoulittlemaidpersonaldimension.id("teleporter_cooldown_sync")
    );

    public static final StreamCodec<ByteBuf, TeleporterCooldownSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, TeleporterCooldownSyncPacket::cooldownEndMs,
            TeleporterCooldownSyncPacket::new
    );

    public static void handle(TeleporterCooldownSyncPacket message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                if (Minecraft.getInstance().screen instanceof PersonalDimensionMainGui gui) {
                    gui.updateCooldown(message.cooldownEndMs());
                } else if (Minecraft.getInstance().screen instanceof PersonalDimensionMaidScreen gui) {
                    gui.updateCooldown(message.cooldownEndMs());
                }
            });
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
