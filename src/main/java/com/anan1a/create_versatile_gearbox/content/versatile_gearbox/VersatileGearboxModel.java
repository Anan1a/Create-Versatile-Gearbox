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
	/**
	 * 模型属性：存储六个面的状态
	 */
	public static final ModelProperty<ShaftState[]> FACE_STATES = new ModelProperty<>();

	/**
	 * 纹理命名空间
	 */
	private static final String MOD_ID = "create_versatile_gearbox";

	/**
	 * 占位纹理名称（用于识别需要动态替换的面）
	 */
	private static final String PLACEHOLDER_TEXTURE_PATH = "block/versatile_gearbox/placeholder";

	public VersatileGearboxModel(BakedModel template) {
		super(template);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
			RenderType renderType) {
		List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);

		// 获取面状态
		ShaftState[] faceStates = getFaceStates(state, extraData);
		if (faceStates == null || faceStates.length != 6) {
			return quads;
		}

		// 创建副本以避免修改原始数据
		quads = new ArrayList<>(quads);

		// 遍历所有四边形
		for (int i = 0; i < quads.size(); i++) {
			// 获取当前四边形
			BakedQuad quad = quads.get(i);

			// 检查四边形是否为空
			if (quad == null)
				continue;

			// 获取原始四边形的纹理 Sprite
			TextureAtlasSprite originalSprite = quad.getSprite();
			if (originalSprite == null)
				continue;

			// 检查是否是占位纹理（需要替换的纹理）
			ResourceLocation originalLocation = originalSprite.contents().name();
			if (!PLACEHOLDER_TEXTURE_PATH.equals(originalLocation.getPath()) || !MOD_ID.equals(originalLocation.getNamespace())) {
				continue;
			}

			// 获取该四边形的面方向
			Direction quadFace = quad.getDirection();
			if (quadFace == null) {
				continue;
			}

			// 获取该面的状态
			int faceIndex = getDirectionIndex(quadFace);
			ShaftState faceState = faceStates[faceIndex];

			// 根据状态选择目标纹理，OFF 状态返回 null
			String targetTexturePath = switch (faceState) {
				case FWD -> "block/versatile_gearbox/fwd";
				case REV -> "block/versatile_gearbox/rev";
				case OFF -> null;
			};

			if (targetTexturePath == null) {
				quads.set(i, null); // OFF 状态移除四边形
				continue;
			}

			// 获取目标纹理的 Sprite
			ResourceLocation targetLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, targetTexturePath);
			TextureAtlasSprite targetSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(targetLocation);

			if (targetSprite == null) {
				continue;
			}

			// 克隆四边形并替换纹理
			BakedQuad newQuad = BakedQuadHelper.clone(quad);
			int[] vertexData = newQuad.getVertices();

			// 计算纹理坐标偏移量（原始纹理到目标纹理的偏移）
			float uOffset = targetSprite.getU0() - originalSprite.getU0();
			float vOffset = targetSprite.getV0() - originalSprite.getV0();

			// 替换四个顶点的纹理坐标
			for (int vertex = 0; vertex < 4; vertex++) {
				float u = BakedQuadHelper.getU(vertexData, vertex);
				float v = BakedQuadHelper.getV(vertexData, vertex);

				// 应用偏移量
				float newU = u + uOffset;
				float newV = v + vOffset;

				BakedQuadHelper.setU(vertexData, vertex, newU);
				BakedQuadHelper.setV(vertexData, vertex, newV);
			}

			quads.set(i, newQuad);
		}

		// 移除 OFF 状态被标记为 null 的四边形
		quads.removeIf(Objects::isNull);

		return quads;
	}

	/**
	 * 获取面状态数组
	 */
	private ShaftState[] getFaceStates(BlockState state, ModelData extraData) {
		// 优先从 ModelData 获取
		if (extraData != null && extraData.has(FACE_STATES)) {
			ShaftState[] faceStates = extraData.get(FACE_STATES);
			if (faceStates != null && faceStates.length == 6) {
				return faceStates;
			}
		}

		// 回退方案：从 BlockState 获取
		if (state != null && state.getBlock() instanceof VersatileGearboxBlock) {
			ShaftState[] faceStates = new ShaftState[6];
			faceStates[0] = VersatileGearboxBlock.getShaftState(Direction.DOWN, state);
			faceStates[1] = VersatileGearboxBlock.getShaftState(Direction.UP, state);
			faceStates[2] = VersatileGearboxBlock.getShaftState(Direction.NORTH, state);
			faceStates[3] = VersatileGearboxBlock.getShaftState(Direction.SOUTH, state);
			faceStates[4] = VersatileGearboxBlock.getShaftState(Direction.WEST, state);
			faceStates[5] = VersatileGearboxBlock.getShaftState(Direction.EAST, state);
			return faceStates;
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