package com.anan1a.create_versatile_gearbox;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 模组客户端专用类，处理所有客户端相关的初始化和配置
 * <p>
 * 使用 @Mod 注解标记为客户端专用模组类（dist = Dist.CLIENT）
 * 该类不会在专用服务器上加载，因此可以安全地访问客户端代码
 */
@Mod(value = CreateVersatileGearbox.MODID, dist = Dist.CLIENT)

/**
 * 使用 EventBusSubscriber 自动注册该类中所有带有 @SubscribeEvent 注解的静态方法
 * 仅订阅客户端事件（value = Dist.CLIENT）
 */
@EventBusSubscriber(modid = CreateVersatileGearbox.MODID, value = Dist.CLIENT)
public class ExampleModClient {

    /**
     * 客户端模组构造函数 - 在客户端模组加载时执行
     * <p>
     * FML 会自动注入 ModContainer 参数，用于注册扩展功能
     *
     * @param container 模组容器，用于注册配置屏幕等扩展点
     */
    public ExampleModClient(ModContainer container) {
        // 允许 NeoForge 为此模组的配置创建一个配置屏幕。
        // 配置屏幕的访问方式：进入模组屏幕 > 点击你的模组 > 点击配置。
        // 不要忘记在 en_us.json 文件中为你的配置选项添加翻译。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * 客户端设置事件处理方法
     * <p>
     * 在 FMLClientSetupEvent 阶段执行，用于初始化客户端特定的功能
     * 使用 @SubscribeEvent 注解自动订阅客户端设置事件
     *
     * @param event FML 客户端设置事件
     */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // 一些客户端设置代码
        CreateVersatileGearbox.LOGGER.info("Greetings from client setup");
        CreateVersatileGearbox.LOGGER.info("Minecraft username >> {}", Minecraft.getInstance().getUser().getName());
    }
}