package com.anan1a.create_versatile_gearbox;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// 此处的值应与 META-INF/neoforge.mods.toml 文件中的条目匹配
@Mod(CreateVersatileGearbox.MODID)
public class CreateVersatileGearbox {
    // 在一个公共位置定义模组 ID，方便所有地方引用
    public static final String MODID = "create_versatile_gearbox";
    // 直接引用 slf4j 日志记录器
    public static final Logger LOGGER = LogUtils.getLogger();

    // 模组类的构造函数是模组加载时运行的第一段代码。
    // FML 会识别某些参数类型（如 IEventBus 或 ModContainer）并自动传入。
    public CreateVersatileGearbox(IEventBus modEventBus, ModContainer modContainer) {
        // 注册 commonSetup 方法用于模组加载
        modEventBus.addListener(this::commonSetup);

        // 调用 Registers 类中的静态方法，注册所有方块和物品
        Registers.registerAll(modEventBus);

        // 将我们自己注册到服务器和其他我们感兴趣的游戏事件。
        // 注意：仅当我们需要 *这个* 类（ExampleMod）直接响应事件时才需要这样做。
        // 如果此类中没有 @SubscribeEvent 注解的函数（如下面的 onServerStarting），请不要添加这一行。
        NeoForge.EVENT_BUS.register(this);

        // 将物品注册到创造模式选项卡
        modEventBus.addListener(this::addCreative);

        // 注册我们模组的 ModConfigSpec，以便 FML 可以为我们创建和加载配置文件
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 一些通用设置代码
        LOGGER.info("来自通用设置的问候");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("泥土块 >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("物品 >> {}", item));
    }

    // 将示例方块物品添加到建筑方块选项卡
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        CreativeTabs.addCreative(event);
    }

    // 你可以使用 SubscribeEvent 并让事件总线发现要调用的方法
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 当服务器启动时执行某些操作
        LOGGER.info("来自服务器启动的问候");
    }
}
