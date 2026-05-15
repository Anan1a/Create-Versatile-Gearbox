package com.anan1a.create_versatile_gearbox.data;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import java.util.Map.Entry;
import java.util.function.BiConsumer;

import com.anan1a.create_versatile_gearbox.Registers;
import com.anan1a.create_versatile_gearbox.ponder.CVGPonderPlugin;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.utility.FilesHelper;
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
            
            // ========== 从默认语言文件加载翻译 ==========
            // 参考 Create 原版做法：使用 default/interface.json 管理界面文本
            provideDefaultLang("interface", add);
            
            // ========== Ponder 场景翻译（自动生成） ==========
            // Ponder 会自动从场景代码中提取文本并生成翻译键
            // 格式：{modid}.ponder.{scene_id}.header 和 .text_N
            PonderIndex.addPlugin(new CVGPonderPlugin());
            PonderIndex.getLangAccess().provideLang(MODID, add);
        });
    }

    /**
     * 从默认语言文件加载翻译
     * <p>
     * 参考 Create 原版的实现方式
     * 文件路径：assets/{modid}/lang/default/{fileName}.json
     *
     * @param fileName 文件名（不含扩展名）
     * @param consumer 语言消费者
     */
    private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
        String path = "assets/" + MODID + "/lang/default/" + fileName + ".json";
        JsonElement jsonElement = FilesHelper.loadJsonResource(path);
        if (jsonElement == null) {
            throw new IllegalStateException(String.format("Could not find default lang file: %s", path));
        }
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            consumer.accept(key, value);
        }
    }
}