package com.anan1a.create_versatile_gearbox;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;

/**
 * 方块注册类
 * <p>
 * 使用 CreateRegistrate 进行方块注册，支持：
 * - 自动关联创造模式选项卡
 * - 自动生成 BlockItem
 * - 简化的注册流程
 */
public class AllBlocks {
    /**
     * CreateRegistrate 实例，用于注册方块
     */
    private static final CreateRegistrate REGISTRATE = Registers.registrate();

    /**
     * 静态初始化块
     * <p>
     * 设置此类中所有方块的默认创造模式选项卡
     * 通过 setCreativeTab() 关联到 CreativeTabs.EXAMPLE_TAB
     * 所有后续注册的方块都会自动使用此选项卡
     */
    static {
        REGISTRATE.setCreativeTab(CreativeTabs.EXAMPLE_TAB);
    }

    /**
     * 示例方块
     * <p>
     * 注册 ID: create_versatile_gearbox:example_block
     * <p>
     * 使用 simpleItem() 自动生成对应的 BlockItem
     * 方块颜色设置为石色（MapColor.STONE）
     * 自动关联到 EXAMPLE_TAB 创造模式选项卡
     */
    public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE.block("example_block", Block::new)
            .properties(p -> p.mapColor(MapColor.STONE))
            // .item() - 手动配置物品时使用
            // .tab(CreativeTabs.EXAMPLE_TAB.getKey()) - 手动指定选项卡
            // .tab(CreativeModeTabs.BUILDING_BLOCKS) - 添加到多个选项卡
            // .build()
            .simpleItem()  // 简化的物品注册，自动生成 BlockItem
            .register();

    // 多功能传动箱 - 参考 Create 模组的 GearboxBlock 实现
    public static final BlockEntry<Block> VERSATILE_GEARBOX = REGISTRATE.block("versatile_gearbox", Block::new)
            // ========== 方块属性配置 ==========
            .properties(p -> p
                    .mapColor(MapColor.METAL)      // 设置地图颜色为金属色
                    .strength(3.0F, 6.0F)          // 设置硬度和抗爆性（参考 Create Gearbox）
                    .noOcclusion())                // 不遮挡其他方块的渲染（用于半透明或特殊形状方块）
            
            // 补充属性配置（单独调用一次 properties 保持代码清晰）
            .properties(p -> p
                    .isValidSpawn((state, level, pos, type) -> false))  // 禁止生物在此方块上生成
            
            // ========== 物品配置 ==========
            .item()                           // 创建对应的方块物品（BlockItem）
            .build()                          // 构建物品配置
            
            // ========== 完成注册 ==========
            .register();                      // 将方块和物品注册到游戏中

    /**
     * 注册触发方法
     * <p>
     * 空方法，用于触发类加载和静态字段初始化
     * 由 Registers.registerAll() 调用
     */
    public static void register() {}
}