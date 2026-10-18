package net.tinkstav.herrshulker.network;

import net.tinkstav.herrshulker.HerrShulker;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload for syncing server config to clients.
 * Sent on player join and when config changes via commands.
 */
public record ConfigSyncPayload(
        boolean allowDyeing,
        boolean allowPerShulkerScaling
) implements CustomPacketPayload {

    public static final Type<ConfigSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HerrShulker.MOD_ID, "config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ConfigSyncPayload::allowDyeing,
                    ByteBufCodecs.BOOL, ConfigSyncPayload::allowPerShulkerScaling,
                    ConfigSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
