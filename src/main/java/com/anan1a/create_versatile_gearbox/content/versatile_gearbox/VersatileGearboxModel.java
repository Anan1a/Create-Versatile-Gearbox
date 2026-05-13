package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class VersatileGearboxModel extends DynamicTextureModel<ShaftState> {
	/**
	 * 状态属性
	 */
	public static final ModelProperty<ShaftState[]> FACE_STATES = new ModelProperty<>();

	/**
	 * 占位符纹理映射
	 */
	private static final Map<String, Map<ShaftState, String>> PLACEHOLDER_TEXTURE_MAPS = Map.of(
		// placeholder1: FWD/REV 状态显示不同纹理，OFF 隐藏
		"placeholder", Map.of(
			ShaftState.FWD, "fwd",
			ShaftState.REV, "rev"
			// OFF 不映射 → 自动隐藏
		),
		// placeholder2: OFF 状态显示 side 纹理，FWD/REV 隐藏
		"placeholder2", Map.of(
			ShaftState.OFF, "side"
			// FWD/REV 不映射 → 自动隐藏
		)
	);

	/**
	 * 构造函数
	 *
	 * @param template 原始烘焙模型
	 */
	public VersatileGearboxModel(BakedModel template) {
		super(
			template,
			"create_versatile_gearbox",
			"block/versatile_gearbox/",
			PLACEHOLDER_TEXTURE_MAPS,
			VersatileGearboxModel::getFaceStatesFromBlock
		);
	}

	/**
	 * 从 ModelData 获取状态数组
	 *
	 * @param extraData 额外的数据
	 * @return 状态数组
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected ShaftState[] getStatesFromModelData(ModelData extraData) {
		if (extraData != null && extraData.has(FACE_STATES)) {
			ShaftState[] states = extraData.get(FACE_STATES);
			return (states != null && states.length == 6) ? states : null;
		}
		return null;
	}

	/**
	 * 根据面方向获取对应的状态
	 *
	 * @param face   面方向
	 * @param states 状态数组
	 * @return 该面对应的状态
	 */
	@Override
	protected ShaftState getStateForFace(Direction face, ShaftState[] states) {
		return states[getDirectionIndex(face)];
	}

	/**
	 * 从 BlockState 获取状态数组
	 *
	 * @param state 状态
	 * @return 状态数组
	 */
	private static ShaftState[] getFaceStatesFromBlock(BlockState state) {
		if (!(state.getBlock() instanceof VersatileGearboxBlock)) {
			return null;
		}
		return new ShaftState[]{
			VersatileGearboxBlock.getShaftState(Direction.DOWN, state),
			VersatileGearboxBlock.getShaftState(Direction.UP, state),
			VersatileGearboxBlock.getShaftState(Direction.NORTH, state),
			VersatileGearboxBlock.getShaftState(Direction.SOUTH, state),
			VersatileGearboxBlock.getShaftState(Direction.WEST, state),
			VersatileGearboxBlock.getShaftState(Direction.EAST, state)
		};
	}

	/**
	 * 获取方向索引
	 *
	 * @param direction 方向
	 * @return 方向索引
	 */
	private int getDirectionIndex(Direction direction) {
		return switch (direction) {
			case DOWN -> 0;
			case UP -> 1;
			case NORTH -> 2;
			case SOUTH -> 3;
			case WEST -> 4;
			case EAST -> 5;
		};
	}
}