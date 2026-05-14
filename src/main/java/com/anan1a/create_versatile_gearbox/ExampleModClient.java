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

// 该类不会在专用服务器上加载。从此处访问客户端代码是安全的。
@Mod(value = CreateVersatileGearbox.MODID, dist = Dist.CLIENT)
// 你可以使用 EventBusSubscriber 自动注册该类中所有带有 @SubscribeEvent 注解的静态方法
@EventBusSubscriber(modid = CreateVersatileGearbox.MODID, value = Dist.CLIENT)
public class ExampleModClient {
    public ExampleModClient(ModContainer container) {
        // 允许 NeoForge 为此模组的配置创建一个配置屏幕。
        // 配置屏幕的访问方式：进入模组屏幕 > 点击你的模组 > 点击配置。
        // 不要忘记在 en_us.json 文件中为你的配置选项添加翻译。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // 一些客户端设置代码
        CreateVersatileGearbox.LOGGER.info("Greetings from client setup");
        CreateVersatileGearbox.LOGGER.info("Minecraft username >> {}", Minecraft.getInstance().getUser().getName());
    }
}