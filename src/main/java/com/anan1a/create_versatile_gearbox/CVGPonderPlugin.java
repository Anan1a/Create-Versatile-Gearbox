package com.anan1a.create_versatile_gearbox;

import com.anan1a.create_versatile_gearbox.ponder.VersatileGearboxScenes;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Versatile Gearbox Ponder 插件
 * <p>
 * 作用：注册多功能齿轮箱的思索场景（Ponder）
 * <p>
 * 工作原理：
 * - Ponder 是 Create 模组的可视化教学系统
 * - 玩家在游戏中右键点击方块时，可以查看交互式教程
 * - 本插件负责告诉 Ponder：有哪些场景、如何展示、属于哪个分类
 * <p>
 * 注册方式：手动注册（在 CreateVersatileGearboxClient 中调用）
 * <p>
 * 类比：就像一个图书管理员，告诉 Ponder "我这里有两本书可以借"
 */
public class CVGPonderPlugin implements PonderPlugin {
    /**
     * 返回模组 ID
     * <p>
     * 作用：告诉 Ponder 这个插件属于哪个模组
     * 用途：Ponder 内部用于管理多个模组的场景
     * <p>
     *
     * @return 模组 ID（create_versatile_gearbox）
     */
    @Override
    public @NotNull String getModId() {
        return CreateVersatileGearbox.MODID;
    }

    /**
     * 注册思索场景
     * <p>
     * 作用：告诉 Ponder 有哪些教学场景可以展示
     * <p>
     * 工作原理：
     * 1. 转换 helper 类型，方便使用 Registrate 的 RegistryEntry
     * 2. 为 VERSATILE_GEARBOX 方块注册两个场景：
     *    - 场景 1（usage）：基本用法演示
     *    - 场景 2（shaft_control）：轴控制演示
     * <p>
     * addStoryboard 参数说明：
     * - 第一个参数：NBT 文件路径（assets/模组ID/ponder/路径.nbt）
     * - 第二个参数：场景执行方法（方法引用）
     * - 第三个参数：场景标签（可选，用于分类，如动力中继、动力源等）
     *
     * @param helper Ponder 场景注册助手
     */
    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // 转换 helper 类型，使其支持 Registrate 的 RegistryEntry
        // withKeyFunction 指定如何从 RegistryEntry 获取 ResourceLocation
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> SCENE_HELPER = helper.withKeyFunction(RegistryEntry::getId);

        // 为多功能齿轮箱注册思索场景
        // forComponents 指定哪些方块有这个场景
        SCENE_HELPER.forComponents(CVGBlocks.VERSATILE_GEARBOX)
                // 场景 1：基本用法（归类到动力中继标签）
                .addStoryBoard("versatile_gearbox/usage", 
                               VersatileGearboxScenes::versatileGearboxUsage,
                               AllCreatePonderTags.KINETIC_RELAYS)
                // 场景 2：轴控制（无标签，只出现在方块专属场景中）
                .addStoryBoard("versatile_gearbox/shaft_control", 
                               VersatileGearboxScenes::versatileGearboxShaftControl);
    }

    /**
     * 注册场景标签
     * <p>
     * 作用：将方块添加到 Ponder 的分类标签中
     * <p>
     * 工作原理：
     * - Ponder 界面有多个分类（如动力源、动力中继、物流等）
     * - 通过 addToTag 将方块归类，玩家可以在对应分类中找到它
     * - 类似于图书馆的图书分类系统
     * <p>
     * 当前分类：
     * - KINETIC_RELAYS（动力中继）：齿轮箱属于传输动力的中继方块
     *
     * @param helper Ponder 标签注册助手
     */
    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        // 转换 helper 类型，使其支持 Registrate 的 RegistryEntry
        PonderTagRegistrationHelper<RegistryEntry<?, ?>> TAG_HELPER = helper.withKeyFunction(RegistryEntry::getId);

        // 将多功能齿轮箱添加到 "动力中继" 分类标签
        // 这样玩家可以在 Ponder 界面的 "动力中继" 分类中找到它
        TAG_HELPER.addToTag(AllCreatePonderTags.KINETIC_RELAYS)
                .add(CVGBlocks.VERSATILE_GEARBOX);
    }
}
