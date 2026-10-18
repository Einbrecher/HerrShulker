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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // --- TEMP 26.x GUI-render diagnostics (remove once the item-path render is confirmed) ---
    @Unique
    private final Logger herrshulker$diagLog = LoggerFactory.getLogger("herrshulker-26xdiag");
    @Unique
    private boolean herrshulker$extractArgCalled = false;
    @Unique
    private String herrshulker$lastDiag = "";

    @Unique
    private void herrshulker$diag(String msg) {
        // Throttle: only emit when the outcome string changes (render runs every frame).
        if (!msg.equals(this.herrshulker$lastDiag)) {
            this.herrshulker$lastDiag = msg;
            this.herrshulker$diagLog.info("[26x lid-item/GUI] {}", msg);
        }
    }

    @Nullable
    public Void extractArgument(ItemStack arg) {
        this.herrshulker$extractArgCalled = true; // diag: did the interface-default override actually run?
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

    // 26.1.2: the GUI/inventory/hand lid overlay is NO LONGER drawn here.
    // The old approach did a nested ItemStackRenderState.submit() inside this special-renderer
    // submit. On 26.1.x that nested submit is scissor-clipped by the GuiItemAtlas slot (sized from
    // the render-state layers' bounding box), so it never showed. Now that the lid is appended as a
    // real sibling layer of the box's render state (MixinSpecialModelWrapper.update), the slot grows
    // to include it — and leaving this nested submit in place rendered a SECOND, un-clipped copy.
    // The lid item path is therefore fully handled at render-state build time; nothing to inject here.

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
            herrshulker$diag("DELIVERY FAIL: stack=" + (this.stack == null ? "NULL" : "set")
                    + " (client/level null? " + (client == null || client.level == null) + "); extractArgCalled=" + herrshulker$extractArgCalled);
            return;
        }

        var item = this.stack.getItem();
        if (!(item instanceof BlockItem blockItem)) {
            herrshulker$diag("abort: stack is not a BlockItem (" + item + "); extractArgCalled=" + herrshulker$extractArgCalled);
            return;
        }

        var block = blockItem.getBlock();
        if (!(block instanceof ShulkerBoxBlock)) {
            herrshulker$diag("abort: BlockItem is not a ShulkerBoxBlock (" + block + ")");
            return;
        }

        TypedEntityData<BlockEntityType<?>> blockComponent = this.stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockComponent == null) {
            herrshulker$diag("DECODE FAIL: stack delivered + is a shulker box, but BLOCK_ENTITY_DATA component is absent");
            return;
        }

        RegistryAccess registryAccess = client.level.registryAccess();
        CompoundTag tag = blockComponent.copyTagWithoutId();
        var problemReporter = new ProblemReporter.Collector();
        var valueInput = TagValueInput.create(problemReporter, registryAccess, tag);

        // Parse lid item
        cachedLidItem = valueInput.read(ModComponents.LID_ITEM, ItemStack.CODEC);
        boolean fromCompat = false;
        if (cachedLidItem.isEmpty()) {
            cachedLidItem = valueInput.read(ModComponents.COMPAT_DISPLAY, ItemStack.CODEC);
            fromCompat = cachedLidItem.isPresent();
        }

        // Parse custom scale
        cachedCustomScale = tag.getFloat(ModComponents.LID_ITEM_CUSTOM_SCALE);

        if (cachedLidItem.isPresent()) {
            herrshulker$diag("OK: stack delivered (extractArgCalled=" + herrshulker$extractArgCalled
                    + "), lidItem=" + cachedLidItem.get().getItem() + (fromCompat ? " [via COMPAT_DISPLAY]" : "") + " -> should render");
        } else {
            herrshulker$diag("DECODE FAIL: stack delivered + BLOCK_ENTITY_DATA present, but LID_ITEM/COMPAT_DISPLAY decode returned empty; beData=" + tag);
        }
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
