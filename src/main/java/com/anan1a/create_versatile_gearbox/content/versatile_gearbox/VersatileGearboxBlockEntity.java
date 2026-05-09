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
     * @return 旋转速度倍率（1或-1）
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 如果没有动力源，返回 1（无Modifier，保持原速）
        if (!hasSource())
            return 1;
        // 获取动力源面并计算Modifier（包含翻转属性）
        return getRotationSpeedModifier(face, getSourceFacing(), getBlockState());
    }

    /**
     * 计算输出面相对于动力源面的旋转速度Modifier（静态方法）
     * <p>
     * 公式：Modifier = 轴方向修正 × 输出面翻转 × 动力源面翻转
     * 翻转状态可通过扳手右键切换。
     *
     * @param face   输出面
     * @param source 动力源面
     * @param state  方块状态
     * @return 旋转速度倍率（1或-1）
     */
    public static float getRotationSpeedModifier(Direction face, Direction source, BlockState state) {
        // 轴方向修正：同向为1，反向为-1
        int axisAdjust = face.getAxisDirection() == source.getAxisDirection() ? 1 : -1;
        
        // 翻转修正：翻转时为-1，不翻转为1
        int currentFlipped = VersatileGearboxBlock.isFaceFlipped(face, state) ? -1 : 1;
        int sourceFlipped = VersatileGearboxBlock.isFaceFlipped(source, state) ? -1 : 1;
        
        return axisAdjust * currentFlipped * sourceFlipped;
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
