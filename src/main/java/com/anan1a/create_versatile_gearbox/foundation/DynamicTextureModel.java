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
 * 支持自定义状态到纹理的映射逻辑。
 * 
 * @param <T> 状态类型（如 ShaftState、Enum 等）
 */
public abstract class DynamicTextureModel<T> extends BakedModelWrapper<BakedModel> {
	
	protected final ResourceLocation placeholder;
	protected final Map<T, ResourceLocation> textureMap;
	protected final Function<BlockState, T[]> stateProvider;
	
	/**
	 * 构造函数
	 * 
	 * @param template       	原始烘焙模型
	 * @param modId          	模组ID
	 * @param textureBase    	纹理基础路径（如 "block/example/"）
	 * @param placeholderName 	占位符纹理名称
	 * @param texturePathMap	状态到纹理路径的映射
	 * @param stateProvider  	从 BlockState 获取状态数组的函数
	 */
	protected DynamicTextureModel(
			BakedModel template,
			String modId,
			String textureBase,
			String placeholderName,
			Map<T, String> texturePathMap,
			Function<BlockState, T[]> stateProvider
	) {
		super(template);
		this.placeholder = createLocation(modId, textureBase, placeholderName);
		this.textureMap = buildTextureMap(modId, textureBase, texturePathMap);
		this.stateProvider = stateProvider;
	}
	
	private static ResourceLocation createLocation(String modId, String base, String name) {
		return ResourceLocation.fromNamespaceAndPath(modId, base + name);
	}
	
	private Map<T, ResourceLocation> buildTextureMap(String modId, String base, Map<T, String> pathMap) {
		Map<T, ResourceLocation> result = new java.util.HashMap<>();
		pathMap.forEach((key, path) -> result.put(key, createLocation(modId, base, path)));
		return result;
	}
	
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
			if (
					originalSprite == null ||
					quadFace == null ||
					!placeholder.equals(originalSprite.contents().name())
			) continue;

			
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
