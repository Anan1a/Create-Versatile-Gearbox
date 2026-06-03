package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import net.minecraft.util.StringRepresentable;

/**
 * 传动轴状态枚举
 * <p>
 * <b>有轴状态</b>：FWD（正向旋转）、REV（反向旋转）<br>
 * <b>无轴状态</b>：OFF（不输出动力）
 */
public enum VersatileGearboxShaftState implements StringRepresentable {
    FWD(true, 1),   // 同向旋转（与动力源同方向），倍率 1
    REV(true, -1),  // 反向旋转（与动力源反方向），倍率 -1
    OFF(false, 0);  // 关闭（不输出动力），倍率 0

    /**
     * 该面是否有传动轴。
     * <p>
     * 有轴 = 渲染半轴模型 + 可进行动力传输。
     * 无轴（OFF）= 不渲染、不传动力（{@code hasShaftTowards} 返回 false）。
     * Renderer、Visual、动力网络判断都应使用此方法。
     */
    private final boolean hasShaft;

    /**
     * 该状态的数值倍率。
     * <p>
     * 用于计算旋转速度的方向和大小：
     * <ul>
     *   <li>FWD → 1（正向传动）</li>
     *   <li>REV → -1（反向传动）</li>
     *   <li>OFF → 0（断开传动）</li>
     * </ul>
     */
    private final int modifier;

    VersatileGearboxShaftState(boolean hasShaft, int modifier) {
        this.hasShaft = hasShaft;
        this.modifier = modifier;
    }

    /**
     * 获取序列化的名称。
     * <p>
     * 用于 NBT 持久化、网络同步、命令参数解析等场景。
     * 返回小写字符串，与 Minecraft/ Create 惯例保持一致。
     *
     * @return 小写字符串名称（如 "fwd", "rev", "off"）
     */
    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    /**
     * 判断该面是否有传动轴。
     *
     * @return true 表示有轴（渲染 + 可传动力），false 表示无轴
     */
    public boolean hasShaft() {
        return hasShaft;
    }

    /**
     * 获取该状态的数值倍率。
     *
     * @return 数值倍率
     */
    public int getModifier() {
        return modifier;
    }
}
