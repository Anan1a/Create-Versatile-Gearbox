package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class VersatileGearboxBlockEntity extends DirectionalShaftHalvesBlockEntity {

    public VersatileGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }

}
