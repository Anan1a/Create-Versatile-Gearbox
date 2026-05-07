package com.anan1a.create_versatile_gearbox;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntry;

public class AllBlocks {
    // 获取 Registrate 实例
    private static final CreateRegistrate REGISTRATE = Registers.registrate();

    // 静态初始化块：设置默认创造模式选项卡
    static {
        REGISTRATE.setCreativeTab(CreativeTabs.EXAMPLE_TAB);
    }

    // 示例方块注册（自动注册对应的 BlockItem，关联到 EXAMPLE_TAB）
    public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE.block("example_block", Block::new)
            .properties(p -> p.mapColor(MapColor.STONE))
            // .item()
            // .tab(CreativeTabs.EXAMPLE_TAB.getKey())
            // .build()
            .simpleItem()
            .register();

    // 注册方法（触发类加载）
    public static void register() {}
}