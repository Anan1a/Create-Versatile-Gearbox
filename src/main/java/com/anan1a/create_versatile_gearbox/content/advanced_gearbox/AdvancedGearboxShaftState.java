package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import net.minecraft.util.StringRepresentable;

/**
 * 传动轴状态枚举
 * <p>
 * <b>有轴状态</b>：FWD（正向旋转）、REV（反向旋转）<br>
 * <b>无轴状态</b>：OFF（不输出动力）
 */
public enum AdvancedGearboxShaftState implements StringRepresentable {
    FWD(true),   // 同向旋转（与动力源同方向）
    REV(true),   // 反向旋转（与动力源反方向）
    OFF(false);  // 关闭（不输出动力）

    /**
     * 该状态是否渲染传动轴。
     * <p>
     * 统一管理渲染逻辑，便于扩展新状态（如 PARTIAL、LOCKED 等）。
     * Renderer 和 Visual 都应使用此方法判断。
     */
    private final boolean shouldRenderShaft;

    AdvancedGearboxShaftState(boolean shouldRenderShaft) {
        this.shouldRenderShaft = shouldRenderShaft;
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
     * 判断该状态是否应该渲染传动轴。
     *
     * @return true 表示渲染传动轴，false 表示不渲染
     */
    public boolean shouldRenderShaft() {
        return shouldRenderShaft;
    }
}
