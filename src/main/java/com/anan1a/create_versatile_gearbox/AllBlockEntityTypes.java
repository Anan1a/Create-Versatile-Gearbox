package com.anan1a.create_versatile_gearbox;

import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VersatileGearboxBlockEntity;
import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VersatileGearboxRenderer;
import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VersatileGearboxVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

/**
 * 方块实体类型注册类
 * <p>
 * 使用 CreateRegistrate 注册方块实体类型，支持：
 * - 自动关联到对应的方块
 * - 配置渲染器（用于原版渲染）
 * - 配置可视化（用于Flywheel渲染引擎）
 */
public class AllBlockEntityTypes {

    private static final CreateRegistrate REGISTRATE = Registers.registrate();

    /**
     * 万能变速箱方块实体
     * <p>
     * 注册 ID: create_versatile_gearbox:versatile_gearbox
     * <p>
    * 使用 CreateRegistrate 链式 API 完成以下配置：
    * 1. 创建方块实体类型
    * 2. 配置可视化渲染（Flywheel引擎）
    * 3. 指定有效方块关联
    * 4. 配置原版渲染器
     */
    public static final BlockEntityEntry<VersatileGearboxBlockEntity> VERSATILE_GEARBOX = REGISTRATE
            // 创建方块实体建造器
            // 参数1: 注册名称 -> 生成键: create_versatile_gearbox:versatile_gearbox
            // 参数2: 构造函数引用 -> 用于创建方块实体实例
            .blockEntity("versatile_gearbox", VersatileGearboxBlockEntity::new)
            
            // 配置 Flywheel 可视化渲染
            // 参数1: 可视化工厂 -> 返回 VersatileGearboxVisual 实例
            // 参数2: threadSafe -> false 表示不启用线程安全模式（简单渲染场景）
            .visual(() -> VersatileGearboxVisual::new, false)
            
            // 指定该方块实体可附着的方块
            // 关联到 AllBlocks.VERSATILE_GEARBOX，确保只能放置在该方块上
            .validBlocks(AllBlocks.VERSATILE_GEARBOX)
            
            // 配置原版渲染器
            // 返回 VersatileGearboxRenderer 实例，用于非 Flywheel 环境的渲染
            .renderer(() -> VersatileGearboxRenderer::new)
            
            // 完成注册，返回注册条目供后续引用
            .register();

    public static void register() {}
}