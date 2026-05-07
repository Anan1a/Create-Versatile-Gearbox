package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.Arrays;
import java.util.List;

import com.anan1a.create_versatile_gearbox.AllBlockEntityTypes;
import com.anan1a.create_versatile_gearbox.AllItems;
import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VersatileGearboxBlock;
import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VersatileGearboxBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

public class VersatileGearboxBlock extends RotatedPillarKineticBlock implements IBE<VersatileGearboxBlockEntity> {

    public VersatileGearboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.PUSH_ONLY;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (state.getValue(AXIS).isVertical())
            return super.getDrops(state, builder);
        return Arrays.asList(new ItemStack(AllItems.VERTICAL_GEARBOX.get()));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        if (state.getValue(AXIS).isVertical())
            return super.getCloneItemStack(state, target, level, pos, player);
        return new ItemStack(AllItems.VERTICAL_GEARBOX.get());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, Axis.Y);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() != state.getValue(AXIS);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<VersatileGearboxBlockEntity> getBlockEntityClass() {
        return VersatileGearboxBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VersatileGearboxBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.VERSATILE_GEARBOX.get();
    }
}
