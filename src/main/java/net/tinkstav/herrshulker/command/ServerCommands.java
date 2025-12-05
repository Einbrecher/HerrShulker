package net.tinkstav.herrshulker.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.tinkstav.herrshulker.HerrShulker;
import net.tinkstav.herrshulker.component.ModComponents;
import net.tinkstav.herrshulker.network.NetworkHandler;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ServerCommands {
    public static final String COMMAND_MRSHULKER = "mrshulker";
    public static final String SUB_COMMAND_SET = "set";

    public static final String SUB_COMMAND_QUERY = "query";
    public static final String SUB_COMMAND_RESET = "reset";
    public static final String SUB_COMMAND_ALLOW_DYEING = "allow_dyeing";
    public static final String SUB_COMMAND_ALLOW_PER_SHULKER_SCALING = "allow_per_shulker_scaling";
    public static final String SUB_COMMAND_CUSTOM_SCALE= "custom_scale";

    public static final String ARGUMENT_ALLOWANCE = "allowance";
    public static final String ARGUMENT_SCALE = "scale";
    public static void registerServerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
        var builder = Commands.literal(COMMAND_MRSHULKER)
                .then(Commands.literal(SUB_COMMAND_SET)
                    .requires(c -> Permissions.check(c, "mrshulker.set", 2))
                    .then(Commands.literal(SUB_COMMAND_ALLOW_DYEING)
                        .requires(c -> Permissions.check(c, "mrshulker.set.allow_dyeing", 2))
                        .then(Commands.argument(ARGUMENT_ALLOWANCE, BoolArgumentType.bool())
                            .executes(ServerCommands::setAllowDyeing)
                        )
                    )
                    .then(Commands.literal(SUB_COMMAND_ALLOW_PER_SHULKER_SCALING)
                        .requires(c -> Permissions.check(c, "mrshulker.set.allow_per_shulker_scaling", 2))
                        .then(Commands.argument(ARGUMENT_ALLOWANCE, BoolArgumentType.bool())
                            .executes(ServerCommands::setAllowCustomScaling)
                        )
                    )
                    .then(Commands.literal(SUB_COMMAND_CUSTOM_SCALE)
                        .requires(c -> Permissions.check(c, "mrshulker.set.custom_scale", 2))
                        .then(Commands.argument(ARGUMENT_SCALE, FloatArgumentType.floatArg(0.1F,3.0F))
                            .executes(ServerCommands::setCustomScale)
                        )
                    )
                )
                .then(Commands.literal(SUB_COMMAND_QUERY)
                    .requires(c -> Permissions.check(c, "mrshulker.query", 2))
                    .then(Commands.literal(SUB_COMMAND_ALLOW_DYEING)
                        .requires(c -> Permissions.check(c, "mrshulker.query.allow_dyeing", 2))
                        .executes(ServerCommands::queryAllowDyeing)
                    )
                    .then(Commands.literal(SUB_COMMAND_ALLOW_PER_SHULKER_SCALING)
                        .requires(c -> Permissions.check(c, "mrshulker.query.allow_per_shulker_scaling", 2))
                        .executes(ServerCommands::queryAllowCustomScaling)
                    )
                    .then(Commands.literal(SUB_COMMAND_CUSTOM_SCALE)
                        .requires(c -> Permissions.check(c, "mrshulker.query.custom_scale", 2))
                        .executes(ServerCommands::queryCustomScale)
                    )
                )
                .then(Commands.literal(SUB_COMMAND_RESET)
                    .requires(c -> Permissions.check(c, "mrshulker.reset", 2))
                    .then(Commands.literal(SUB_COMMAND_ALLOW_DYEING)
                        .requires(c -> Permissions.check(c, "mrshulker.reset.allow_dyeing", 2))
                        .executes(ServerCommands::resetAllowDyeing)
                    )
                    .then(Commands.literal(SUB_COMMAND_ALLOW_PER_SHULKER_SCALING)
                        .requires(c -> Permissions.check(c, "mrshulker.reset.allow_per_shulker_scaling", 2))
                        .executes(ServerCommands::resetAllowCustomScaling)
                    )
                    .then(Commands.literal(SUB_COMMAND_CUSTOM_SCALE)
                        .requires(c -> Permissions.check(c, "mrshulker.reset.custom_scale", 2))
                        .executes(ServerCommands::resetCustomScale)
                    )
                );
        dispatcher.register(builder);
    }
    private static int setAllowDyeing(CommandContext<CommandSourceStack> context) {
        boolean value = BoolArgumentType.getBool(context, ARGUMENT_ALLOWANCE);
        HerrShulker.Config.setAllowDyeing(value);
        HerrShulker.syncedAllowDyeing = value;
        NetworkHandler.broadcastConfigUpdate(context.getSource().getServer());
        context.getSource().sendSystemMessage(Component.literal("%s: %b".formatted(SUB_COMMAND_ALLOW_DYEING,HerrShulker.Config.allowDyeing)));
        return 1;
    }
    private static int queryAllowDyeing(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(Component.literal("%s: %b".formatted(SUB_COMMAND_ALLOW_DYEING,HerrShulker.Config.allowDyeing)));
        return 1;
    }
    private static int resetAllowDyeing(CommandContext<CommandSourceStack> commandContext) {
        HerrShulker.Config.resetAllowDyeing();
        HerrShulker.syncedAllowDyeing = HerrShulker.Config.isDyeingAllowed();
        NetworkHandler.broadcastConfigUpdate(commandContext.getSource().getServer());
        commandContext.getSource().sendSystemMessage(Component.literal("%s reset to: %b".formatted(SUB_COMMAND_ALLOW_DYEING, HerrShulker.Config.allowDyeing)));
        return 1;
    }
    private static int setAllowCustomScaling(CommandContext<CommandSourceStack> context) {
        boolean value = BoolArgumentType.getBool(context, ARGUMENT_ALLOWANCE);
        HerrShulker.Config.setAllowPerShulkerScaling(value);
        HerrShulker.syncedAllowPerShulkerScaling = value;
        NetworkHandler.broadcastConfigUpdate(context.getSource().getServer());
        context.getSource().sendSystemMessage(Component.literal("%s: %b".formatted(SUB_COMMAND_ALLOW_PER_SHULKER_SCALING,HerrShulker.Config.isPerShulkerScalingAllowed())));
        return 1;
    }
    private static int queryAllowCustomScaling(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(Component.literal("%s: %b".formatted(SUB_COMMAND_ALLOW_PER_SHULKER_SCALING,HerrShulker.Config.isPerShulkerScalingAllowed())));
        return 1;
    }
    private static int resetAllowCustomScaling(CommandContext<CommandSourceStack> commandContext) {
        HerrShulker.Config.resetAllowPerShulkerScaling();
        HerrShulker.syncedAllowPerShulkerScaling = HerrShulker.Config.isPerShulkerScalingAllowed();
        NetworkHandler.broadcastConfigUpdate(commandContext.getSource().getServer());
        commandContext.getSource().sendSystemMessage(Component.literal("%s reset to: %b".formatted(SUB_COMMAND_ALLOW_PER_SHULKER_SCALING, HerrShulker.Config.isPerShulkerScalingAllowed())));
        return 1;
    }

    private static int setCustomScale(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        if(!HerrShulker.Config.isPerShulkerScalingAllowed()){
            commandContext.getSource().sendFailure(Component.literal("Per Shulker scaling is not allowed on this server. Ask an admin to configure it."));
            return -1;
        }

        var customScale = FloatArgumentType.getFloat(commandContext,ARGUMENT_SCALE);
        var source = commandContext.getSource();
        if(!source.isPlayer()){
            source.sendFailure(Component.literal("Only players can set custom scaling."));
            return -1;
        }
        var player = source.getPlayerOrException();
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if(stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock){
            TypedEntityData<BlockEntityType<?>> blockComponent = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if(blockComponent != null && blockComponent.contains(ModComponents.LID_ITEM)){
                CompoundTag nbt = blockComponent.copyTagWithoutId();
                nbt.putFloat(ModComponents.LID_ITEM_CUSTOM_SCALE, customScale);
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockComponent.type(), nbt));
                return 1;
            }
            source.sendFailure(Component.literal("Custom scaling can only be applied to shulker boxes with lid items attached."));
            return -1;
        }
        source.sendSystemMessage(Component.literal("Must have a shulker box in main hand."));
        return -1;
    }
    private static int resetCustomScale(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        var source = commandContext.getSource();
        if(!source.isPlayer()){
            source.sendFailure(Component.literal("Only Players can set custom scaling."));
            return -1;
        }
        var player = source.getPlayerOrException();
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if(stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock){
            TypedEntityData<BlockEntityType<?>> blockComponent = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if(blockComponent != null && blockComponent.contains(ModComponents.LID_ITEM_CUSTOM_SCALE)){
                CompoundTag nbt = blockComponent.copyTagWithoutId();
                nbt.remove(ModComponents.LID_ITEM_CUSTOM_SCALE);
                stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockComponent.type(), nbt));
                source.sendSystemMessage(Component.literal("Custom Scaling removed."));
                return 1;
            }
            source.sendFailure(Component.literal("Custom Scaling is not applied to this shulker."));
            return -1;
        }
        source.sendFailure(Component.literal("Must have a shulker box in main hand."));
        return -1;
    }
    private static int queryCustomScale(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        var source = commandContext.getSource();
        var player = source.getPlayerOrException();
        var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if(stack.getItem() instanceof BlockItem block && block.getBlock() instanceof ShulkerBoxBlock){
            TypedEntityData<BlockEntityType<?>> blockComponent = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if(blockComponent != null) {
                var customScale = blockComponent.copyTagWithoutId().getFloat(ModComponents.LID_ITEM_CUSTOM_SCALE);
                customScale.ifPresentOrElse(
                        scale->source.sendSystemMessage(Component.literal("custom_scale: %f".formatted(scale))),
                        ()->source.sendSystemMessage(Component.literal("custom_scale not set."))
                );
            } else {
                source.sendSystemMessage(Component.literal("custom_scale not set."));
            }
            return 1;
        }
        source.sendFailure(Component.literal("Must have a shulker box in main hand."));
        return -1;
    }




}
