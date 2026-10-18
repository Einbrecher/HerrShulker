package net.tinkstav.herrshulker.mixin;

import net.tinkstav.herrshulker.IShulkerLidItem;
import net.tinkstav.herrshulker.HerrShulker;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class MixinDyeItem {

    @Inject(method = "useOn",
            at = @At("RETURN"),
            require = 1,
            cancellable = true)
    private void herrshulker$useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        // Only apply to DyeItem instances
        if (!((Object) this instanceof DyeItem dyeItem)) {
            return;
        }

        if (!HerrShulker.syncedAllowDyeing) {
            return;
        }

        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var state = level.getBlockState(pos);
        var targetBlock = state.getBlock();

        if (!(targetBlock instanceof ShulkerBoxBlock)) {
            return;
        }

        // On client, return SUCCESS to show arm swing animation
        if (level.isClientSide()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity targetShulker)) {
            return;
        }

        // 26.x: DyeItem.getDyeColor() removed — derive color by item identity
        DyeColor dyeColor = herrshulker$getDyeColorFromItem(dyeItem);
        if (dyeColor == null) {
            return;
        }

        if (targetShulker.getColor() == dyeColor) {
            cir.setReturnValue(InteractionResult.PASS);
            return;
        }
        Block block = herrshulker$getDyedShulker(dyeColor);
        level.setBlock(pos, block.defaultBlockState().setValue(ShulkerBoxBlock.FACING, state.getValue(ShulkerBoxBlock.FACING)), 2);

        if(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity newShulker){
            level.playLocalSound(player, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1, 0.95f);

            // Transfer inventory
            ((AccessorShulkerBoxBlockEntity) newShulker).setInventory(((AccessorShulkerBoxBlockEntity) targetShulker).getInventory());

            // Transfer lid item and custom scale
            if (newShulker instanceof IShulkerLidItem newLid) {
                var oldLid = IShulkerLidItem.from(targetShulker);
                oldLid.getLidItem().ifPresent(newLid::setLidItem);
                oldLid.getLidItemCustomScale().ifPresent(newLid::setLidItemCustomScale);
            }

            level.blockEntityChanged(pos);
        }

        // Only consume dye if not in creative mode
        if(player != null && !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }

        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    /**
     * 26.x: getDyeColor() was removed from DyeItem. Derive DyeColor by matching item identity.
     * This is the re-derived approach: DyeItem subclasses were merged into a single DyeItem class
     * with per-instance Items.* statics in 26.x, so we match item identity to DyeColor.
     */
    @Unique
    @Nullable
    private static DyeColor herrshulker$getDyeColorFromItem(DyeItem dyeItem) {
        Item item = (Item) (Object) dyeItem;
        if (item == Items.WHITE_DYE) return DyeColor.WHITE;
        if (item == Items.ORANGE_DYE) return DyeColor.ORANGE;
        if (item == Items.MAGENTA_DYE) return DyeColor.MAGENTA;
        if (item == Items.LIGHT_BLUE_DYE) return DyeColor.LIGHT_BLUE;
        if (item == Items.YELLOW_DYE) return DyeColor.YELLOW;
        if (item == Items.LIME_DYE) return DyeColor.LIME;
        if (item == Items.PINK_DYE) return DyeColor.PINK;
        if (item == Items.GRAY_DYE) return DyeColor.GRAY;
        if (item == Items.LIGHT_GRAY_DYE) return DyeColor.LIGHT_GRAY;
        if (item == Items.CYAN_DYE) return DyeColor.CYAN;
        if (item == Items.PURPLE_DYE) return DyeColor.PURPLE;
        if (item == Items.BLUE_DYE) return DyeColor.BLUE;
        if (item == Items.BROWN_DYE) return DyeColor.BROWN;
        if (item == Items.GREEN_DYE) return DyeColor.GREEN;
        if (item == Items.RED_DYE) return DyeColor.RED;
        if (item == Items.BLACK_DYE) return DyeColor.BLACK;
        return null;
    }

    @Unique
    private static @NotNull Block herrshulker$getDyedShulker(DyeColor color) {
        return switch (color) {
            case WHITE -> Blocks.WHITE_SHULKER_BOX;
            case ORANGE -> Blocks.ORANGE_SHULKER_BOX;
            case MAGENTA -> Blocks.MAGENTA_SHULKER_BOX;
            case LIGHT_BLUE -> Blocks.LIGHT_BLUE_SHULKER_BOX;
            case YELLOW -> Blocks.YELLOW_SHULKER_BOX;
            case LIME -> Blocks.LIME_SHULKER_BOX;
            case PINK -> Blocks.PINK_SHULKER_BOX;
            case GRAY -> Blocks.GRAY_SHULKER_BOX;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_SHULKER_BOX;
            case CYAN -> Blocks.CYAN_SHULKER_BOX;
            case PURPLE -> Blocks.PURPLE_SHULKER_BOX;
            case BLUE -> Blocks.BLUE_SHULKER_BOX;
            case BROWN -> Blocks.BROWN_SHULKER_BOX;
            case GREEN -> Blocks.GREEN_SHULKER_BOX;
            case RED -> Blocks.RED_SHULKER_BOX;
            case BLACK -> Blocks.BLACK_SHULKER_BOX;
        };
    }
}
