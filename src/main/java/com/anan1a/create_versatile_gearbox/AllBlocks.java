package com.anan1a.create_versatile_gearbox;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AllBlocks {
    // 创建一个 Deferred Register 来保存方块，所有方块将注册到 "create_versatile_gearbox" 命名空间下
    public static final DeferredRegister.Blocks BLOCKS = Registers.BLOCKS;
    // 创建一个 Deferred Register 来保存物品，所有物品将注册到 "create_versatile_gearbox" 命名空间下
    public static final DeferredRegister.Items ITEMS = Registers.ITEMS;

    // 创建一个 ID 为 "create_versatile_gearbox:example_block" 的新方块，结合了命名空间和路径
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // 创建一个 ID 为 "create_versatile_gearbox:example_block" 的新方块物品，结合了命名空间和路径
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", AllBlocks.EXAMPLE_BLOCK);


    // 注册方块
//    public static void register(IEventBus modEventBus) {
//        BLOCKS.register(modEventBus);
//    }
}