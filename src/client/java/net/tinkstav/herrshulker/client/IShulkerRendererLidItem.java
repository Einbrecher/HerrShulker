package net.tinkstav.herrshulker.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Interface mixed into ShulkerBoxSpecialRenderer for lid item rendering support.
 * Provides access to the current ItemStack and per-layer render state management.
 */
public interface IShulkerRendererLidItem {
    TypedEntityData<BlockEntityType<?>> getBlockEntityData();

    ItemStack getStack();

    void setStack(ItemStack stack);

    /**
     * Sets the ItemStackRenderState to use for rendering the lid item.
     * This should be called per-layer to avoid state corruption when
     * multiple shulker boxes render in the same frame.
     */
    void setLidItemRenderState(ItemStackRenderState renderState);

    /**
     * Gets the current ItemStackRenderState for rendering the lid item.
     * Returns the per-layer state if set, or a fallback shared state.
     */
    ItemStackRenderState getLidItemRenderState();
}
