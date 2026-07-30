package com.mythicrpg.crafting;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LuckyBlock extends Block {

    private static final int LUCK_OFFSET = 10;

    public static final IntProperty LUCK = IntProperty.of(
            "luck",
            0,
            20
    );

    public LuckyBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(LUCK, encodeLuck(0)));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        int realLuck = LuckyBlockLuckManager.getLuck(context.getStack());
        return getDefaultState().with(LUCK, encodeLuck(realLuck));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LUCK);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()
                && world instanceof ServerWorld serverWorld
                && player instanceof ServerPlayerEntity serverPlayer) {
            int realLuck = decodeLuck(state.get(LUCK));
            LuckyBlockChoiceManager.consumeChoice(serverWorld, pos);
            LuckyBlockEventManager.trigger(serverWorld, pos, serverPlayer, realLuck);
        }

        return super.onBreak(world, pos, state, player);
    }

    public static int encodeLuck(int realLuck) {
        int clamped = LuckyBlockLuckManager.clamp(realLuck);
        return clamped + LUCK_OFFSET;
    }

    public static int decodeLuck(int storedLuck) {
        return LuckyBlockLuckManager.clamp(storedLuck - LUCK_OFFSET);
    }
}