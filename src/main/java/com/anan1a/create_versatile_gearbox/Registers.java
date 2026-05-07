package com.anan1a.create_versatile_gearbox;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import net.neoforged.bus.api.IEventBus;

import com.simibubi.create.foundation.data.CreateRegistrate;

public class Registers {
    // 引用模组 ID，方便所有地方引用
    public static final String MODID = CreateVersatileGearbox.MODID;

    // 创建一个 CreateRegistrate 实例，用于注册方块、物品
    // 设置 defaultCreativeTab 为 null，避免自动分配到未注册的选项卡
    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    // 提供 Registrate 实例给其他类使用
    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }

    // 注册所有模组中的方块、物品和创造模式选项卡
    public static void registerAll(IEventBus modEventBus) {
        // 1. 首先注册事件监听器
        REGISTRATE.registerEventListeners(modEventBus);

        // 2. 先注册创造模式选项卡（参考 Create 模组的顺序）
        // DeferredHolder 的 lambda 表达式是延迟执行的，所以不会立即访问依赖项
        CreativeTabs.register(modEventBus);

        // 3. 触发类加载（静态字段初始化）
        AllBlocks.register();
        AllItems.register();
    }
}
