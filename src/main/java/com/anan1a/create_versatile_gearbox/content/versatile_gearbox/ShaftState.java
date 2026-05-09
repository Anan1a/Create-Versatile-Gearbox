package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import net.minecraft.util.StringRepresentable;

/**
 * 传动轴状态枚举
 * <p>
 * OFF:     关闭（不输出动力）
 * SAME:    同向旋转（与动力源同方向）
 * OPPOSITE: 反向旋转（与动力源反方向）
 */
public enum ShaftState implements StringRepresentable {
    OFF,
    SAME,
    OPPOSITE;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
