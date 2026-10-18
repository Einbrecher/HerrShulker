package net.tinkstav.herrshulker.client.mixin;

import net.tinkstav.herrshulker.client.state.ShulkerBoxLidItemRenderState;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

@Mixin(ShulkerBoxRenderState.class)
public class MixinShulkerBoxRenderState implements ShulkerBoxLidItemRenderState {

    @Unique
    private Optional<ItemStack> herrshulker$lidItem = Optional.empty();

    @Unique
    private Optional<Float> herrshulker$lidItemCustomScale = Optional.empty();

    /**
     * Each ShulkerBoxRenderState has its own ItemStackRenderState for the lid item.
     * This is crucial because the new render API queues render nodes that are processed later,
     * and if we shared a single ItemStackRenderState across all shulker boxes,
     * they would all show the same (last) item.
     */
    @Unique
    private final ItemStackRenderState herrshulker$lidItemRenderState = new ItemStackRenderState();

    @Override
    public Optional<ItemStack> herrshulker$getLidItem() {
        return herrshulker$lidItem;
    }

    @Override
    public void herrshulker$setLidItem(Optional<ItemStack> lidItem) {
        this.herrshulker$lidItem = lidItem;
    }

    @Override
    public Optional<Float> herrshulker$getLidItemCustomScale() {
        return herrshulker$lidItemCustomScale;
    }

    @Override
    public void herrshulker$setLidItemCustomScale(Optional<Float> scale) {
        this.herrshulker$lidItemCustomScale = scale;
    }

    @Override
    public ItemStackRenderState herrshulker$getLidItemRenderState() {
        return herrshulker$lidItemRenderState;
    }
}
