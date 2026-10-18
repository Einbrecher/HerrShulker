package net.tinkstav.herrshulker.client.mixin;

import net.tinkstav.herrshulker.HerrShulker;
import net.tinkstav.herrshulker.client.GuiLidItemReader;
import net.tinkstav.herrshulker.client.HerrShulkerClient;
import net.tinkstav.herrshulker.client.IShulkerRendererLidItem;
import net.tinkstav.herrshulker.client.LidItemPose;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(SpecialModelWrapper.class)
public abstract class MixinSpecialModelWrapper<T> implements ItemModel {
    @Final
    @Shadow
    private SpecialModelRenderer<T> specialRenderer;

    /**
     * 26.1.2 fix for the GUI/inventory/hand item path.
     *
     * On 1.21.x the renderer's stack was captured via the
     * {@code NoDataSpecialModelRenderer.extractArgument(ItemStack)} override on
     * MixinShulkerBoxSpecialRenderer. After the 26.1 special-model interface restructuring
     * (dual {@code extractArgument} defaults + a new no-data {@code submit}) that override no
     * longer reliably wins over the interface default, so the renderer's stack stays null in
     * the item path and no lid item is drawn — while the world BlockEntity path is unaffected.
     *
     * {@code update(...)} is the stable, always-called point that holds the real ItemStack, and
     * vanilla invokes {@code extractArgument} then {@code setupSpecialModel} later in the same
     * call. Capturing here at HEAD guarantees {@code LayerRenderState.setupSpecialModel} (@RETURN)
     * reads a non-null {@code getStack()}, independent of interface-default dispatch. Not gated on
     * display context so inventory/hand/GUI all populate.
     */
    @Inject(method = "update", at=@At("HEAD"), require = 1)
    public void herrshulker$captureStackOnUpdate(ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext, ClientLevel clientLevel, @Nullable ItemOwner itemOwner, int i, CallbackInfo ci){
        if(this.specialRenderer instanceof IShulkerRendererLidItem shulkerRenderer){
            shulkerRenderer.setStack(itemStack);
        }
    }

    @Inject(method = "update", at=@At("TAIL"), require = 1)
    public void update(ItemStackRenderState itemStackRenderState, ItemStack itemStack, ItemModelResolver itemModelResolver, ItemDisplayContext itemDisplayContext, ClientLevel clientLevel, @Nullable ItemOwner itemOwner, int i, CallbackInfo ci){
        if(!(this.specialRenderer instanceof IShulkerRendererLidItem)){
            return;
        }
        Optional<ItemStack> maybeLid = GuiLidItemReader.readLidItem(itemStack, clientLevel);
        if(maybeLid.isEmpty()){
            return;
        }
        ItemStack lidItem = maybeLid.get();

        // GUI atlas cache key: invalidate the cached atlas slot when the lid contents change.
        if(itemDisplayContext == ItemDisplayContext.GUI){
            var data = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
            if(data != null){
                itemStackRenderState.appendModelIdentityElement(data);
            }
        }

        // 26.1.2: the GUI item pipeline (GuiItemAtlas + oversized PIP) renders an item from its
        // ItemStackRenderState LAYERS into a fixed, scissor-clipped atlas slot. A nested submit from
        // inside the special renderer falls outside that slot and is clipped, so we append the lid item
        // as real sibling layers of the box's render state, then place them (below) and flip the
        // oversized-GUI routing flag (further below) so the box+lid escapes the slot scissor.
        // (The world render keeps its own nested-submit path — there is no atlas there.)
        ItemStackRenderStateAccessor accessor = (ItemStackRenderStateAccessor) itemStackRenderState;
        int before = accessor.herrshulker$getActiveLayerCount();
        itemModelResolver.appendItemLayers(itemStackRenderState, lidItem, itemDisplayContext, clientLevel, itemOwner, i);
        int after = accessor.herrshulker$getActiveLayerCount();

        if(after == before){
            HerrShulker.LOGGER.warn("[26x lid-item/GUI] appendItemLayers added no layers for lid {} (ctx {}) — lid will not render", lidItem.getItem(), itemDisplayContext);
            return;
        }

        // Lid placement. The lid must render in the SAME view as the box and sit on its lid.
        // The box (special-model) layer carries the context display transform — isometric in GUI,
        // the hand pose in 1st/3rd person — so the appended lid layers must SHARE that transform.
        // (Zeroing it to NO_TRANSFORM put the lid in raw model space: "square to the UI" for blocks
        // and edge-on/barely-visible for flat sprites, detached from the isometric box.) The lid's own
        // placement — seat on the box top, spin/clock, scale — lives in localTransform (box MODEL space,
        // built by LidItemPose), so the shared display transform then projects it onto the box's top face.
        float baseScale = HerrShulkerClient.Config.getLidItemScale("block");
        ItemStackRenderState.LayerRenderState[] layers = accessor.herrshulker$getLayers();
        // The box layer (added by vanilla before our @TAIL append) holds the context display transform.
        ItemTransform boxItemTransform = (before > 0)
                ? ((LayerRenderStateAccessor) layers[before - 1]).herrshulker$getItemTransform()
                : ItemTransform.NO_TRANSFORM;
        for(int idx = before; idx < after && idx < layers.length; idx++){
            ItemStackRenderState.LayerRenderState layer = layers[idx];
            // gui-light flag is the reliable flat-vs-3D discriminator (item CLASS is not: seeds are
            // BlockItems yet render as flat sprites). Flat sprites read larger laid flat → own pose.
            boolean flat = !((LayerRenderStateAccessor) layer).herrshulker$usesBlockLight();
            layer.setItemTransform(boxItemTransform);   // share the box's view (isometric in GUI, hand pose otherwise)
            layer.setLocalTransform(flat ? LidItemPose.forFlat(baseScale) : LidItemPose.forBlock(baseScale));
        }

        // ===== 26.1.2 inventory/hotbar fix: route box+lid through the oversized GUI item path =====
        // GuiItemAtlas.drawToSlot renders a normal item into a FIXED slotTextureSize slot and scissors
        // to that rect, so the lid — which sits above the box — is clipped off the slot's top edge.
        // An item escapes that scissor ONLY when BOTH hold (verified against 26.1.2 sources):
        //   1. ItemStackRenderState.isOversizedInGui() == true, and
        //   2. getModelBoundingBox() exceeds one block (>16px) in X or Y.
        // GuiItemRenderState.calculateOversizedItemBounds() then returns a non-null rectangle and
        // GuiRenderer.prepareItemElements() routes the item to OversizedItemRenderer (a PIP renderer),
        // which submits the WHOLE render state (box + lid) with no slot scissor.
        //
        // Condition 2 is already satisfied: the lid layers appended above carry real extents
        // (CuboidItemModelWrapper.update sets them from the model's quad corners), and
        // ItemStackRenderState.visitExtents re-applies itemTransform*localTransform — the SAME matrix
        // used to render — when building the box. So getModelBoundingBox() (computed later, after this
        // @TAIL returns, by GuiItemRenderState) already includes the lifted lid. We therefore must NOT
        // pre-transform the extents ourselves: visitExtents would apply our localTransform a second time
        // and corrupt the bounds. The only missing piece is condition 1 — the routing flag — which must
        // be set AFTER appendItemLayers above (that call reset the flag to the lid item's own, usually
        // false, property). Gate on GUI: the flag is read only there; hand/ground/item-frame have no
        // atlas and already show the lid.
        if (itemDisplayContext == ItemDisplayContext.GUI) {
            itemStackRenderState.setOversizedInGui(true);
        }
    }

}
