package com.anan1a.create_versatile_gearbox.data;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

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
            
            // ========== Creative Mode Tab ==========
            add.accept("itemGroup." + MODID + ".versatile_gearbox_tab", "Versatile Gearbox");

            // ========== Configuration UI ==========
            add.accept(MODID + ".configuration.title", "Versatile Gearbox Configuration");
            add.accept(MODID + ".configuration.section." + MODID + ".common.toml", "Common Configuration");
            add.accept(MODID + ".configuration.section." + MODID + ".common.toml.title", "Common Configuration");
            
            // ========== Config Option: Log Dirt Block ==========
            add.accept(MODID + ".configuration.logDirtBlock", "Log Dirt Block");
            add.accept(MODID + ".configuration.logDirtBlock.tooltip", "Whether to log the dirt block on common setup");
            
            // ========== Config Option: Magic Number ==========
            add.accept(MODID + ".configuration.magicNumber", "Magic Number");
            add.accept(MODID + ".configuration.magicNumber.tooltip", "A magic number");
            
            // ========== Config Option: Magic Number Introduction ==========
            add.accept(MODID + ".configuration.magicNumberIntroduction", "Magic Number Introduction Text");
            add.accept(MODID + ".configuration.magicNumberIntroduction.tooltip", "What do you want the magic number introduction to say");
            
            // ========== Config Option: Item List ==========
            add.accept(MODID + ".configuration.items", "Item List");
            add.accept(MODID + ".configuration.items.tooltip", "A list of items to log during common setup");
        });
    }
}