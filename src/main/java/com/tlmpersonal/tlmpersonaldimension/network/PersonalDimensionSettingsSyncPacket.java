package com.tlmpersonal.tlmpersonaldimension.network;

import com.tlmpersonal.tlmpersonaldimension.TouhoulittlemaidpersonaldimensionClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PersonalDimensionSettingsSyncPacket {

    private final CompoundTag settings;
    private final boolean allowCheatConfigs;

    public PersonalDimensionSettingsSyncPacket(CompoundTag settings, boolean allowCheatConfigs) {
        this.settings = settings;
        this.allowCheatConfigs = allowCheatConfigs;
    }

    public static void encode(PersonalDimensionSettingsSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.settings);
        buf.writeBoolean(msg.allowCheatConfigs);
    }

    public static PersonalDimensionSettingsSyncPacket decode(FriendlyByteBuf buf) {
        return new PersonalDimensionSettingsSyncPacket(buf.readNbt(), buf.readBoolean());
    }

    public static void handle(PersonalDimensionSettingsSyncPacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(message));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(PersonalDimensionSettingsSyncPacket message) {
        TouhoulittlemaidpersonaldimensionClient.handleSettingsSync(message.settings, message.allowCheatConfigs);
    }
}
