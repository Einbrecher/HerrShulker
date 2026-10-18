package net.tinkstav.herrshulker.client;

import net.tinkstav.herrshulker.HerrShulker;
import net.tinkstav.herrshulker.client.config.ClientConfig;
import net.tinkstav.herrshulker.network.ConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;


/**
 * Client-side mod initializer for HerrShulker on MC 26.x.
 *
 * 26.x API delta: fabric-client-command-api-v2 is NOT available in fabric-api 0.148.2+26.1.2.
 * Client commands (shulker_display) are NOT registered on 26.x.
 * Client config management (getLidItemScale, etc.) still works via ClientConfig directly.
 * KNOWN LIMITATION: /shulker_display client commands unavailable on 26.x until fabric-client-command-api-v2
 * is available for this MC version.
 */
public class HerrShulkerClient implements ClientModInitializer {

    public static ClientConfig Config;

    @Override
    public void onInitializeClient() {
        HerrShulker.LOGGER.info("HerrShulkerClient.onInitializeClient() start");

        // Load client config
        Config = ClientConfig.load();

        // Register config sync packet receiver
        registerConfigSyncReceiver();

        HerrShulker.LOGGER.info("HerrShulkerClient.onInitializeClient() finish");
        HerrShulker.LOGGER.warn("26.x: Client commands (shulker_display) are not available — " +
                "fabric-client-command-api-v2 not present in fabric-api 0.148.2+26.1.2");
    }

    /**
     * Registers the client-side receiver for config sync packets from the server.
     */
    private void registerConfigSyncReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
            // Update the synced config values in the common HerrShulker class
            HerrShulker.syncedAllowDyeing = payload.allowDyeing();
            HerrShulker.syncedAllowPerShulkerScaling = payload.allowPerShulkerScaling();
            HerrShulker.LOGGER.debug("Received config sync: allowDyeing={}, allowPerShulkerScaling={}",
                    payload.allowDyeing(), payload.allowPerShulkerScaling());
        });
        HerrShulker.LOGGER.info("Registered config sync receiver");
    }
}
