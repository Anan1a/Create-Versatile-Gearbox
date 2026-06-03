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
        // 获取方块状态
        BlockState state = getBlockState();
        // 获取动力源面
        Direction source = getSourceFacing();
        // 计算旋转方向
        return getRotationSpeedModifier(face, source, state);
    }

    /**
     * 根据动力源面计算指定输出面的旋转速度 Modifier（静态方法）。
     * <p>
     * 三状态计算逻辑：
     * <pre>
     * modifier = axisAdjust × faceModifier × sourceModifier
     *
     * axisAdjust: 轴方向对齐时 +1，相反时 -1
     *   faceModifier: FWD=+1, REV=-1, OFF=0
     * sourceModifier: FWD=+1, REV=-1, OFF=0
     * </pre>
     *
     * @param face   输出面
     * @param source 动力源面
     * @param state  方块状态
     * @return 旋转速度倍率（-1 ~ 1）
     */
    public static float getRotationSpeedModifier(Direction face, Direction source, BlockState state) {
        // 获取动力源面状态
        VersatileGearboxShaftState sourceState = VersatileGearboxBlock.getShaftState(source, state);
        // 如果动力源面关闭（非传动轴状态），返回 0（不输入动力）
        if (!sourceState.hasShaft()) return 0;

        // 获取输出面状态
        VersatileGearboxShaftState faceState = VersatileGearboxBlock.getShaftState(face, state);
        // 如果输出面关闭（非传动轴状态），返回 0（不输出动力）
        if (!faceState.hasShaft()) return 0;

        // 轴方向修正：AxisDirection.getStep() 返回 POSITIVE=1, NEGATIVE=-1
        return face.getAxisDirection().getStep() * source.getAxisDirection().getStep()
                * faceState.getModifier() * sourceState.getModifier();
    }

    /**
     * 计算指定方向的实际旋转速度。
     * <p>
     * 统一 Renderer 和 Visual 的速度计算逻辑：
     * <ol>
     *   <li>使用传入的基础速度</li>
     *   <li>如果有速度且有动力源，应用方向倍率</li>
     * </ol>
     *
     * @param baseSpeed    基础速度（来自动力网络）
     * @param direction    要计算的方向
     * @param sourceFacing 动力源方向（可为 null）
     * @return 该方向的旋转速度（负数表示反向旋转）
     */
    public float getSpeedForDirection(float baseSpeed, Direction direction, Direction sourceFacing) {
        return baseSpeed != 0 ? baseSpeed * getRotationSpeedModifier(direction, sourceFacing, getBlockState()) : 0;
    }

    @Override
    protected boolean isNoisy() {
        return true;
    }
}
