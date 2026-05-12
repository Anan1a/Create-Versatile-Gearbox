package com.anan1a.create_versatile_gearbox;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTabs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.tterrag.registrate.util.entry.ItemProviderEntry;

/**
 * 创造模式选项卡注册类
 * <p>
 * 使用手动注册列表的方式，参考 create_connected 模组
 */
public class CreativeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER = Registers.creativeModeTabs();

    /**
     * 手动注册的物品列表
     * <p>
     * 按顺序列出所有需要显示在选项卡中的物品/方块
     * 方块使用 XXBlocks.XX.asStack()，物品使用 XXItems.XX
     */
    public static final List<ItemProviderEntry<?, ?>> ITEMS = List.of(
            // 示例方块
            AllBlocks.EXAMPLE_BLOCK,
            
            // 多功能传动箱相关
            AllBlocks.VERSATILE_GEARBOX,

            // 示例物品
            AllItems.EXAMPLE_ITEM
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = REGISTER.register("versatile_gearbox_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_versatile_gearbox.versatile_gearbox_tab"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> AllItems.EXAMPLE_ITEM.asItem().getDefaultInstance())
                    .displayItems(new SimpleDisplayItemsGenerator(ITEMS))
                    .build());

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }

    /**
     * 简单的物品显示生成器
     * <p>
     * 遍历 ITEMS 列表并输出到选项卡
     */
    private record SimpleDisplayItemsGenerator(
            List<ItemProviderEntry<?, ?>> items
    ) implements DisplayItemsGenerator {

        @Override
        public void accept(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output) {
            for (ItemProviderEntry<?, ?> item : items) {
                output.accept(item);
            }
        }
    }
}
