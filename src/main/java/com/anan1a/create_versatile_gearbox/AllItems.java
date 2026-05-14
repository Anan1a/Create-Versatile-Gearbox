package com.anan1a.create_versatile_gearbox;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

/**
 * 物品注册类
 * <p>
 * 使用 CreateRegistrate 进行物品注册，支持：
 * - 自动关联创造模式选项卡
 * - 简化的属性配置
 * - 统一的注册流程
 */
public class AllItems {
    /**
     * CreateRegistrate 实例，用于注册物品
     */
    private static final CreateRegistrate REGISTRATE = Registers.registrate();

//    /**
//     * 示例物品
//     * <p>
//     * 注册 ID: create_versatile_gearbox:example_item
//     * <p>
//     * 配置为食物属性：
//     * - alwaysEdible: 可以随时食用
//     * - nutrition(1): 恢复 1 点饥饿值
//     * - saturationModifier(2f): 饱和度修正值为 2.0
//     * 自动关联到 EXAMPLE_TAB 创造模式选项卡
//     */
//    public static final ItemEntry<Item> EXAMPLE_ITEM = REGISTRATE
//            .item("example_item", Item::new)
//            .properties(p -> p.food(new FoodProperties.Builder()
//                    .alwaysEdible()
//                    .nutrition(1)
//                    .saturationModifier(2f)
//                    .build()))
//            .register();

    /**
     * 注册触发方法
     * <p>
     * 空方法，用于触发类加载和静态字段初始化
     * 由 Registers.registerAll() 调用
     */
    public static void register() {}
}
