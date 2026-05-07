package com.anan1a.create_versatile_gearbox;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * 模组主类，是整个模组的入口点
 * <p>
 * 标记为 @Mod 注解，FML 会自动识别并加载此类
 * MODID 必须与 META-INF/neoforge.mods.toml 文件中的条目匹配
 */
@Mod(CreateVersatileGearbox.MODID)
public class CreateVersatileGearbox {
    /**
     * 模组唯一标识符（MODID）
     * 在整个模组中统一引用，避免硬编码
     */
    public static final String MODID = "create_versatile_gearbox";

    /**
     * 日志记录器实例
     * 使用 slf4j 框架，用于输出调试和运行时信息
     */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 模组构造函数 - 模组加载时最先执行的代码
     * <p>
     * FML 会自动注入所需的参数：
     * - IEventBus: 模组事件总线，用于注册监听器
     * - ModContainer: 模组容器，用于配置等操作
     *
     * @param modEventBus 模组事件总线
     * @param modContainer 模组容器
     */
    public CreateVersatileGearbox(IEventBus modEventBus, ModContainer modContainer) {
        // 注册 commonSetup 方法，在模组加载阶段执行
        modEventBus.addListener(this::commonSetup);

        // 调用统一的注册方法，注册所有方块、物品和创造模式选项卡
        // 详见 Registers.java 的 registerAll() 方法
        Registers.registerAll(modEventBus);

        // 注册到 NeoForge 事件总线，以便响应服务器事件
        // 注意：仅当需要在此类中处理 @SubscribeEvent 注解的方法时才需要
        NeoForge.EVENT_BUS.register(this);

        // 注册模组配置文件
        // Config.SPEC 定义了配置的结构和默认值
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /**
     * 模组通用设置方法
     * <p>
     * 在 FMLCommonSetupEvent 阶段执行，用于初始化通用逻辑
     *
     * @param event FML 通用设置事件
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        // 输出初始化日志
        LOGGER.info("Create Versatile Gearbox 模组初始化完成");

        // 示例：根据配置决定是否输出泥土块的注册信息
        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("泥土块注册键 >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        // 示例：输出配置中的魔法数字
        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        // 示例：遍历并输出配置中的物品字符串列表
        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("配置物品 >> {}", item));
    }

    /**
     * 服务器启动事件监听器
     * <p>
     * 使用 @SubscribeEvent 注解，当服务器启动时自动触发
     *
     * @param event 服务器启动事件
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 服务器启动时输出日志
        LOGGER.info("Create Versatile Gearbox 服务器启动完成");
    }
}
