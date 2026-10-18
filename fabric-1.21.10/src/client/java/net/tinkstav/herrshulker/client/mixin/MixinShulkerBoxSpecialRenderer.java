package net.tinkstav.herrshulker.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.tinkstav.herrshulker.HerrShulker;
import net.tinkstav.herrshulker.client.IShulkerRendererLidItem;
import net.tinkstav.herrshulker.client.HerrShulkerClient;
import net.tinkstav.herrshulker.component.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.ShulkerBoxSpecialRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.TagValueInput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ShulkerBoxSpecialRenderer.class)
public abstract class MixinShulkerBoxSpecialRenderer implements NoDataSpecialModelRenderer, IShulkerRendererLidItem {

    // Current stack being rendered
    @Unique @Nullable
    private ItemStack stack;

    // Fallback shared render state (used if per-layer state not set)
    @Unique
    private final ItemStackRenderState fallbackLidItemRenderState = new ItemStackRenderState();

    // Per-layer render state passed from MixinLayerRenderState
    @Unique @Nullable
    private ItemStackRenderState currentLidItemRenderState;

    // === Caching fields for Issue 1: Per-frame NBT parsing optimization ===
    @Unique @Nullable
    private ItemStack cachedSourceStack;
    @Unique
    private Optional<ItemStack> cachedLidItem = Optional.empty();
    @Unique
    private Optional<Float> cachedCustomScale = Optional.empty();

    @Nullable
    public Void extractArgument(ItemStack arg) {
        // Invalidate cache if stack changed
        if (arg != this.stack) {
            this.cachedSourceStack = null;
        }
        this.stack = arg;
        return NoDataSpecialModelRenderer.super.extractArgument(arg);
    }

    @Override
    @Unique
    public @Nullable ItemStack getStack() {
        return stack;
    }

    @Override
    @Unique
    public void setStack(@Nullable ItemStack stack) {
        // Invalidate cache if stack changed
        if (stack != this.stack) {
            this.cachedSourceStack = null;
        }
        this.stack = stack;
    }

    @Override
    @Unique
    public void setLidItemRenderState(ItemStackRenderState renderState) {
        this.currentLidItemRenderState = renderState;
    }

    @Override
    @Unique
    public ItemStackRenderState getLidItemRenderState() {
        // Use per-layer state if available, otherwise fallback to shared state
        return currentLidItemRenderState != null ? currentLidItemRenderState : fallbackLidItemRenderState;
    }

    @Inject(
            method = "submit",
            at = @At("RETURN")
    )
    public void submit(ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, int packedOverlay, boolean hasFoilType, int seed, CallbackInfo ci) {

        Minecraft minecraftClient = Minecraft.getInstance();
        Optional<ItemStack> maybeItem = getLidItem(minecraftClient);
        if (maybeItem.isEmpty()) {
            return;
        }
        var lidItem = maybeItem.get().copy();
        poseStack.pushPose();

        // For non-map items, render as item on lid
        if (lidItem.getItem() instanceof BlockItem && ((BlockItem) (lidItem.getItem())).getBlock() instanceof ShulkerBoxBlock) {
            poseStack.translate(0.5F, 1.0F, 0.5F);
        } else {
            poseStack.rotateAround(Direction.NORTH.getRotation(), 0.0F, 1.0F, 0.0F);
            poseStack.translate(-0.5F, 0.5F, 0.0F);

            var scale = HerrShulkerClient.Config.getLidItemScale(displayContext.getSerializedName());
            if (HerrShulker.Config.isPerShulkerScalingAllowed() && HerrShulkerClient.Config.getShowCustomScales()) {
                var customScale = getLidItemCustomScale(minecraftClient);
                scale = customScale.orElse(scale);
            }
            poseStack.scale(scale, scale, scale);
        }

        // Use per-layer render state to avoid corruption when multiple shulkers render in same frame
        ItemStackRenderState lidItemRenderState = getLidItemRenderState();
        ItemModelResolver itemModelResolver = minecraftClient.getItemModelResolver();
        lidItemRenderState.clear();
        itemModelResolver.updateForTopItem(lidItemRenderState, lidItem, ItemDisplayContext.FIXED, minecraftClient.level, null, seed);
        lidItemRenderState.submit(poseStack, submitNodeCollector, packedLight, packedOverlay, seed);

        poseStack.popPose();
    }

    /**
     * Checks if the cached data is still valid for the current stack.
     */
    @Unique
    private boolean isCacheValid() {
        return this.stack != null && this.stack == this.cachedSourceStack;
    }

    /**
     * Parses and caches the lid item data from the current stack.
     * Uses caching to avoid expensive NBT parsing every frame.
     */
    @Unique
    private void parseAndCacheLidData(Minecraft client) {
        if (isCacheValid()) {
            return; // Cache is still valid
        }

        // Reset cache
        cachedLidItem = Optional.empty();
        cachedCustomScale = Optional.empty();
        cachedSourceStack = this.stack;

        if (client == null || client.level == null || this.stack == null) {
            return;
        }

        var item = this.stack.getItem();
        if (!(item instanceof BlockItem blockItem)) {
            return;
        }

        var block = blockItem.getBlock();
        if (!(block instanceof ShulkerBoxBlock)) {
            return;
        }

        TypedEntityData<BlockEntityType<?>> blockComponent = this.stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockComponent == null) {
            return;
        }

        RegistryAccess registryAccess = client.level.registryAccess();
        CompoundTag tag = blockComponent.copyTagWithoutId();
        var problemReporter = new ProblemReporter.Collector();
        var valueInput = TagValueInput.create(problemReporter, registryAccess, tag);

        // Parse lid item
        cachedLidItem = valueInput.read(ModComponents.LID_ITEM, ItemStack.CODEC);
        if (cachedLidItem.isEmpty()) {
            cachedLidItem = valueInput.read(ModComponents.COMPAT_DISPLAY, ItemStack.CODEC);
        }

        // Parse custom scale
        cachedCustomScale = tag.getFloat(ModComponents.LID_ITEM_CUSTOM_SCALE);
    }

    @Unique
    private Optional<ItemStack> getLidItem(Minecraft client) {
        parseAndCacheLidData(client);
        return cachedLidItem;
    }

    @Unique
    private Optional<Float> getLidItemCustomScale(Minecraft client) {
        parseAndCacheLidData(client);
        return cachedCustomScale;
    }

    @Override
    @Unique
    public TypedEntityData<BlockEntityType<?>> getBlockEntityData() {
        if (this.stack != null) {
            return this.stack.get(DataComponents.BLOCK_ENTITY_DATA);
        }
        return null;
    }
}
