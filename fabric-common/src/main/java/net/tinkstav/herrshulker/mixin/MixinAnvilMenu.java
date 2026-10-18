package net.tinkstav.herrshulker.mixin;


import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.Nullable;
import net.tinkstav.herrshulker.component.ModComponents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class MixinAnvilMenu extends ItemCombinerMenu {
    @Shadow @Nullable
    private String itemName;

    @Shadow @Final
    private DataSlot cost;

    @Shadow
    private int repairItemCountCost;

    public MixinAnvilMenu(@Nullable MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess context, ItemCombinerMenuSlotDefinition forgingSlotsManager) {
        super(type, syncId, playerInventory, context, forgingSlotsManager);
    }

    @Inject(
            method = {"createResult"},
            at = {@At("HEAD")},
            cancellable = true
    )
    public void createResult(CallbackInfo ci) {
        ItemStack input = this.inputSlots.getItem(0);
        if (this.isItemAShulker(input)) {
            boolean resultModified = false;
            ItemStack lidStack = this.inputSlots.getItem(1).copy();
            ItemStack result = input.copy();
            TypedEntityData<BlockEntityType<?>> blockComponent = result.get(DataComponents.BLOCK_ENTITY_DATA);
            var nameIsChanging = isNameChanging(result);
            if (lidStack.isEmpty()) {
                // If lidstack is empty, and name has not changed, remove liditem
                if (!nameIsChanging) {
                    if (blockComponent != null && (blockComponent.contains(ModComponents.LID_ITEM) || blockComponent.contains(ModComponents.COMPAT_DISPLAY))) {
                        CompoundTag nbt = blockComponent.copyTagWithoutId();
                        nbt.remove(ModComponents.LID_ITEM);
                        nbt.remove(ModComponents.COMPAT_DISPLAY);
                        nbt.remove(ModComponents.LID_ITEM_CUSTOM_SCALE);

                        if (nbt.isEmpty()) {
                            result.remove(DataComponents.BLOCK_ENTITY_DATA);
                        } else {
                            // Preserve existing block entity type for mod compatibility
                            result.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockComponent.type(), nbt));
                        }
                        resultModified = true;
                    }
                } else {
                    // If lidstack is empty, and name has changed, only change name
                    updateResultName(result);
                    resultModified = true;
                }
            }
            // If lidstack has value, set liditem
            else {
                if (lidStack.has(DataComponents.CONTAINER)) {
                    lidStack.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
                }
                this.access.execute((world, blockPosition) -> {
                    CompoundTag nbt = blockComponent != null ? blockComponent.copyTagWithoutId() : new CompoundTag();
                    ProblemReporter.Collector problemReporter = new ProblemReporter.Collector();
                    var output = TagValueOutput.createWithContext(problemReporter, world.registryAccess());
                    // REMOVED: output.putString("id", "minecraft:shulker_box");
                    // The block entity type is preserved from existing data or defaults to vanilla
                    output.store(ModComponents.LID_ITEM, ItemStack.CODEC, lidStack);
                    nbt.merge(output.buildResult());

                    // Preserve existing block entity type for mod compatibility
                    BlockEntityType<?> entityType = blockComponent != null
                            ? blockComponent.type()
                            : BlockEntityType.SHULKER_BOX;
                    result.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(entityType, nbt));
                });
                if (nameIsChanging) {
                    updateResultName(result);
                }
                resultModified = true;
            }
            if (resultModified) {
                this.resultSlots.setItem(0, result);
                this.cost.set(1);
                this.repairItemCountCost = 1;
            }
            this.broadcastChanges();
            ci.cancel();
        }
    }

    @Inject(
            method = {"onTake"},
            at = {@At("HEAD")}
    )
    public void onTake(CallbackInfo ci) {
        if (this.isItemAShulker(this.inputSlots.getItem(0))) {
            this.inputSlots.getItem(1).grow(1);
        }
    }

    @Unique
    private void updateResultName(ItemStack result) {
        if (result.has(DataComponents.CUSTOM_NAME) &&
                (StringUtil.isBlank(this.itemName)
                        || result.getHoverName().getString().equals(result.getItemName().getString()))
        ) {
            result.remove(DataComponents.CUSTOM_NAME);
        } else if (this.itemName != null
                && !StringUtil.isBlank(this.itemName)
                && !this.itemName.equals(result.getHoverName().getString())) {
            result.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
        }
    }

    @Unique
    private boolean isNameChanging(ItemStack target) {
        var hoverName = target.getHoverName().getString();
        var hasCustomName = target.has(DataComponents.CUSTOM_NAME);
        var isBlank = StringUtil.isBlank(this.itemName);
        return (hasCustomName && isBlank)
                || (this.itemName != null && !isBlank && !this.itemName.equals(hoverName));
    }

    @Unique
    private boolean isItemAShulker(ItemStack candidate) {
        Item firstItem = candidate.getItem();
        if (firstItem instanceof BlockItem itemBlock) {
            return itemBlock.getBlock() instanceof ShulkerBoxBlock;
        }
        return false;
    }
}
