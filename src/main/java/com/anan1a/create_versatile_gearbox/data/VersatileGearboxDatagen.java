package com.anan1a.create_versatile_gearbox.data;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import java.util.function.BiConsumer;

import com.anan1a.create_versatile_gearbox.Registers;
import com.anan1a.create_versatile_gearbox.ponder.CVGPonderPlugin;
import com.tterrag.registrate.providers.ProviderType;

import net.createmod.ponder.foundation.PonderIndex;
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
            
            // ========== 创造模式物品栏 ==========
            add.accept("itemGroup." + MODID + ".versatile_gearbox_tab", "Versatile Gearbox");

            // ========== 配置界面 ==========
            add.accept(MODID + ".configuration.title", "Versatile Gearbox Configuration");
            add.accept(MODID + ".configuration.section." + MODID + ".common.toml", "Common Configuration");
            add.accept(MODID + ".configuration.section." + MODID + ".common.toml.title", "Common Configuration");
            
            // ========== 配置项：记录泥土方块 ==========
            add.accept(MODID + ".configuration.logDirtBlock", "Log Dirt Block");
            add.accept(MODID + ".configuration.logDirtBlock.tooltip", "Whether to log the dirt block on common setup");
            
            // ========== 配置项：魔法数字 ==========
            add.accept(MODID + ".configuration.magicNumber", "Magic Number");
            add.accept(MODID + ".configuration.magicNumber.tooltip", "A magic number");
            
            // ========== 配置项：魔法数字介绍 ==========
            add.accept(MODID + ".configuration.magicNumberIntroduction", "Magic Number Introduction Text");
            add.accept(MODID + ".configuration.magicNumberIntroduction.tooltip", "What do you want the magic number introduction to say");
            
            // ========== 配置项：物品列表 ==========
            add.accept(MODID + ".configuration.items", "Item List");
            add.accept(MODID + ".configuration.items.tooltip", "A list of items to log during common setup");
            
            // ========== Ponder 场景翻译（自动生成） ==========
            // Ponder 会自动从场景代码中提取文本并生成翻译键
            // 格式：{modid}.ponder.{scene_id}.header 和 .text_N
            PonderIndex.addPlugin(new CVGPonderPlugin());
            PonderIndex.getLangAccess().provideLang(MODID, add);
        });
    }
}