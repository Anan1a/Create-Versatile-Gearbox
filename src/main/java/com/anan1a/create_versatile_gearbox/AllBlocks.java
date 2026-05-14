package com.anan1a.create_versatile_gearbox;

import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.ShaftState;
import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VersatileGearboxBlock;
import com.anan1a.create_versatile_gearbox.foundation.AllModSpriteShifts;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.simibubi.create.api.stress.BlockStressValues;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.foundation.data.AssetLookup;

import static com.simibubi.create.foundation.data.BlockStateGen.axisBlock;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

/**
 * 方块注册类
 * <p>
 * 使用 CreateRegistrate 进行方块注册，支持：
 * - 自动关联创造模式选项卡
 * - 自动生成 BlockItem
 * - 简化的注册流程
 */
public class AllBlocks {
    /**
     * CreateRegistrate 实例，用于注册方块
     */
    private static final CreateRegistrate REGISTRATE = Registers.registrate();

//    /**
//     * 示例方块
//     * <p>
//     * 注册 ID: create_versatile_gearbox:example_block
//     * <p>
//     * 使用 simpleItem() 自动生成对应的 BlockItem
//     * 方块颜色设置为石色（MapColor.STONE）
//     * 自动关联到 EXAMPLE_TAB 创造模式选项卡
//     */
//    public static final BlockEntry<Block> EXAMPLE_BLOCK = REGISTRATE
//            .block("example_block", Block::new)  // 注册名为 "example_block" 的方块，使用默认 Block 构造函数
//            .properties(p -> p                     // 配置方块属性
//				.mapColor(MapColor.STONE)           // 地图颜色为石色（影响地图渲染和泥土颜色判定）
//				.noOcclusion()                      // 禁用遮挡，使方块允许光线和物体穿过透明像素
//			)
//			.blockstate((c, p) -> p.simpleBlock(       // 自定义方块状态生成
//				c.getEntry(),                           // 获取当前注册的方块
//				p.models()                              // 获取模型生成器
//					.cubeAll(                          // 创建一个 cube_all 父模型的方块模型
//						"example_block",                // 模型名称（生成文件: example_block.json）
//						p.modLoc("block/example_block")// 纹理路径: create_versatile_gearbox:block/example_block
//					)
//					.renderType("translucent")          // 设置渲染类型为半透明（支持透明通道）
//			))
//            // .item() // 手动配置物品时使用
//            // .tab(CreativeTabs.EXAMPLE_TAB.getKey()) // 手动指定选项卡
//            // .tab(CreativeModeTabs.BUILDING_BLOCKS) // 添加到多个选项卡
//            // .build()
//            .simpleItem()  // 简化的物品注册，自动生成 BlockItem
//            .register();

	/**
	 * Versatile Gearbox - 多功能齿轮箱
	 * <p>
	 * 支持六面独立轴状态控制的核心方块
	 * 具备动态纹理渲染，根据状态动态生成纹理
	 * 和连接纹理功能，与安山合金机壳兼容
	 */
	public static final BlockEntry<VersatileGearboxBlock> VERSATILE_GEARBOX = REGISTRATE
            .block("versatile_gearbox", VersatileGearboxBlock::new)

			// ========== 基础属性配置 ==========
			.initialProperties(SharedProperties::stone)// 基础属性，这里设置为石色
			
			// ========== 额外属性配置 ==========
			.properties(p -> p
				.mapColor(MapColor.PODZOL)  // 地图颜色
				.noOcclusion()           // 禁用遮挡
			)
			
			// ========== 应力配置 ==========
			// 齿轮箱应力影响 - 使用 Create 开放的 API
			.onRegister(block -> BlockStressValues.IMPACTS.register(block, () -> 0))
			
			// ========== 采集工具配置 ==========
			// 可以用斧头或镐子采集
			.transform(axeOrPickaxe())
			
			// ========== 连接纹理配置 ==========
			// 为自定义机壳和安山合金机壳注册连接纹理行为
			// EncasedCTBehaviour 根据相邻方块状态处理纹理渲染
			.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllModSpriteShifts.VERSATILE_GEARBOX_OFF)))
			.onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(AllSpriteShifts.ANDESITE_CASING)))
									
			// ========== 外壳连接性规则 ==========
			// 定义连接条件：仅当轴状态为 OFF 时面才连接
			// 防止轴激活时出现视觉错误
			.onRegister(CreateRegistrate.casingConnectivity((block, cc) -> {
				cc.make(block, AllModSpriteShifts.VERSATILE_GEARBOX_OFF,
						(s, f) -> VersatileGearboxBlock.getShaftState(f, s) == ShaftState.OFF);
				cc.make(block, AllSpriteShifts.ANDESITE_CASING,
						(s, f) -> VersatileGearboxBlock.getShaftState(f, s) == ShaftState.OFF);
			}))

			// ========== 扳手黑名单配置 ==========
			// 将 VersatileGearbox 添加到扳手旋转菜单黑名单，禁止旋转整个方块
			.onRegister(block -> RadialWrenchMenu.registerBlacklistedBlock(BuiltInRegistries.BLOCK.getKey(block)))
			
			// ========== Blockstate 生成 ==========
			// 使用简单 blockstate 避免生成大量 variants（3轴 × 3⁶面状态 = 2187个）
			.blockstate((c, p) -> p.simpleBlock(c.getEntry(), p.models().getExistingFile(p.modLoc("block/versatile_gearbox/block"))))
			
			// ========== 物品配置 ==========
			// customItemModel() 会查找 models/block/versatile_gearbox/item.json
			.item()
			.transform(customItemModel())
			
			// ========== 完成注册 ==========
			.register();

    /**
     * 注册触发方法
     * <p>
     * 空方法，用于触发类加载和静态字段初始化
     * 由 Registers.registerAll() 调用
     */
    public static void register() {}
}