package com.anan1a.create_versatile_gearbox;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;

import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VerticalVersatileGearboxItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

/**
 * 物品注册类
 * <p>
 * 使用 CreateRegistrate 进行物品注册，支持：
 * - 自动关联创造模式选项卡
 * - 简化的属性配置
 * - 统一的注册流程
 */
public class AllItems {
    /**
     * CreateRegistrate 实例，用于注册物品
     */
    private static final CreateRegistrate REGISTRATE = Registers.registrate();

    /*
      静态初始化块
      <p>
      设置此类中所有物品的默认创造模式选项卡
      通过 setCreativeTab() 关联到 CreativeTabs.EXAMPLE_TAB
      所有后续注册的物品都会自动使用此选项卡
     */
    static {
        REGISTRATE.setCreativeTab(CreativeTabs.EXAMPLE_TAB);
    }

    /**
     * 示例物品
     * <p>
     * 注册 ID: create_versatile_gearbox:example_item
     * <p>
     * 配置为食物属性：
     * - alwaysEdible: 可以随时食用
     * - nutrition(1): 恢复 1 点饥饿值
     * - saturationModifier(2f): 饱和度修正值为 2.0
     * 自动关联到 EXAMPLE_TAB 创造模式选项卡
     */
    public static final ItemEntry<Item> EXAMPLE_ITEM = REGISTRATE.item("example_item", Item::new)
            .properties(p -> p.food(new FoodProperties.Builder()
                    .alwaysEdible()
                    .nutrition(1)
                    .saturationModifier(2f)
                    .build()))
            .register();

    /**
     * 垂直多功能传动箱物品
     * <p>
     * 放置时会自动检测周围的旋转方块并设置合适的水平轴方向
     * 使用自定义模型：models/block/versatile_gearbox/item_vertical.json
     */
    public static final ItemEntry<VerticalVersatileGearboxItem> VERTICAL_VERSATILE_GEARBOX = REGISTRATE
            .item("vertical_versatile_gearbox", VerticalVersatileGearboxItem::new)
            .model(AssetLookup.customBlockItemModel("versatile_gearbox", "item_vertical"))
            .register();

    /**
     * 注册触发方法
     * <p>
     * 空方法，用于触发类加载和静态字段初始化
     * 由 Registers.registerAll() 调用
     */
    public static void register() {}
}
