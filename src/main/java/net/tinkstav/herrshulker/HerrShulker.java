package net.tinkstav.herrshulker;

import com.mojang.brigadier.CommandDispatcher;
import net.tinkstav.herrshulker.command.ServerCommands;
import net.tinkstav.herrshulker.config.ServerConfig;
import net.tinkstav.herrshulker.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HerrShulker implements ModInitializer {

    public static final String MOD_ID = "mrshulker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static ServerConfig Config;

    /**
     * Synced config values - these hold the EFFECTIVE config:
     * - On server: always equals Config values
     * - On client: synced from server, or defaults in singleplayer
     *
     * Common code (like MixinDyeItem) should read from these fields
     * rather than Config directly to ensure client-side prediction
     * matches server-side enforcement.
     */
    public static boolean syncedAllowDyeing = true;
    public static boolean syncedAllowPerShulkerScaling = true;

    @Override
    public void onInitialize() {
        HerrShulker.LOGGER.info("HerrShulker.onInitialize() start");

        // Load config
        Config = ServerConfig.load();

        // Initialize synced values from config (for singleplayer / server side)
        syncedAllowDyeing = Config.isDyeingAllowed();
        syncedAllowPerShulkerScaling = Config.isPerShulkerScalingAllowed();

        // Register networking
        NetworkHandler.register();
        NetworkHandler.registerServerEvents();

        // Register commands
        CommandRegistrationCallback.EVENT.register(HerrShulker::registerServerCommands);

        HerrShulker.LOGGER.info("HerrShulker.onInitialize() finish");
    }

    private static void registerServerCommands(CommandDispatcher<CommandSourceStack> commandSourceStackCommandDispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection commandSelection) {
        HerrShulker.LOGGER.info("registering stuff.");
        ServerCommands.registerServerCommands(commandSourceStackCommandDispatcher, commandBuildContext, commandSelection);
    }
}
