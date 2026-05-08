package com.anan1a.create_versatile_gearbox.data;

import java.util.function.BiConsumer;

import com.anan1a.create_versatile_gearbox.Registers;
import com.tterrag.registrate.providers.ProviderType;

import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 数据生成器注册类
 * 负责注册各种数据生成提供者
 * 
 * 注意：方块和物品的名称会由 Registrate 自动生成，无需手动添加
 * 自动生成规则：将注册名称的下划线替换为空格，首字母大写
 * 例如: versatile_gearbox -> Versatile Gearbox
 */
public class VersatileGearboxDatagen {

    public static void gatherData(GatherDataEvent event) {
        // 添加语言文件生成器
        Registers.registrate().addDataGenerator(ProviderType.LANG, provider -> {
            BiConsumer<String, String> add = provider::add;
            
            // ========== 创造模式选项卡 ==========
            add.accept("itemGroup.create_versatile_gearbox.versatile_gearbox_tab", "Versatile Gearbox");
            
            // ========== 配置界面 ==========
            add.accept("create_versatile_gearbox.configuration.title", "Versatile Gearbox Configuration");
            add.accept("create_versatile_gearbox.configuration.section.create_versatile_gearbox.common.toml", "Common Configuration");
            add.accept("create_versatile_gearbox.configuration.section.create_versatile_gearbox.common.toml.title", "Common Configuration");
            
            // ========== 配置选项: Log Dirt Block ==========
            add.accept("create_versatile_gearbox.configuration.logDirtBlock", "Log Dirt Block");
            add.accept("create_versatile_gearbox.configuration.logDirtBlock.tooltip", "Whether to log the dirt block on common setup");
            
            // ========== 配置选项: Magic Number ==========
            add.accept("create_versatile_gearbox.configuration.magicNumber", "Magic Number");
            add.accept("create_versatile_gearbox.configuration.magicNumber.tooltip", "A magic number");
            
            // ========== 配置选项: Magic Number Introduction ==========
            add.accept("create_versatile_gearbox.configuration.magicNumberIntroduction", "Magic Number Introduction Text");
            add.accept("create_versatile_gearbox.configuration.magicNumberIntroduction.tooltip", "What do you want the magic number introduction to say");
            
            // ========== 配置选项: Item List ==========
            add.accept("create_versatile_gearbox.configuration.items", "Item List");
            add.accept("create_versatile_gearbox.configuration.items.tooltip", "A list of items to log during common setup");
        });
    }
}