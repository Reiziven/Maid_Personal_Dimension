package com.tlmpersonal.tlmpersonaldimension.network;

import com.tlmpersonal.tlmpersonaldimension.Config;
import com.tlmpersonal.tlmpersonaldimension.Touhoulittlemaidpersonaldimension;
import com.tlmpersonal.tlmpersonaldimension.TouhoulittlemaidpersonaldimensionClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PersonalDimensionSettingsSyncPacket(CompoundTag settings, boolean allowCheatConfigs) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PersonalDimensionSettingsSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            Touhoulittlemaidpersonaldimension.id("personal_dimension_settings_sync")
    );

    public static final StreamCodec<ByteBuf, PersonalDimensionSettingsSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, PersonalDimensionSettingsSyncPacket::settings,
            ByteBufCodecs.BOOL, PersonalDimensionSettingsSyncPacket::allowCheatConfigs,
            PersonalDimensionSettingsSyncPacket::new
    );

    public static void handle(PersonalDimensionSettingsSyncPacket message, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            context.enqueueWork(() -> {
                TouhoulittlemaidpersonaldimensionClient.handleSettingsSync(message.settings(), message.allowCheatConfigs());
            });
        }
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
