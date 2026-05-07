package com.anan1a.create_versatile_gearbox;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CreativeTabs {
    // 创建一个 Deferred Register 来保存创造模式选项卡
    private static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateVersatileGearbox.MODID);

    // 创建一个 ID 为 "create_versatile_gearbox:example_tab" 的创造模式选项卡
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = REGISTER.register("example_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_versatile_gearbox_tab"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> AllItems.EXAMPLE_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // 直接使用物品实例
                        output.accept(AllItems.EXAMPLE_ITEM.get());
                    })
                    .build());

    // 注册方法
    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
