package net.tinkstav.herrshulker.client;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.TagValueInput;
import net.tinkstav.herrshulker.component.ModComponents;

import java.util.Optional;

/**
 * Decodes the HerrShulker "lid item" from a shulker-box ItemStack's BLOCK_ENTITY_DATA component.
 *
 * Used by the GUI/inventory/hand item-render path, where (unlike the world block-entity path)
 * there is no live BlockEntity to read from — the lid item must be parsed out of the stack's
 * serialized block-entity NBT. This mirrors the decode the special renderer used previously,
 * extracted so the render-state build path (SpecialModelWrapper.update) can reuse it.
 */
public final class GuiLidItemReader {
    private GuiLidItemReader() {}

    public static Optional<ItemStack> readLidItem(ItemStack stack, Level level) {
        if (stack == null || level == null) {
            return Optional.empty();
        }
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ShulkerBoxBlock)) {
            return Optional.empty();
        }
        TypedEntityData<BlockEntityType<?>> blockComponent = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockComponent == null) {
            return Optional.empty();
        }
        RegistryAccess registryAccess = level.registryAccess();
        CompoundTag tag = blockComponent.copyTagWithoutId();
        var problemReporter = new ProblemReporter.Collector();
        var valueInput = TagValueInput.create(problemReporter, registryAccess, tag);
        Optional<ItemStack> lid = valueInput.read(ModComponents.LID_ITEM, ItemStack.CODEC);
        if (lid.isEmpty()) {
            lid = valueInput.read(ModComponents.COMPAT_DISPLAY, ItemStack.CODEC);
        }
        return lid;
    }
}
