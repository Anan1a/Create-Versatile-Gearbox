package com.anan1a.create_versatile_gearbox.data;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import java.util.Map.Entry;
import java.util.function.BiConsumer;

import com.anan1a.create_versatile_gearbox.Registers;
import com.anan1a.create_versatile_gearbox.CVGPonderPlugin;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.ProviderType;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 数据生成入口
 * <p>
 * 响应 GatherDataEvent，向 Registrate 注册 LANG 生成器，
 * 从 {@code assets/{modid}/lang/default/} 加载界面翻译，
 * 并从 Ponder 场景代码中提取教学文本。
 */
public class VersatileGearboxDatagen {

    /**
     * 数据生成入口。
     * <p>
     * 通过 {@link GatherDataEvent} 向 Registrate 注册一个 LANG 数据生成器。
     * 该生成器收集两类翻译：① {@code assets/{modid}/lang/default/} 中的静态 JSON 文本；
     * ② Ponder 场景代码中用到的教学文本。
     *
     * @param event NeoForge 数据收集事件
     */
    public static void gatherData(GatherDataEvent event) {
        // 守卫检查：仅当本模组被请求生成数据时才执行，避免跨模组事件干扰
        if (!event.getMods().contains(MODID))
            return;

        Registers.registrate().addDataGenerator(ProviderType.LANG, provider -> {
            BiConsumer<String, String> add = provider::add;

            // 加载静态界面/配置文本
            provideDefaultLang("interface", add);

            // 提取 Ponder 场景中的翻译键
            providePonderLang(add);
        });
    }

    /**
     * 从 {@code assets/{modid}/lang/default/{fileName}.json} 加载翻译键值对。
     * <p>
     * 参考 Create 原版做法，将界面/配置类文本单独管理，与 Ponder 自动文本分离。
     *
     * @param fileName 文件名（不含 {@code .json} 后缀）
     * @param consumer 翻译键-值消费者
     */
    private static void provideDefaultLang(String fileName, BiConsumer<String, String> consumer) {
        // 构建默认语言文件路径：assets/{modid}/lang/default/{fileName}.json
        String path = "assets/" + MODID + "/lang/default/" + fileName + ".json";
        
        // 使用 Create 的 FilesHelper 加载资源（支持 JAR 内/外部文件）
        JsonElement jsonElement = FilesHelper.loadJsonResource(path);
        
        // 文件不存在时抛异常，强制开发者补全必要的翻译文件
        if (jsonElement == null) {
            throw new IllegalStateException("Could not find default lang file: " + path);
        }
        
        // 解析为 JsonObject 并遍历所有键值对
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            // 类型安全检查：仅处理字符串值，跳过嵌套对象
            if (entry.getValue().isJsonPrimitive()) {
                consumer.accept(key, entry.getValue().getAsString());
            }
        }
    }

    /**
     * 注册 Ponder 插件并提取场景中的翻译文本。
     * <p>
     * Ponder 场景中使用的文本（如标题、步骤说明）通过此方法自动收集，
     * 键格式为 {@code {modid}.ponder.{scene_id}.header} 和 {@code .text_N}。
     */
    private static void providePonderLang(BiConsumer<String, String> consumer) {
        // 注册本模组的 Ponder 插件（由于 datagen 阶段不运行 FMLClientSetupEvent，需手动注册）
        PonderIndex.addPlugin(new CVGPonderPlugin());
        
        // 从 Ponder 索引中提取本模组场景的翻译键
        // 自动收集场景标题（.header）和步骤文本（.text_N）
        PonderIndex.getLangAccess().provideLang(MODID, consumer);
    }
}