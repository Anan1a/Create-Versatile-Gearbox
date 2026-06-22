package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 万能变速箱方块实体
 * <p>
 * 继承 SplitShaftBlockEntity，实现多方向动力传输。
 * 核心逻辑：根据输出面与动力源面的轴方向关系决定旋转方向，
 * 支持每个输出面独立翻转（通过扳手切换）。
 * <p>
 * 状态存储在 BlockState（EnumProperty）中，访问统一走 {@link VersatileGearboxBlock} 的静态方法。
 */
public class VersatileGearboxBlockEntity extends SplitShaftBlockEntity {

    public VersatileGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ===== 动力传输逻辑 =====

    /**
     * 获取指定输出面的旋转速度 Modifier。
     *
     * @param face 输出面方向
     * @return 旋转速度倍率（0=关闭, 1=同向, -1=反向）
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 计算旋转方向
        return face.getAxisDirection().getStep()
                * VersatileGearboxBlock.getShaftState(face, getBlockState()).getModifier();
    }

    /**
     * 获取指定面的视觉旋转速度（Renderer / Visual 统一入口）。
     * <p>
     * 从动力网络读取本块的基础速度，叠加该面的旋转倍率后返回。
     * 仅用于渲染/视觉展示，不参与动力网络计算。
     * 速度 = 0 时直接返回 0（无动力传入或该面关闭）。
     *
     * @param direction 要计算的面方向
     * @return 该面的视觉旋转速度（正=正向旋转，负=反向旋转，0=静止）
     */
    public float getVisualSpeed(Direction direction) {
        float baseSpeed = getSpeed();
        return baseSpeed != 0
                ? baseSpeed * getRotationSpeedModifier(direction)
                : 0;
    }
}
