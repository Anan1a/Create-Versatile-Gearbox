package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import net.minecraft.util.StringRepresentable;

/**
 * 传动轴状态枚举
 * <p>
 * <b>有轴状态</b>：FWD（正向旋转）、REV（反向旋转）<br>
 * <b>无轴状态</b>：OFF（不输出动力）
 */
public enum AdvancedGearboxShaftState implements StringRepresentable {
    FWD(true, false, 1),   // 同向旋转（与动力源同方向），无纹理连接，倍率 1
    REV(true, false, -1),  // 反向旋转（与动力源反方向），无纹理连接，倍率 -1
    OFF(false, true, 0);   // 关闭（不输出动力），有纹理连接，倍率 0

    /**
     * 该面是否有传动轴。
     * <p>
     * 有轴 = 渲染半轴模型 + 可进行动力传输。
     * 无轴（OFF）= 不渲染、不传动力（`hasShaftTowards` 返回 false）。
     * Renderer、Visual、动力网络判断都应使用此方法。
     */
    private final boolean hasShaft;

    /**
     * 该状态是否显示纹理连接效果。
     * <p>
     * 用于控制方块表面的纹理连接视觉效果，
     * 与传动轴渲染逻辑分离，可独立控制。
     */
    private final boolean hasTextureConnection;

    /**
     * 该状态的数值倍率。
     * <p>
     * 用于计算旋转速度的方向和大小：
     */
    private final int modifier;

    AdvancedGearboxShaftState(boolean hasShaft, boolean hasTextureConnection, int modifier) {
        this.hasShaft = hasShaft;
        this.hasTextureConnection = hasTextureConnection;
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
     * 获取下一个状态。
     * <p>
     * 顺序：FWD → REV → OFF → FWD
     *
     * @return 下一个状态
     */
    public AdvancedGearboxShaftState next() {
        return switch (this) {
            case FWD -> REV;
            case REV -> OFF;
            case OFF -> FWD;
        };
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
     * 判断该状态是否显示纹理连接效果。
     *
     * @return true 表示显示纹理连接，false 表示不显示
     */
    public boolean hasTextureConnection() {
        return hasTextureConnection;
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
