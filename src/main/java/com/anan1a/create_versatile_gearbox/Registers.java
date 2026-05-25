package com.anan1a.create_versatile_gearbox;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.simibubi.create.foundation.data.CreateRegistrate;

/**
 * 模组注册管理类
 * <p>
 * 负责管理所有注册相关的逻辑，包括：
 * - CreateRegistrate 实例的创建和配置
 * - 统一的注册入口
 * - 注册顺序的控制
 */
public class Registers {
    /**
     * 模组 ID，引用自主类，确保一致性
     */
    public static final String MODID = CreateVersatileGearbox.MODID;

    /**
     * 主创造模式选项卡的 ResourceKey
     * <p>
     * 唯一标识符，用于在 Registrate 初始化时引用选项卡
     * 确保即使选项卡还未注册也能安全使用
     */
    public static final ResourceKey<CreativeModeTab> VERSATILE_GEARBOX_TAB_KEY =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath(MODID, "versatile_gearbox_tab"));

    /**
     * CreateRegistrate 实例
     * <p>
     * Create 模组提供的注册工具，简化方块、方块实体、物品等的注册流程
     * 直接设置 defaultCreativeTab 为 MAIN_TAB_KEY，实现自动关联
     * 采用 Create 官方风格的注册流程
     */
    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .defaultCreativeTab(VERSATILE_GEARBOX_TAB_KEY);

    /**
     * 创造模式选项卡的 DeferredRegister 实例
     * <p>
     * 统一管理所有创造模式选项卡的注册
     * 使用 Registries.CREATIVE_MODE_TAB 作为注册表类型
     * MODID 作为命名空间
     */
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /**
     * 获取 Registrate 实例
     * <p>
     * 供其他注册类（如 CVGBlocks、CVGItems）使用
     *
     * @return CreateRegistrate 实例
     */
    public static CreateRegistrate registrate() {
        return REGISTRATE;
    }

    /**
     * 获取创造模式选项卡的 DeferredRegister 实例
     * <p>
     * 供 CreativeTabs 类使用，实现注册逻辑的集中管理
     *
     * @return 创造模式选项卡的 DeferredRegister 实例
     */
    public static DeferredRegister<CreativeModeTab> creativeModeTabs() {
        return CREATIVE_MODE_TABS;
    }

    /**
     * 统一注册入口
     * <p>
     * 按照正确的顺序注册所有模组内容：
     * 1. 注册 Registrate 事件监听器
     * 2. 注册创造模式选项卡（必须先于方块/物品注册）
     * 3. 触发方块和物品类的静态初始化（注册实际内容）
     *
     * @param modEventBus 模组事件总线
     */
    public static void registerAll(IEventBus modEventBus) {
        // 步骤1：注册 Registrate 事件监听器
        // 必须在选项卡和方块注册之前
        REGISTRATE.registerEventListeners(modEventBus);

        // 步骤2：注册创造模式选项卡
        CreativeTabs.register(modEventBus);

        // 步骤3：触发方块类加载
        CVGBlocks.register();

        // 步骤4：注册方块实体类型
        CVGBlockEntityTypes.register();

        // 步骤5：触发物品类加载
        CVGItems.register();
    }
}
