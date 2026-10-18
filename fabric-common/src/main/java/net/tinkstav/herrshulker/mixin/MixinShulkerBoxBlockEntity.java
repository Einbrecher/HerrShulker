package net.tinkstav.herrshulker.mixin;


import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.tinkstav.herrshulker.IShulkerLidItem;
import net.tinkstav.herrshulker.component.ModComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;


@Mixin({ShulkerBoxBlockEntity.class})
public abstract class MixinShulkerBoxBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer, IShulkerLidItem {
    protected MixinShulkerBoxBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    // Initialize to EMPTY instead of null for safety
    @Unique
    private ItemStack lidItem = ItemStack.EMPTY;

    @Unique
    private float lidItemCustomScale;

    @Override
    @Unique
    public Optional<ItemStack> getLidItem() {
        // Return empty Optional if lidItem is empty, otherwise return the item
        return lidItem.isEmpty() ? Optional.empty() : Optional.of(lidItem);
    }

    @Override
    @Unique
    public void setLidItem(ItemStack stack) {
        // Handle null by converting to EMPTY
        lidItem = stack != null ? stack : ItemStack.EMPTY;
    }

    @Override
    @Unique
    public Optional<Float> getLidItemCustomScale() {
        return lidItemCustomScale > 0.0F ? Optional.of(lidItemCustomScale) : Optional.empty();
    }

    @Override
    @Unique
    public void setLidItemCustomScale(Float customScale) {
        lidItemCustomScale = customScale != null ? customScale : 0.0F;
    }

    @Inject(method = {"saveAdditional"}, at = {@At("RETURN")})
    public void mixinSaveAdditional(ValueOutput valueOutput, CallbackInfo ci) {
        getLidItem().ifPresent(itemStack -> valueOutput.store(ModComponents.LID_ITEM, ItemStack.CODEC, itemStack));
        getLidItemCustomScale().ifPresent(scale -> valueOutput.putFloat(ModComponents.LID_ITEM_CUSTOM_SCALE, scale));
    }

    @Inject(method = {"loadAdditional"}, at = {@At("RETURN")})
    public void mixinLoadAdditional(ValueInput valueInput, CallbackInfo ci) {
        Optional<ItemStack> maybeLidItem = valueInput.read(ModComponents.LID_ITEM, ItemStack.CODEC);
        if (maybeLidItem.isEmpty()) {
            maybeLidItem = valueInput.read(ModComponents.COMPAT_DISPLAY, ItemStack.CODEC);
        }
        // Use EMPTY instead of null
        this.lidItem = maybeLidItem.orElse(ItemStack.EMPTY);

        var customScale = valueInput.read(ModComponents.LID_ITEM_CUSTOM_SCALE, Codec.FLOAT);
        customScale.ifPresent(this::setLidItemCustomScale);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag nbt = super.getUpdateTag(registries);
        ProblemReporter.Collector problemReporter = new ProblemReporter.Collector();
        var output = TagValueOutput.createWithContext(problemReporter, registries);
        getLidItem().ifPresent(itemStack -> output.store(ModComponents.LID_ITEM, ItemStack.CODEC, itemStack));
        getLidItemCustomScale().ifPresent(scale -> output.putFloat(ModComponents.LID_ITEM_CUSTOM_SCALE, scale));
        nbt.merge(output.buildResult());
        return nbt;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 0);
        }
    }
}
