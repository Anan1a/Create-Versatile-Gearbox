package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * 万能变速箱动态模型
 * <p>
 * 根据六个面的状态（FWD/REV/OFF）动态替换纹理。
 * 使用 BakedModelWrapper 包装原始模型，在渲染时动态修改四边形的纹理坐标。
 */
public class VersatileGearboxModel extends BakedModelWrapper<BakedModel> {
	public static final ModelProperty<ShaftState[]> FACE_STATES = new ModelProperty<>();
	
	private static final String MOD_ID = "create_versatile_gearbox";
	private static final String TEXTURE_BASE = "block/versatile_gearbox/";
	private static final ResourceLocation PLACEHOLDER = createLocation("placeholder");
	private static final ResourceLocation FWD_TEXTURE = createLocation("fwd");
	private static final ResourceLocation REV_TEXTURE = createLocation("rev");
	
	private static ResourceLocation createLocation(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, TEXTURE_BASE + path);
	}

	public VersatileGearboxModel(BakedModel template) {
		super(template);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
			RenderType renderType) {
		List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
		ShaftState[] faceStates = getFaceStates(state, extraData);
		if (faceStates == null || faceStates.length != 6) {
			return quads;
		}

		quads = new ArrayList<>(quads);
		for (int i = 0; i < quads.size(); i++) {
			BakedQuad quad = quads.get(i);
			if (quad == null) continue;

			TextureAtlasSprite originalSprite = quad.getSprite();
			Direction quadFace = quad.getDirection();
			if (
					originalSprite == null ||
					quadFace == null ||
					!PLACEHOLDER.equals(originalSprite.contents().name())
			) continue;

			ShaftState faceState = faceStates[getDirectionIndex(quadFace)];
			ResourceLocation targetLocation = switch (faceState) {
				case FWD -> FWD_TEXTURE;
				case REV -> REV_TEXTURE;
				case OFF -> null;
			};
			if (targetLocation == null) {
				quads.set(i, null);
				continue;
			}

			TextureAtlasSprite targetSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(targetLocation);
			if (targetSprite == null) continue;
			BakedQuad newQuad = BakedQuadHelper.clone(quad);
			int[] vertexData = newQuad.getVertices();
			float uOffset = targetSprite.getU0() - originalSprite.getU0();
			float vOffset = targetSprite.getV0() - originalSprite.getV0();

			for (int vertex = 0; vertex < 4; vertex++) {
				BakedQuadHelper.setU(vertexData, vertex, BakedQuadHelper.getU(vertexData, vertex) + uOffset);
				BakedQuadHelper.setV(vertexData, vertex, BakedQuadHelper.getV(vertexData, vertex) + vOffset);
			}

			quads.set(i, newQuad);
		}

		quads.removeIf(Objects::isNull);
		return quads;
	}

	private ShaftState[] getFaceStates(BlockState state, ModelData extraData) {
		if (extraData != null && extraData.has(FACE_STATES)) {
			ShaftState[] faceStates = extraData.get(FACE_STATES);
			if (faceStates != null && faceStates.length == 6) {
				return faceStates;
			}
		}

		if (state != null && state.getBlock() instanceof VersatileGearboxBlock) {
			return new ShaftState[]{
				VersatileGearboxBlock.getShaftState(Direction.DOWN, state),
				VersatileGearboxBlock.getShaftState(Direction.UP, state),
				VersatileGearboxBlock.getShaftState(Direction.NORTH, state),
				VersatileGearboxBlock.getShaftState(Direction.SOUTH, state),
				VersatileGearboxBlock.getShaftState(Direction.WEST, state),
				VersatileGearboxBlock.getShaftState(Direction.EAST, state)
			};
		}

		return null;
	}

	/**
	 * 将方向转换为索引（0-5）
	 * 顺序：DOWN, UP, NORTH, SOUTH, WEST, EAST
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