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
 */
public class VersatileGearboxBlockEntity extends SplitShaftBlockEntity {

    public VersatileGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 获取指定输出面的旋转速度Modifier
     *
     * @param face 输出面方向
     * @return 旋转速度倍率（0=关闭, 1=同向, -1=反向）
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 获取方块状态
        BlockState state = getBlockState();

        // 1. 检查输出面状态
        if (VersatileGearboxBlock.getShaftState(face, state) == VersatileGearboxShaftState.OFF) return 0;

        // 2. 检查是否有动力源
        if (!hasSource()) return 0;

        // 3. 检查输入面状态
        Direction source = getSourceFacing();
        if (VersatileGearboxBlock.getShaftState(source, state) == VersatileGearboxShaftState.OFF) return 0;

        // 4. 计算旋转方向
        return getRotationSpeedModifier(face, source, state);
    }

    /**
     * 根据动力源面计算指定输出面的旋转速度Modifier（静态方法）
     * <p>
     * 三状态版本：
     * FWD:     同向旋转（与动力源同方向）
     * REV:     反向旋转（与动力源反方向）
     * OFF:     关闭（不输出动力）
     *
     * @param face   输出面
     * @param source 动力源面
     * @param state  方块状态
     * @return 旋转速度倍率（0=关闭, 1=同向, -1=反向）
     */
    public static float getRotationSpeedModifier(Direction face, Direction source, BlockState state) {
        // 获取输入面（动力源面）状态
        VersatileGearboxShaftState sourceState = VersatileGearboxBlock.getShaftState(source, state);
        
        // 如果输入面关闭，所有输出都停止
        if (sourceState == VersatileGearboxShaftState.OFF) return 0;

        // 获取输出面状态
        VersatileGearboxShaftState faceState = VersatileGearboxBlock.getShaftState(face, state);

        // 如果输出面关闭，返回0（不输出动力）
        if (faceState == VersatileGearboxShaftState.OFF) return 0;

        // 轴方向修正
        int axisAdjust = face.getAxisDirection() == source.getAxisDirection() ? 1 : -1;

        int faceModifier = getStateModifier(faceState);
        int sourceModifier = getStateModifier(sourceState);

        return axisAdjust * faceModifier * sourceModifier;
    }

    /**
     * 将轴状态转换为旋转方向修正值
     *
     * @param state 轴状态（FWD同向/REV反向/OFF关闭）
     * @return 方向修正值：1（同向）、-1（反向）、0（关闭）
     */
    private static int getStateModifier(VersatileGearboxShaftState state) {
        return switch (state) {
            case FWD -> 1;
            case REV -> -1;
            case OFF -> 0;
        };
    }

    @Override
    protected boolean isNoisy() {
        return true;
    }
}
