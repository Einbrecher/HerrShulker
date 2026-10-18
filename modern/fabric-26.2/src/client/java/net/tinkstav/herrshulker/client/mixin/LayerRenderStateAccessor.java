package net.tinkstav.herrshulker.client.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads a per-layer flag needed to orient an appended lid-item layer in the GUI.
 *
 * {@code usesBlockLight} mirrors the model's {@code gui_light} ("side" = 3D block model =&gt; true,
 * "front" = flat 2D sprite =&gt; false). This is the reliable flat-vs-3D discriminator at render-state
 * build time — the item CLASS is not (seeds are BlockItems yet render as flat sprites).
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface LayerRenderStateAccessor {
    @Accessor("usesBlockLight")
    boolean herrshulker$usesBlockLight();

    /** The layer's context display transform (isometric in GUI, hand pose in 1st/3rd person).
     *  Read from the box layer so appended lid layers can SHARE the box's view instead of zeroing it. */
    @Accessor("itemTransform")
    ItemTransform herrshulker$getItemTransform();
}
