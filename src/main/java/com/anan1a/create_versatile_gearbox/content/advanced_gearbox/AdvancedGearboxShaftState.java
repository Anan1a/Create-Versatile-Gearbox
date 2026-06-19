package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import net.minecraft.util.StringRepresentable;

/**
 * 传动轴状态枚举
 * <p>
 * <b>有轴状态</b>：SHAFT（有传动轴，可变配）<br>
 * <b>无轴状态</b>：OFF（关闭，不输出动力）、CFG（配置面，用作滑条交互面）
 */
public enum AdvancedGearboxShaftState implements StringRepresentable {
    SHAFT(true, false),  // 有传动轴
    OFF(false, true),   // 关闭（不输出动力），有纹理连接
    CFG(false, false);  // 配置面（无轴、无纹理连接，用作滑条交互面）

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

    AdvancedGearboxShaftState(boolean hasShaft, boolean hasTextureConnection) {
        this.hasShaft = hasShaft;
        this.hasTextureConnection = hasTextureConnection;
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
     * 顺序：SHAFT → OFF → CFG → SHAFT
     *
     * @return 下一个状态
     */
    public AdvancedGearboxShaftState next() {
        return switch (this) {
            case SHAFT -> OFF;
            case OFF -> CFG;
            case CFG -> SHAFT;
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
}
