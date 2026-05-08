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
	 * 智能检测周围环境并设置合适的轴方向：
	 * 1. 检测四个水平方向是否有旋转方块（实现 IRotate 接口）
	 * 2. 如果找到唯一匹配的轴，使用该轴
	 * 3. 如果找到多个不同的轴，取消智能选择
	 * 4. 如果没有找到，使用玩家朝向的顺时针方向作为轴
	 * 5. 最后将方块设置为水平轴向（X 或 Z）
	 * 
	 * @param pos 放置位置
	 * @param world 世界
	 * @param player 玩家
	 * @param stack 物品栈
	 * @param state 方块状态
	 * @return 是否成功更新
	 */
	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos pos, Level world, Player player, ItemStack stack, BlockState state) {
		// 存储检测到的优先轴方向
		Axis preferredAxis = null;
		
		// 遍历四个水平方向（东、南、西、北）
		for (Direction side : Iterate.horizontalDirections) {
			// 获取相邻位置的方块状态
			BlockState blockState = world.getBlockState(pos.relative(side));
			
			// 检查相邻方块是否实现了 IRotate 接口（可旋转的动力组件）
			if (blockState.getBlock() instanceof IRotate) {
				IRotate rotateBlock = (IRotate) blockState.getBlock();
				
				// 检查相邻方块是否有朝向当前位置的轴
				if (rotateBlock.hasShaftTowards(world, pos.relative(side), blockState, side.getOpposite())) {
					// 如果已经找到一个轴，且当前轴与之前的不同
					if (preferredAxis != null && preferredAxis != side.getAxis()) {
						// 说明有多个不同方向的轴，取消智能选择
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
			// 没有检测到旋转方块，使用玩家朝向的顺时针方向
			axis = player.getDirection().getClockWise().getAxis();
		} else {
			// 检测到旋转方块，使用垂直于检测轴的方向
			// 例如：检测到 X 轴的旋转方块，就设置为 Z 轴（水平方向）
			axis = preferredAxis == Axis.X ? Axis.Z : Axis.X;
		}
		
		// 更新方块状态，设置为计算出的轴方向
		world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.AXIS, axis));
		
		return super.updateCustomBlockEntityTag(pos, world, player, stack, state);
	}

}