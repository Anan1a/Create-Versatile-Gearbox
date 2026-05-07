package com.anan1a.create_versatile_gearbox;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.neoforge.registries.DeferredRegister;

public class Registers {
    // 引用模组 ID，方便所有地方引用
    public static final String MODID = CreateVersatileGearbox.MODID;
    // 创建一个 Deferred Register 来保存方块，所有方块将注册到 "create_versatile_gearbox" 命名空间下
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // 创建一个 Deferred Register 来保存物品，所有物品将注册到 "create_versatile_gearbox" 命名空间下
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // 创建一个 Deferred Register 来保存创造模式选项卡，所有选项卡将注册到 "create_versatile_gearbox" 命名空间下
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);


    // 注册所有模组中的方块、物品和创造模式选项卡
    public static void registerAll(IEventBus modEventBus) {
        // 触发类加载
        LoadClass ();

        // 注册到模组事件总线，以便方块被注册
//        ModBlocks.register(modEventBus);
        BLOCKS.register(modEventBus);
        // 注册到模组事件总线，以便物品被注册
//        ModItems.register(modEventBus);
        ITEMS.register(modEventBus);
        // 注册到模组事件总线，以便选项卡被注册
//        CreativeTabs.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private static void LoadClass() {
        // 触发类加载
        new AllBlocks();
        new AllItems();
        new CreativeTabs();
    }
}
