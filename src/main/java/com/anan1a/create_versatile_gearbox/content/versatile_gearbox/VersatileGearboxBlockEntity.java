package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 万能变速箱方块实体
 * <p>
 * 【继承关系】
 * - 继承自 SplitShaftBlockEntity，获得多半轴动力传输能力
 * - SplitShaftBlockEntity 已实现多方向动力输入/输出的基础逻辑
 * <p>
 * 【核心功能】
 * - 当动力从某个面输入时，根据输出面与动力源面的轴方向关系决定旋转方向：
 *   - 输出面与动力源面轴方向相反（相对面）→ 反向旋转（速度倍率 -1）
 *   - 输出面与动力源面轴方向相同（邻位面）→ 同向旋转（速度倍率 1）
 * <p>
 * 【示例】方向轴=Y（垂直放置），动力从北(N)面输入：
 * - 北(N)面轴方向=-Z（负Z方向）
 * - 南(S)面轴方向=+Z（正Z方向）→ 与动力源轴方向相反 → 反向旋转（-1）
 * - 东(E)面轴方向=+X → 轴不同但方向相同 → 同向旋转（1）
 * - 西(W)面轴方向=-X → 轴不同但方向相同 → 同向旋转（1）
 * <p>
 * 【轴概念区分】
 * - 方向轴（箱体轴）：方块放置朝向（Y/X/Z），决定哪些面有传动轴
 * - 传动轴：每个输出面的半轴，围绕自己的轴旋转
 */
public class VersatileGearboxBlockEntity extends SplitShaftBlockEntity {

    public VersatileGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 获取指定面的旋转速度Modifier
     * <p>
     * 【调用场景】
     * - 动力网络更新时，用于计算每个输出面的实际旋转速度
     * - 渲染时，用于确定半轴的旋转方向
     * <p>
     * 【核心逻辑】
     * - 通过 getSourceFacing() 获取动力源面
     * - 调用静态方法计算输出面与动力源面的关系
     * <p>
     * 【轴方向判定】
     * - 轴方向（AxisDirection）：Direction 的正负方向（POSITIVE/NEGATIVE）
     * - 例如：EAST(+X) 的轴方向是 POSITIVE，WEST(-X) 是 NEGATIVE
     *
     * @param face 要查询的面（输出面）
     * @return 旋转速度Modifier：1 表示同向，-1 表示反向
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 如果没有动力源，返回 1（无Modifier，保持原速）
        if (!hasSource())
            return 1;
        // 获取动力源面并计算Modifier
        return getRotationSpeedModifier(face, getSourceFacing());
    }

    /**
     * 根据动力源面计算指定输出面的旋转速度Modifier（静态方法）
     * <p>
     * 【核心算法】
     * - face.getAxisDirection()：获取输出面的轴方向（POSITIVE/NEGATIVE）
     * - source.getAxisDirection()：获取动力源面的轴方向
     * - 比较两个轴方向：相同返回 1，不同返回 -1
     * <p>
     * 【数学原理】
     * - 当两个面在同一轴上且方向相反时（如 NORTH 和 SOUTH），轴方向不同
     * - 当两个面在不同轴上但方向相同时（如 NORTH 和 WEST），轴方向相同
     * <p>
     * 【示例】
     * - source=NORTH(-Z), face=SOUTH(+Z) → 轴方向不同 → 返回 -1（反向）
     * - source=NORTH(-Z), face=WEST(-X) → 轴方向相同 → 返回 1（同向）
     * - source=NORTH(-Z), face=EAST(+X) → 轴方向不同 → 返回 -1（反向）
     *
     * @param face   输出面
     * @param source 动力源面（输入面）
     * @return 旋转速度Modifier：1=同向，-1=反向
     */
    public static float getRotationSpeedModifier(Direction face, Direction source) {
        // 比较两个方向的轴方向
        // 相同 → 同向旋转 → 返回 1
        // 不同 → 反向旋转 → 返回 -1
        return face.getAxisDirection() == source.getAxisDirection() ? 1 : -1;
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }
}
