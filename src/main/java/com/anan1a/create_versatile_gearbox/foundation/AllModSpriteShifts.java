package com.anan1a.create_versatile_gearbox.foundation;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;

import net.minecraft.resources.ResourceLocation;

/**
 * 自定义连接纹理配置
 * <p>
 * 为多功能齿轮箱的机壳定义连接纹理映射关系
 */
public class AllModSpriteShifts {

    /**
     * 多功能齿轮箱机壳连接纹理
     * <p>
     * 使用 OMNIDIRECTIONAL 类型，支持全方向连接
     * 自动查找：
     * - 基础纹理：create_versatile_gearbox:block/versatile_gearbox/casing
     * - 连接纹理：create_versatile_gearbox:block/versatile_gearbox/casing_connected
     */
    public static final CTSpriteShiftEntry VERSATILE_GEARBOX_CASING = getCT(
        AllCTTypes.OMNIDIRECTIONAL, 
        "versatile_gearbox/casing"
    );

    /**
     * 获取连接纹理配置的辅助方法
     * 
     * @param type 连接类型（如 OMNIDIRECTIONAL、HORIZONTAL 等）
     * @param name 纹理名称（不含路径和扩展名）
     * @return 连接纹理配置项
     */
    private static CTSpriteShiftEntry getCT(com.simibubi.create.foundation.block.connected.CTType type, String name) {
        return CTSpriteShifter.getCT(
            type,
            ResourceLocation.fromNamespaceAndPath(MODID, "block/" + name),
            ResourceLocation.fromNamespaceAndPath(MODID, "block/" + name + "_connected")
        );
    }
}
