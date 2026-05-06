package com.anan1a.create_versatile_gearbox.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.anan1a.create_versatile_gearbox.CreateVersatileGearbox;

public class ModBlocks {
    // 创建一个 Deferred Register 来保存方块，所有方块将注册到 "create_versatile_gearbox" 命名空间下
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateVersatileGearbox.MODID);

    // 创建一个 ID 为 "create_versatile_gearbox:example_block" 的新方块，结合了命名空间和路径
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block",
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE));

    // 注册方块
    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}