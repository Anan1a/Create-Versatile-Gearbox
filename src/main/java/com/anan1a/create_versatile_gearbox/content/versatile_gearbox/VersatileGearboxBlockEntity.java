package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 万能变速箱方块实体
 * <p>
 * 继承自 DirectionalShaftHalvesBlockEntity，实现平行变速箱的核心功能：
 * - 当动力从某个面输入时，该面的对面输出反向旋转（速度倍率 -1）
 * - 其他两个面输出同向旋转（速度倍率 1）
 * <p>
 * 例如：Y轴方块（默认放置），当动力从北(N)面输入时：
 * - 南(S)面输出：反向旋转（-1）
 * - 东(E)面和西(W)面输出：同向旋转（1）
 */
public class VersatileGearboxBlockEntity extends SplitShaftBlockEntity {

    public VersatileGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 获取指定面的旋转速度Modifier
     * <p>
     * 核心逻辑：
     * - 如果该面与动力源面在同一轴方向上（轴方向相同），返回 1（同向旋转）
     * - 如果该面与动力源面在不同轴方向上（轴方向相反），返回 -1（反向旋转）
     * <p>
     * 这是平行变速箱的关键特性：只有与动力源相对的面会反转，
     * 而与动力源同轴的其他两个面保持同向旋转。
     *
     * @param face 要查询的面（输出面）
     * @return 旋转速度Modifier：1 表示同向，-1 表示反向
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 如果没有动力源，返回 1（无Modifier）
        if (!hasSource())
            return 1;
        // 使用动力源面计算Modifier
        return getRotationSpeedModifier(face, getSourceFacing());
    }

    /**
     * 根据动力源面计算指定输出面的旋转速度Modifier
     * <p>
     * 原理：
     * - Direction.getAxisDirection() 返回 POSITIVE 或 NEGATIVE
     * - 当两个面的轴方向相同时（都是正方向或都是负方向），返回 1
     * - 当两个面的轴方向相反时，返回 -1
     *
     * @param face  输出面
     * @param source 动力源面（输入面）
     * @return 旋转速度Modifier
     */
    public static float getRotationSpeedModifier(Direction face, Direction source) {
        return face.getAxisDirection() == source.getAxisDirection() ? 1 : -1;
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
