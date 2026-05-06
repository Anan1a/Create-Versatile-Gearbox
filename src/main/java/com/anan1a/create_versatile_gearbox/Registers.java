package com.anan1a.create_versatile_gearbox;

import net.neoforged.bus.api.IEventBus;

import com.anan1a.create_versatile_gearbox.block.ModBlocks;
import com.anan1a.create_versatile_gearbox.item.ModItems;

public class Registers {
    public static void registerAll(IEventBus modEventBus) {
        // 注册到模组事件总线，以便方块被注册
        ModBlocks.register(modEventBus);
        // 注册到模组事件总线，以便物品被注册
        ModItems.register(modEventBus);
        // 注册到模组事件总线，以便选项卡被注册
        CreativeTabs.register(modEventBus);
    }
}
