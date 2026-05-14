package com.anan1a.create_versatile_gearbox.foundation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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

/**
 * 通用动态纹理模型基类
 * <p>
 * 根据占位符纹理和映射规则，在渲染时动态替换四边形纹理。
 * 支持多个占位符纹理，每个占位符可独立配置状态→纹理映射。
 * 
 * @param <T> 状态类型（如 ShaftState、Enum 等）
 */
public abstract class DynamicTextureModel<T> extends BakedModelWrapper<BakedModel> {
	/**
	 * 占位符→(状态→纹理路径)的映射
	 */
	protected final Map<ResourceLocation, Map<T, ResourceLocation>> placeholderTextureMaps;

	/**
	 * 从 BlockState 获取状态数组的函数
	 */
	protected final Function<BlockState, T[]> stateProvider;
	
	/**
	 * 构造函数（使用局部路径，自动拼接）
	 * 
	 * @param template       			原始烘焙模型
	 * @param modId          			模组ID
	 * @param textureBase   			纹理基础路径（如 "block/example/"）
	 * @param placeholderTextureMaps	占位符→(状态→纹理路径)的映射
	 * @param stateProvider  			从 BlockState 获取状态数组的函数
	 */
	protected DynamicTextureModel(
			BakedModel template,
			String modId,
			String textureBase,
			Map<String, Map<T, String>> placeholderTextureMaps,
			Function<BlockState, T[]> stateProvider
	) {
		super(template);
		this.placeholderTextureMaps = new java.util.HashMap<>();
		placeholderTextureMaps.forEach((placeholderName, texturePathMap) -> {
			ResourceLocation placeholder = createLocation(modId, textureBase, placeholderName);
			this.placeholderTextureMaps.put(placeholder, buildTextureMap(modId, textureBase, texturePathMap));
		});
		this.stateProvider = stateProvider;
	}

	/**
	 * 构造函数（使用完整路径，不自动拼接）
	 * 
	 * @param template       			原始烘焙模型
	 * @param placeholderTextureMaps	占位符→(状态→纹理路径)的映射（使用完整 ResourceLocation）
	 * @param stateProvider  			从 BlockState 获取状态数组的函数
	 */
	protected DynamicTextureModel(
			BakedModel template,
			Map<ResourceLocation, Map<T, ResourceLocation>> placeholderTextureMaps,
			Function<BlockState, T[]> stateProvider
	) {
		super(template);
		this.placeholderTextureMaps = placeholderTextureMaps;
		this.stateProvider = stateProvider;
	}

	/**
	 * 创建 ResourceLocation
	 */
	private static ResourceLocation createLocation(String modId, String base, String name) {
		return ResourceLocation.fromNamespaceAndPath(modId, base + name);
	}

	/**
	 * 构建占位符→(状态→纹理路径)的映射
	 */
	private Map<T, ResourceLocation> buildTextureMap(String modId, String base, Map<T, String> pathMap) {
		Map<T, ResourceLocation> result = new java.util.HashMap<>();
		pathMap.forEach((key, path) -> result.put(key, createLocation(modId, base, path)));
		return result;
	}

	/**
	 * 重写获取四边形纹理方法
	 */
	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
			RenderType renderType) {
		List<BakedQuad> quads = super.getQuads(state, side, rand, extraData, renderType);
		
		T[] states = getStates(state, extraData);
		if (states == null) {
			return quads;
		}
		
		quads = new ArrayList<>(quads);
		for (int i = 0; i < quads.size(); i++) {
			BakedQuad quad = quads.get(i);
			if (quad == null) continue;
			
			TextureAtlasSprite originalSprite = quad.getSprite();
			Direction quadFace = quad.getDirection();
			if (originalSprite == null || quadFace == null) continue;
			
			ResourceLocation originalLocation = originalSprite.contents().name();
			// 查找匹配的占位符
			Map<T, ResourceLocation> textureMap = placeholderTextureMaps.get(originalLocation);
			if (textureMap == null) continue;
			
			T stateKey = getStateForFace(quadFace, states);
			ResourceLocation targetLocation = textureMap.get(stateKey);
			
			if (targetLocation == null) {
				quads.set(i, null);
				continue;
			}
			
			TextureAtlasSprite targetSprite = Minecraft.getInstance()
					.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
					.apply(targetLocation);
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
	
	/**
	 * 获取状态数组（优先从 ModelData，回退到 BlockState）
	 */
	@SuppressWarnings("unchecked")
	protected T[] getStates(BlockState state, ModelData extraData) {
		T[] states = getStatesFromModelData(extraData);
		if (states != null) {
			return states;
		}
		
		if (stateProvider != null) {
			return stateProvider.apply(state);
		}
		
		return null;
	}
	
	/**
	 * 从 ModelData 获取状态数组（子类可重写）
	 */
	@SuppressWarnings("unchecked")
	protected T[] getStatesFromModelData(ModelData extraData) {
		return null;
	}
	
	/**
	 * 根据面方向获取对应的状态（子类必须实现）
	 * 
	 * @param face   面方向
	 * @param states 状态数组
	 * @return 该面对应的状态
	 */
	protected abstract T getStateForFace(Direction face, T[] states);
}
