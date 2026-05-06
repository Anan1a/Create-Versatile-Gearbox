package com.anan1a.create_versatile_gearbox;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
    // 创建一个 Deferred Register 来保存方块，所有方块将注册到 "examplemod" 命名空间下
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // 创建一个 Deferred Register 来保存物品，所有物品将注册到 "examplemod" 命名空间下
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // 创建一个 Deferred Register 来保存创造模式选项卡，所有选项卡将注册到 "examplemod" 命名空间下
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 创建一个 ID 为 "examplemod:example_block" 的新方块，结合了命名空间和路径
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // 创建一个 ID 为 "examplemod:example_block" 的新方块物品，结合了命名空间和路径
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // 创建一个 ID 为 "examplemod:example_id" 的新食物物品，营养值为 1，饱和度倍率为 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // 创建一个 ID 为 "examplemod:example_tab" 的创造模式选项卡，用于放置示例物品，位于战斗选项卡之后
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.examplemod")) // 创造模式选项卡标题的语言键
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // 将示例物品添加到选项卡中。对于你自己的选项卡，此方法优于事件
            }).build());

    // 模组类的构造函数是模组加载时运行的第一段代码。
    // FML 会识别某些参数类型（如 IEventBus 或 ModContainer）并自动传入。
    public CreateVersatileGearbox(IEventBus modEventBus, ModContainer modContainer) {
        // 注册 commonSetup 方法用于模组加载
        modEventBus.addListener(this::commonSetup);

        // 将 Deferred Register 注册到模组事件总线，以便方块被注册
        BLOCKS.register(modEventBus);
        // 将 Deferred Register 注册到模组事件总线，以便物品被注册
        ITEMS.register(modEventBus);
        // 将 Deferred Register 注册到模组事件总线，以便选项卡被注册
        CREATIVE_MODE_TABS.register(modEventBus);

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
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    // 你可以使用 SubscribeEvent 并让事件总线发现要调用的方法
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 当服务器启动时执行某些操作
        LOGGER.info("来自服务器启动的问候");
    }
}
