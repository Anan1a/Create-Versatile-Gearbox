package com.anan1a.create_versatile_gearbox;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

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
 * 使用 Create 官方风格的注册方式
 * 通过 ResourceKey 与 Registrate 自动关联
 */
public class CreativeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER = Registers.creativeModeTabs();

    /**
     * 手动注册的物品列表
     * <p>
     * 按顺序列出所有需要显示在选项卡中的物品/方块
     * 方块使用 XXBlocks.XX.asStack()，物品使用 XXItems.XX
     */
//    public static final List<ItemProviderEntry<?, ?>> ITEMS = List.of(
//            // 多功能传动箱相关
////            CVGBlocks.VERSATILE_GEARBOX
//    );

    /**
     * 主创造模式选项卡
     * <p>
     * 注册 ID: create_versatile_gearbox:versatile_gearbox_tab
     * 使用 Registers.MAIN_TAB_KEY 中定义的 ResourceKey 保持一致
     * 实现与 Registrate 的自动关联
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VERSATILE_GEARBOX_TAB = REGISTER.register("versatile_gearbox_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID + ".versatile_gearbox_tab"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(CVGBlocks.VERSATILE_GEARBOX::asStack)
//                    .displayItems(new SimpleDisplayItemsGenerator(ITEMS))
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
