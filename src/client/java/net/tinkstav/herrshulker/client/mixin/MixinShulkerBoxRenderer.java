package net.tinkstav.herrshulker.client.mixin;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.tinkstav.herrshulker.HerrShulker;
import net.tinkstav.herrshulker.client.HerrShulkerClient;
import net.tinkstav.herrshulker.client.state.ShulkerBoxLidItemRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.renderer.blockentity.state.ShulkerBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.tinkstav.herrshulker.IShulkerLidItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;


@Mixin({ShulkerBoxRenderer.class})
public abstract class MixinShulkerBoxRenderer {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/level/block/entity/ShulkerBoxBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = {@At("RETURN")}
    )
    private void extractRenderState(ShulkerBoxBlockEntity shulkerBoxBlockEntity, ShulkerBoxRenderState shulkerBoxRenderState, float f, Vec3 vec3, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, CallbackInfo ci){
        // Store lid item data in a custom interface on the render state
        if(shulkerBoxRenderState instanceof ShulkerBoxLidItemRenderState lidItemState) {
            var iShulker = IShulkerLidItem.from(shulkerBoxBlockEntity);
            lidItemState.mrshulker$setLidItem(iShulker.getLidItem());
            lidItemState.mrshulker$setLidItemCustomScale(iShulker.getLidItemCustomScale());
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ShulkerBoxRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = {@At("RETURN")}
    )
    private void postSubmit(ShulkerBoxRenderState shulkerBoxRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci){
        if(!(shulkerBoxRenderState instanceof ShulkerBoxLidItemRenderState lidItemState)) {
            return;
        }

        Optional<ItemStack> lidItem = lidItemState.mrshulker$getLidItem();
        if(lidItem.isEmpty()){
            return;
        }

        Minecraft minecraftClient = Minecraft.getInstance();
        Direction shulkerFacing = shulkerBoxRenderState.direction;
        float lidProgress = shulkerBoxRenderState.progress;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);

        float f, g;
        if(shulkerFacing.getAxis().isHorizontal()){
            f = 0.0F;
            g = 180.0F - shulkerFacing.toYRot();
        } else {
            f = (float) (-90 * shulkerFacing.getAxisDirection().getStep());
            g = 180F;
        }
        poseStack.translate(
            (0.5F + lidProgress / 2.0F) * shulkerFacing.getStepX(),
            0.5F + ((0.5F + lidProgress / 2.0F) * shulkerFacing.getStepY()),
            (0.5F + lidProgress / 2.0F) * (float)shulkerFacing.getStepZ()
        );

        poseStack.mulPose(Axis.XP.rotationDegrees(f));
        poseStack.mulPose(Axis.YP.rotationDegrees(g));
        poseStack.mulPose(Axis.ZP.rotationDegrees(lidProgress * 270.0F));

        var scale = HerrShulkerClient.Config.getLidItemScale("block");
        if(HerrShulker.Config.isPerShulkerScalingAllowed() && HerrShulkerClient.Config.getShowCustomScales()){
            var customScale = lidItemState.mrshulker$getLidItemCustomScale();
            scale = customScale.orElse(scale);
        }
        poseStack.scale(scale, scale, scale);

        // Use new render state system for item rendering
        // Get the per-render-state ItemStackRenderState to avoid state corruption
        // when multiple shulker boxes are rendered in the same frame
        ItemStackRenderState lidItemRenderState = lidItemState.mrshulker$getLidItemRenderState();
        ItemModelResolver itemModelResolver = minecraftClient.getItemModelResolver();
        lidItemRenderState.clear();
        itemModelResolver.updateForTopItem(lidItemRenderState, lidItem.get(), ItemDisplayContext.FIXED, minecraftClient.level, null, 0);
        lidItemRenderState.submit(poseStack, submitNodeCollector, shulkerBoxRenderState.lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }
}
