package com.anan1a.create_versatile_gearbox;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

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
                    .displayItems(new RegistrateDisplayItemsGenerator(() -> CreativeTabs.EXAMPLE_TAB))
                    .build());

    // 注册方法
    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }

    // 参考 Create 模组的 RegistrateDisplayItemsGenerator
    private static class RegistrateDisplayItemsGenerator implements DisplayItemsGenerator {
        private final java.util.function.Supplier<DeferredHolder<CreativeModeTab, CreativeModeTab>> tabFilterSupplier;

        public RegistrateDisplayItemsGenerator(java.util.function.Supplier<DeferredHolder<CreativeModeTab, CreativeModeTab>> tabFilterSupplier) {
            this.tabFilterSupplier = tabFilterSupplier;
        }

        private DeferredHolder<CreativeModeTab, CreativeModeTab> getTabFilter() {
            return tabFilterSupplier.get();
        }

        @Override
        public void accept(ItemDisplayParameters parameters, Output output) {
            List<Item> items = new ArrayList<>();

            // 收集方块物品
            for (RegistryEntry<Block, Block> entry : Registers.registrate().getAll(Registries.BLOCK)) {
                if (!CreateRegistrate.isInCreativeTab(entry, getTabFilter()))
                    continue;
                Item item = entry.get().asItem();
                if (item != net.minecraft.world.item.Items.AIR)
                    items.add(item);
            }

            // 收集非方块物品
            for (RegistryEntry<Item, Item> entry : Registers.registrate().getAll(Registries.ITEM)) {
                if (!CreateRegistrate.isInCreativeTab(entry, getTabFilter()))
                    continue;
                Item item = entry.get();
                if (!(item instanceof BlockItem))
                    items.add(item);
            }

            // 输出所有物品
            for (Item item : items) {
                output.accept(item);
            }
        }
    }
}
