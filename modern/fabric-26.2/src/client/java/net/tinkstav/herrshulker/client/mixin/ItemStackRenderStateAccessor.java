package net.tinkstav.herrshulker.client.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for ItemStackRenderState internals needed by the 26.1.2 GUI lid-item path.
 *
 * The GUI item pipeline (GuiItemAtlas + oversized PIP) renders an item from its render-state
 * LAYERS, sizing/scissoring the atlas slot from those layers' bounding box. To draw the lid item
 * in the GUI we append it as real layers and then transform only the newly-appended ones — which
 * requires reading the active layer count before/after the append and reaching the layer objects.
 */
@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccessor {
    @Accessor("activeLayerCount")
    int herrshulker$getActiveLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] herrshulker$getLayers();
}
