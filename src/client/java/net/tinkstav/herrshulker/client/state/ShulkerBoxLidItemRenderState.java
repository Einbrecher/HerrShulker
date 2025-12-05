package net.tinkstav.herrshulker.client.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface ShulkerBoxLidItemRenderState {
    Optional<ItemStack> mrshulker$getLidItem();
    void mrshulker$setLidItem(Optional<ItemStack> lidItem);
    Optional<Float> mrshulker$getLidItemCustomScale();
    void mrshulker$setLidItemCustomScale(Optional<Float> scale);

    /**
     * Get the ItemStackRenderState for rendering the lid item.
     * Each ShulkerBoxRenderState has its own instance to avoid state corruption
     * when multiple shulker boxes are rendered in the same frame.
     */
    ItemStackRenderState mrshulker$getLidItemRenderState();
}
