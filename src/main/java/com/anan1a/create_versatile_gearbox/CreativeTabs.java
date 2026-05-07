package com.anan1a.create_versatile_gearbox;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.CreativeModeTabs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

/**
 * 创造模式选项卡注册类
 * <p>
 * 使用 DeferredRegister 注册创造模式选项卡
 * 通过 RegistrateDisplayItemsGenerator 自动收集关联的物品
 */
public class CreativeTabs {
    /**
     * DeferredRegister 实例，用于注册创造模式选项卡
     */
    private static final DeferredRegister<CreativeModeTab> REGISTER =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateVersatileGearbox.MODID);

    /**
     * 示例创造模式选项卡
     * <p>
     * 注册 ID: create_versatile_gearbox:example_tab
     * <p>
     * 配置说明：
     * - title: 使用翻译键 "itemGroup.create_versatile_gearbox_tab"
     * - withTabsBefore: 显示在战斗选项卡之前
     * - icon: 使用 EXAMPLE_ITEM 作为图标
     * - displayItems: 使用 RegistrateDisplayItemsGenerator 自动收集物品
     */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = REGISTER.register("example_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_versatile_gearbox_tab"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> AllItems.EXAMPLE_ITEM.get().getDefaultInstance())
                    // 使用 Supplier 延迟获取选项卡引用，避免初始化顺序问题
                    .displayItems(new RegistrateDisplayItemsGenerator(() -> CreativeTabs.EXAMPLE_TAB))
                    .build());

    /**
     * 注册方法
     * <p>
     * 将创造模式选项卡注册到事件总线
     * 必须在方块和物品注册之前调用
     *
     * @param modEventBus 模组事件总线
     */
    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }

    /**
     * 参考 Create 模组的 RegistrateDisplayItemsGenerator 实现
     * 用于自动收集注册时关联到特定创造模式选项卡的所有物品
     * 
     * <p>工作原理：
     * 1. 通过 CreateRegistrate.setCreativeTab() 设置物品与选项卡的关联
     * 2. 该类在选项卡显示时自动收集所有关联的物品
     * 3. 使用 Supplier 延迟获取选项卡引用，避免初始化顺序问题
     * 
     * @param tabFilterSupplier 目标选项卡的延迟引用提供者
     */
    private record RegistrateDisplayItemsGenerator(
            java.util.function.Supplier<DeferredHolder<CreativeModeTab, CreativeModeTab>> tabFilterSupplier
    ) implements DisplayItemsGenerator {

        @Override
        public void accept(ItemDisplayParameters parameters, Output output) {
            // 获取目标选项卡（延迟获取，避免初始化顺序问题）
            var targetTab = tabFilterSupplier.get();

            // ========== 收集方块物品 ==========
            // 遍历所有注册的方块，过滤出关联到当前选项卡的方块
            // 转换为对应的物品形式（BlockItem）并输出
            Registers.registrate().getAll(Registries.BLOCK)
                    .stream()
                    .filter(blockEntry -> CreateRegistrate.isInCreativeTab(blockEntry, targetTab))
                    .map(blockEntry -> blockEntry.get().asItem())
                    .filter(item -> item != net.minecraft.world.item.Items.AIR)
                    .forEach(output::accept);

            // ========== 收集物品 ==========
            // 遍历所有注册的物品，过滤出关联到当前选项卡且不是 BlockItem 的物品
            // （BlockItem 已经在上方处理过，避免重复）
            Registers.registrate().getAll(Registries.ITEM)
                    .stream()
                    .filter(itemEntry -> CreateRegistrate.isInCreativeTab(itemEntry, targetTab))
                    .map(RegistryEntry::get)
                    .filter(item -> !(item instanceof BlockItem))
                    .forEach(output::accept);
        }
    }
}
