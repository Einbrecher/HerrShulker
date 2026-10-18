package net.tinkstav.herrshulker.client;

import com.mojang.brigadier.CommandDispatcher;
import net.tinkstav.herrshulker.HerrShulker;
import net.tinkstav.herrshulker.client.command.ClientCommands;
import net.tinkstav.herrshulker.client.config.ClientConfig;
import net.tinkstav.herrshulker.network.ConfigSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.commands.CommandBuildContext;


public class HerrShulkerClient implements ClientModInitializer {

    public static ClientConfig Config;

    @Override
    public void onInitializeClient() {
        HerrShulker.LOGGER.info("HerrShulkerClient.onInitializeClient() start");

        // Load client config
        Config = ClientConfig.load();

        // Register client commands
        ClientCommandRegistrationCallback.EVENT.register(HerrShulkerClient::registerClientCommands);

        // Register config sync packet receiver
        registerConfigSyncReceiver();

        HerrShulker.LOGGER.info("HerrShulkerClient.onInitializeClient() finish");
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

    private static void registerClientCommands(CommandDispatcher<FabricClientCommandSource> fabricClientCommandSourceCommandDispatcher, CommandBuildContext commandBuildContext) {
        HerrShulker.LOGGER.info("registering client commands");
        ClientCommands.registerClientCommands(fabricClientCommandSourceCommandDispatcher, commandBuildContext);
        HerrShulker.LOGGER.info("client command registration complete.");
    }
}
