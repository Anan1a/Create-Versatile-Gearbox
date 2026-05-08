package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.Map;

import com.anan1a.create_versatile_gearbox.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 垂直多功能传动箱物品
 * <p>
 * 这是一个特殊的 BlockItem，用于放置水平轴向的多功能传动箱。
 * 与普通的齿轮箱物品不同，它会智能检测周围的旋转方块（如传动轴），
 * 自动选择合适的水平轴方向进行放置。
 * <p>
 * 核心功能：
 * - 检测周围四个水平方向是否有旋转方块
 * - 根据检测结果自动选择最合适的轴方向
 * - 如果没有检测到旋转方块，则根据玩家朝向确定轴方向
 */
public class VerticalVersatileGearboxItem extends BlockItem {

	/**
	 * 构造函数
	 * @param builder 物品属性构建器
	 */
	public VerticalVersatileGearboxItem(Properties builder) {
		// 关联到多功能传动箱方块
		super(AllBlocks.VERSATILE_GEARBOX.get(), builder);
	}

	/**
	 * 获取物品描述ID
	 * <p>
	 * 使用独立的翻译键，使垂直齿轮箱显示不同的名称
	 * @return 物品描述翻译键
	 */
	@Override
	public String getDescriptionId() {
		return "item.create_versatile_gearbox.vertical_versatile_gearbox";
	}

	/**
	 * 注册方块映射（空实现）
	 * <p>
	 * 因为方块已经在 AllBlocks 中注册过了，这里不需要再次注册
	 * @param p_195946_1_ 方块到物品的映射表
	 * @param p_195946_2_ 此物品实例
	 */
	@Override
	public void registerBlocks(Map<Block, net.minecraft.world.item.Item> p_195946_1_, net.minecraft.world.item.Item p_195946_2_) {
		// 空实现 - 方块由 AllBlocks 注册
	}

	/**
	 * 放置方块时的自定义逻辑
	 * <p>
	 * 【智能检测流程】
	 * 1. 遍历四个水平方向（EAST, SOUTH, WEST, NORTH）
	 * 2. 检查每个方向的相邻方块是否实现 IRotate 接口
	 * 3. 如果相邻方块有朝向当前位置的传动轴，记录其轴方向
	 * 4. 如果检测到多个不同的轴方向，取消智能选择（避免冲突）
	 * <p>
	 * 【轴方向确定】
	 * - 有唯一匹配轴 → 使用垂直于该轴的水平轴
	 *   （例如：检测到 X 轴传动轴，设置为 Z 轴方向）
	 * - 无匹配或多轴冲突 → 使用玩家朝向的顺时针方向
	 * <p>
	 * 【设计原理】
	 * 当放置齿轮箱时，需要让齿轮箱的传动轴与相邻的传动轴对齐
	 * 例如：相邻方块在 X 轴方向有传动轴，齿轮箱就应该设置为 Z 轴方向，
	 * 这样齿轮箱的前后两面（Z轴方向）就会有传动轴接口
	 * 
	 * @param pos    放置位置
	 * @param world  世界
	 * @param player 玩家
	 * @param stack  物品栈
	 * @param state  方块状态（默认状态）
	 * @return 是否成功更新
	 */
	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, Player player, ItemStack stack, BlockState state) {
		// 存储检测到的优先轴方向（可能为 null）
		Axis preferredAxis = null;
		
		// 遍历四个水平方向（东、南、西、北）
		for (Direction side : Iterate.horizontalDirections) {
			// 获取相邻位置的方块状态
			BlockState blockState = world.getBlockState(pos.relative(side));
			
			// 检查相邻方块是否是可旋转的动力组件（实现 IRotate 接口）
			if (blockState.getBlock() instanceof IRotate) {
				IRotate rotateBlock = (IRotate) blockState.getBlock();
				
				// 检查相邻方块是否有朝向当前位置的传动轴
				// side.getOpposite() 是从相邻方块指向当前位置的方向
				if (rotateBlock.hasShaftTowards(world, pos.relative(side), blockState, side.getOpposite())) {
					// 如果已经找到一个轴，且当前轴与之前的不同
					if (preferredAxis != null && preferredAxis != side.getAxis()) {
						// 多个不同方向的轴，存在冲突，取消智能选择
						preferredAxis = null;
						break;
					} else {
						// 记录当前轴方向
						preferredAxis = side.getAxis();
					}
				}
			}
		}

		// 确定最终的轴方向
		Axis axis;
		if (preferredAxis == null) {
			// 没有检测到旋转方块或存在冲突
			// 使用玩家朝向的顺时针方向作为轴（更符合直觉）
			axis = player.getDirection().getClockWise().getAxis();
		} else {
			// 检测到唯一匹配的轴
			// 设置为垂直于检测轴的水平轴
			// 例如：检测到 X 轴的旋转方块 → 设置为 Z 轴
			//       检测到 Z 轴的旋转方块 → 设置为 X 轴
			axis = preferredAxis == Axis.X ? Axis.Z : Axis.X;
		}
		
		// 更新方块状态，设置为计算出的轴方向
		world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.AXIS, axis));
		
		return super.updateCustomBlockEntityTag(pos, world, player, stack, state);
	}

}