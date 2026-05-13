package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

public class VersatileGearboxModel extends DynamicTextureModel<ShaftState> {
	public static final ModelProperty<ShaftState[]> FACE_STATES = new ModelProperty<>();
	
	private static final Map<ShaftState, String> TEXTURE_MAP = Map.of(
		ShaftState.FWD, "fwd",
		ShaftState.REV, "rev"
	);
	
	public VersatileGearboxModel(BakedModel template) {
		super(
			template,
			"create_versatile_gearbox",
			"block/versatile_gearbox/",
			"placeholder",
			TEXTURE_MAP,
			VersatileGearboxModel::getFaceStatesFromBlock
		);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected ShaftState[] getStatesFromModelData(ModelData extraData) {
		if (extraData != null && extraData.has(FACE_STATES)) {
			ShaftState[] states = extraData.get(FACE_STATES);
			return (states != null && states.length == 6) ? states : null;
		}
		return null;
	}
	
	@Override
	protected ShaftState getStateForFace(Direction face, ShaftState[] states) {
		return states[getDirectionIndex(face)];
	}
	
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