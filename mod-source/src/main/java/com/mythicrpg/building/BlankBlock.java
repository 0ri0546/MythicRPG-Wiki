package com.mythicrpg.building;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** White base cube whose six exterior faces can independently consume and display a material. */
public final class BlankBlock extends BlockWithEntity {
    public static final MapCodec<BlankBlock> CODEC = createCodec(BlankBlock::new);

    public BlankBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BlankBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(
            World world,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!world.isClient
                && world.getBlockEntity(pos) instanceof BlankBlockEntity blank) {
            blank.setAppearance(BlankBlockItemData.read(stack));
        }
    }

    @Override
    protected ItemActionResult onUseWithItem(
            ItemStack stack,
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            net.minecraft.util.Hand hand,
            BlockHitResult hit
    ) {
        if (player.isSneaking() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!BlankBlockMaterialRegistry.isAllowed(blockItem.getBlock())) {
            return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (world.isClient) {
            return ItemActionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof BlankBlockEntity blank)) {
            return ItemActionResult.FAIL;
        }

        Direction face = hit.getSide();
        net.minecraft.util.Identifier material = BlankBlockMaterialRegistry.id(blockItem.getBlock());
        if (java.util.Objects.equals(blank.material(face), material)) {
            return ItemActionResult.SUCCESS;
        }

        net.minecraft.util.Identifier previous = blank.setFace(face, material);
        if (!player.isCreative()) {
            stack.decrement(1);
            refundMaterial(player, previous);
        }
        player.getInventory().markDirty();
        return ItemActionResult.SUCCESS;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (world.getBlockEntity(pos) instanceof BlankBlockEntity blank) {
            BlankBlockItemData.write(stack, blank.appearance());
        }
        return stack;
    }

    @Override
    protected List<ItemStack> getDroppedStacks(
            BlockState state,
            LootContextParameterSet.Builder builder
    ) {
        List<ItemStack> drops = new ArrayList<>(super.getDroppedStacks(state, builder));
        net.minecraft.entity.Entity cause = builder.getOptional(LootContextParameters.THIS_ENTITY);
        BlockEntity blockEntity = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (!(cause instanceof PlayerEntity) && blockEntity instanceof BlankBlockEntity blank) {
            Map<Item, Integer> materials = new IdentityHashMap<>();
            for (net.minecraft.util.Identifier id : blank.appearance().configuredMaterials()) {
                Item item = BlankBlockMaterialRegistry.item(id);
                if (item != Items.AIR) materials.merge(item, 1, Integer::sum);
            }
            for (Map.Entry<Item, Integer> entry : materials.entrySet()) {
                drops.add(new ItemStack(entry.getKey(), entry.getValue()));
            }
        }
        return List.copyOf(drops);
    }

    @Override
    public void afterBreak(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            ItemStack tool
    ) {
        if (!world.isClient
                && !player.isCreative()
                && blockEntity instanceof BlankBlockEntity blank) {
            Map<Item, Integer> refunds = new IdentityHashMap<>();
            for (net.minecraft.util.Identifier id : blank.appearance().configuredMaterials()) {
                Item item = BlankBlockMaterialRegistry.item(id);
                if (item != Items.AIR) {
                    refunds.merge(item, 1, Integer::sum);
                }
            }
            for (Map.Entry<Item, Integer> entry : refunds.entrySet()) {
                ItemStack refund = new ItemStack(entry.getKey(), entry.getValue());
                player.getInventory().insertStack(refund);
                if (!refund.isEmpty()) {
                    player.dropItem(refund, false);
                }
            }
            player.getInventory().markDirty();
        }
        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }

    private static void refundMaterial(PlayerEntity player, net.minecraft.util.Identifier material) {
        if (material == null) {
            return;
        }
        Item item = BlankBlockMaterialRegistry.item(material);
        if (item == Items.AIR) {
            return;
        }
        ItemStack refund = new ItemStack(item);
        player.getInventory().insertStack(refund);
        if (!refund.isEmpty()) {
            player.dropItem(refund, false);
        }
    }
}
