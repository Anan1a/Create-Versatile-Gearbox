package com.anan1a.create_versatile_gearbox;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

public class AllItems {
    // 获取 Registrate 实例
    private static final CreateRegistrate REGISTRATE = Registers.registrate();

    // 静态初始化块：设置默认创造模式选项卡
    static {
        REGISTRATE.setCreativeTab(CreativeTabs.EXAMPLE_TAB);
    }

    // 示例物品注册（自动注册对应的 Item，关联到 EXAMPLE_TAB）
    public static final ItemEntry<Item> EXAMPLE_ITEM = REGISTRATE.item("example_item", Item::new)
            .properties(p -> p.food(new FoodProperties.Builder()
                    .alwaysEdible().nutrition(1).saturationModifier(2f).build()))
            .register();

    // 注册方法（触发类加载）
    public static void register() {}
}
