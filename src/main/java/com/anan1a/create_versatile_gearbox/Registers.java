package com.anan1a.create_versatile_gearbox;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import net.neoforged.bus.api.IEventBus;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.neoforged.neoforge.registries.DeferredRegister;

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
     * CreateRegistrate 实例
     * <p>
     * Create 模组提供的注册工具，简化方块、物品等的注册流程
     * 设置 defaultCreativeTab 为 null 是为了避免自动分配到未注册的选项卡导致崩溃
     * 实际的选项卡关联通过 setCreativeTab() 方法在各个注册类中设置
     */
    private static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

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
     * 供其他注册类（如 AllBlocks、AllItems）使用
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
        // 步骤1：注册 Registrate 的事件监听器
        // 这会处理数据生成、模型注册等后续逻辑
        REGISTRATE.registerEventListeners(modEventBus);

        // 步骤2：注册创造模式选项卡
        // 必须先注册选项卡，否则方块/物品无法关联到正确的选项卡
        // 使用 DeferredRegister 延迟注册，lambda 表达式不会立即执行
        CreativeTabs.register(modEventBus);

        // 步骤3：触发静态字段初始化
        // 调用空的 register() 方法会触发类加载，从而执行静态初始化块
        // 静态块中的 setCreativeTab() 和实际注册代码会在此执行
        AllBlocks.register();
        AllItems.register();
    }
}
