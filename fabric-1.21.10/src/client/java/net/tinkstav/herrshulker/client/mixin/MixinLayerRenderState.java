package net.tinkstav.herrshulker.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.tinkstav.herrshulker.client.IShulkerRendererLidItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class MixinLayerRenderState {
    @Shadow @Nullable
    private SpecialModelRenderer<Object> specialRenderer;

    /**
     * Unique field to store the shulker box ItemStack for this specific LayerRenderState.
     * This avoids conflicts with the shared renderer instance.
     */
    @Unique @Nullable
    private ItemStack mrshulker$capturedStack;

    /**
     * Per-layer ItemStackRenderState for rendering lid items.
     * Each LayerRenderState gets its own instance to prevent state corruption
     * when multiple shulker boxes are rendered in the same frame.
     * Lazily initialized to avoid infinite recursion during class loading.
     */
    @Unique
    private ItemStackRenderState mrshulker$lidItemRenderState;

    @Unique
    private ItemStackRenderState herrshulker$getLidItemRenderState() {
        if (mrshulker$lidItemRenderState == null) {
            mrshulker$lidItemRenderState = new ItemStackRenderState();
        }
        return mrshulker$lidItemRenderState;
    }

    @Inject(method = "setupSpecialModel", at = @At("RETURN"))
    public <T> void setupShulkerBoxSpecialRenderer(SpecialModelRenderer<T> specialModelRenderer, T object, CallbackInfo ci) {
        if (specialModelRenderer instanceof IShulkerRendererLidItem iShulkerRendererLidItem) {
            // Capture the stack from the renderer into our unique field on this LayerRenderState
            this.mrshulker$capturedStack = iShulkerRendererLidItem.getStack();
        } else {
            this.mrshulker$capturedStack = null;
        }
    }

    @Inject(method = "submit", at = @At("HEAD"))
    void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, int j, int k, CallbackInfo ci) {
        if (this.specialRenderer != null && (this.specialRenderer instanceof IShulkerRendererLidItem iShulkerRendererLidItem)) {
            // Restore the captured stack to the renderer just before submit
            iShulkerRendererLidItem.setStack(this.mrshulker$capturedStack);
            // Pass our per-layer render state to the renderer to avoid state corruption
            iShulkerRendererLidItem.setLidItemRenderState(herrshulker$getLidItemRenderState());
        }
    }
}
