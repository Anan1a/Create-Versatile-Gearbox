package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import net.minecraft.util.StringRepresentable;

/**
 * 传动轴状态枚举
 * <p>
 * OFF:     关闭（不输出动力）
 * FWD:    同向旋转（与动力源同方向）
 * REV: 反向旋转（与动力源反方向）
 */
public enum ShaftState implements StringRepresentable {
    OFF,
    FWD,
    REV;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
