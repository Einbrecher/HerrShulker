package net.tinkstav.herrshulker.network;

import net.tinkstav.herrshulker.HerrShulker;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;

/**
 * Handles network registration and server-side packet sending for config sync.
 */
public class NetworkHandler {

    /**
     * Registers the payload type. Must be called during mod initialization.
     */
    public static void register() {
        // Register S2C (server-to-client) payload type
        PayloadTypeRegistry.playS2C().register(
                ConfigSyncPayload.TYPE,
                ConfigSyncPayload.STREAM_CODEC
        );
        HerrShulker.LOGGER.info("Registered config sync payload");
    }

    /**
     * Registers server-side events. Must be called during mod initialization.
     */
    public static void registerServerEvents() {
        // Send config to player on join (only if they have the mod installed)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (ServerPlayNetworking.canSend(handler.getPlayer(), ConfigSyncPayload.TYPE)) {
                sender.sendPacket(createConfigPayload());
                HerrShulker.LOGGER.debug("Sent config sync to {}", handler.getPlayer().getName().getString());
            } else {
                HerrShulker.LOGGER.debug("Client {} does not have HerrShulker installed, skipping config sync",
                        handler.getPlayer().getName().getString());
            }
        });
    }

    /**
     * Creates a config sync payload from current server config.
     */
    public static ConfigSyncPayload createConfigPayload() {
        return new ConfigSyncPayload(
                HerrShulker.Config.isDyeingAllowed(),
                HerrShulker.Config.isPerShulkerScalingAllowed()
        );
    }

    /**
     * Broadcasts current config to all connected players who have the mod installed.
     * Call this when config is changed via commands.
     */
    public static void broadcastConfigUpdate(MinecraftServer server) {
        if (server == null) {
            return;
        }
        var payload = createConfigPayload();
        int sentCount = 0;
        for (var player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, ConfigSyncPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
                sentCount++;
            }
        }
        HerrShulker.LOGGER.debug("Broadcast config update to {} players with mod installed", sentCount);
    }
}
