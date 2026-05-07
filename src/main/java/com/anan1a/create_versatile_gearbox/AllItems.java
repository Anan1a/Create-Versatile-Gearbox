package com.anan1a.create_versatile_gearbox;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AllItems {
    // 创建一个 Deferred Register 来保存物品，所有物品将注册到 "create_versatile_gearbox" 命名空间下
    public static final DeferredRegister.Items ITEMS = Registers.ITEMS;

    // 创建一个 ID 为 "create_versatile_gearbox:example_item" 的新食物物品，营养值为 1，饱和度倍率为 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item",
            new Item.Properties().food(new FoodProperties.Builder()
                    .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // 注册物品
//    public static void register(IEventBus modEventBus) {
//        ITEMS.register(modEventBus);
//    }
}
