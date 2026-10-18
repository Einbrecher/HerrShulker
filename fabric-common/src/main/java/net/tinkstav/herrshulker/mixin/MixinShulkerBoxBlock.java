package net.tinkstav.herrshulker.mixin;


import com.mojang.serialization.Codec;
import net.tinkstav.herrshulker.IShulkerLidItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.tinkstav.herrshulker.component.ModComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin({ShulkerBoxBlock.class})
public abstract class MixinShulkerBoxBlock {
    @Inject(method = {"getDrops"}, at = {@At("RETURN")})
    public void getDroppedStacks(BlockState blockState, LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ShulkerBoxBlockEntity shulker) {
            if (blockEntity.getLevel() != null) {
                for (ItemStack stack : cir.getReturnValue()) {
                    var iLidItem = IShulkerLidItem.from(shulker);
                    var customScale = iLidItem.getLidItemCustomScale();
                    var maybeLidItem = iLidItem.getLidItem();
                    maybeLidItem.ifPresent(lidStack -> {
                        TypedEntityData<BlockEntityType<?>> existingData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
                        CompoundTag nbt = existingData != null ? existingData.copyTagWithoutId() : new CompoundTag();

                        ProblemReporter.Collector problemReporter = new ProblemReporter.Collector();
                        var output = TagValueOutput.createWithContext(problemReporter, blockEntity.getLevel().registryAccess());
                        // REMOVED: output.putString("id", "minecraft:shulker_box");
                        // The block entity type is preserved from existing data or defaults to vanilla
                        output.store(ModComponents.LID_ITEM, ItemStack.CODEC, lidStack);
                        customScale.ifPresent(scale -> output.store(ModComponents.LID_ITEM_CUSTOM_SCALE, Codec.FLOAT, scale));
                        nbt.merge(output.buildResult());

                        // Preserve existing block entity type for mod compatibility
                        BlockEntityType<?> entityType = existingData != null
                                ? existingData.type()
                                : BlockEntityType.SHULKER_BOX;
                        stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(entityType, nbt));
                    });
                }
            }
        }
    }
}
