package com.tlmpersonal.tlmpersonaldimension.network;

import com.tlmpersonal.tlmpersonaldimension.client.screen.PersonalDimensionMaidScreen;
import com.tlmpersonal.tlmpersonaldimension.client.gui.PersonalDimensionMainGui;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TeleporterCooldownSyncPacket {

    private final long cooldownEndMs;

    public TeleporterCooldownSyncPacket(long cooldownEndMs) {
        this.cooldownEndMs = cooldownEndMs;
    }

    public static void encode(TeleporterCooldownSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeVarLong(msg.cooldownEndMs);
    }

    public static TeleporterCooldownSyncPacket decode(FriendlyByteBuf buf) {
        return new TeleporterCooldownSyncPacket(buf.readVarLong());
    }

    public static void handle(TeleporterCooldownSyncPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(message));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(TeleporterCooldownSyncPacket message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PersonalDimensionMainGui gui) {
            gui.updateCooldown(message.cooldownEndMs);
        } else if (mc.screen instanceof PersonalDimensionMaidScreen gui) {
            gui.updateCooldown(message.cooldownEndMs);
        }
    }
}
