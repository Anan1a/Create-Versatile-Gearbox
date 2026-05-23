
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用动态纹理模型基类
 * <p>
 * 根据占位符纹理和映射规则，在渲染时动态替换四边形纹理。
 * 支持多个占位符纹理，每个占位符可独立配置状态→纹理映射。
 *
 * @param <T> 状态类型（如 ShaftState、Enum 等）
 */
@OnlyIn(Dist.CLIENT)
public abstract class DynamicTextureModel<T> extends BakedModelWrapper<BakedModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTextureModel.class);

    /**
     * 占位符→(状态→纹理路径)的映射
     */
    protected final Map<ResourceLocation, Map<T, ResourceLocation>> placeholderTextureMaps;

    /**
     * 从 BlockState 获取状态数组的函数
     */
    protected final Function<BlockState, T[]> stateProvider;
    
    /**
     * 纹理缓存：避免每帧重复查找 TextureAtlasSprite
     * Key: targetLocation, Value: cached sprite
     */
    private final Map<ResourceLocation, TextureAtlasSprite> textureCache = new java.util.HashMap<>();

    /**
     * 构造函数（使用局部路径，自动拼接）
     *
     * @param template                   原始烘焙模型
     * @param modId                      模组ID
     * @param textureBase               纹理基础路径（如 "block/example/"）
     * @param placeholderTextureMaps    占位符→(状态→纹理路径)的映射
     * @param stateProvider              从 BlockState 获取状态数组的函数
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
     * @param template                   原始烘焙模型
     * @param placeholderTextureMaps    占位符→(状态→纹理路径)的映射（使用完整 ResourceLocation）
     * @param stateProvider              从 BlockState 获取状态数组的函数
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
     * 获取或缓存的纹理精灵
     *
     * @param targetLocation 纹理位置
     * @return 纹理精灵，如果不存在则返回 null
     */
    private TextureAtlasSprite getOrCreateCachedSprite(ResourceLocation targetLocation) {
        return textureCache.computeIfAbsent(targetLocation, loc -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                LOGGER.warn("Minecraft instance not available during texture lookup for: {}", loc);
                return null;
            }
            try {
                TextureAtlasSprite sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(loc);
                if (sprite == null) {
                    LOGGER.warn("Failed to load texture: {}", loc);
                }
                return sprite;
            } catch (Exception e) {
                LOGGER.error("Error loading texture {}: {}", loc, e.getMessage());
                return null;
            }
        });
    }

    /**
     * 重写获取四边形纹理方法
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        List<BakedQuad> originalQuads = super.getQuads(state, side, rand, extraData, renderType);

        T[] states = getStates(state, extraData);
        if (states == null || states.length == 0) {
            return originalQuads;
        }

        // 预检查：确定是否有需要替换的纹理
        boolean needsModification = false;
        for (BakedQuad quad : originalQuads) {
            if (quad == null) continue;
            TextureAtlasSprite sprite = quad.getSprite();
            if (sprite != null && placeholderTextureMaps.containsKey(sprite.contents().name())) {
                needsModification = true;
                break;
            }
        }

        // 如果没有需要修改的 quad，直接返回原列表（避免不必要的拷贝）
        if (!needsModification) {
            return originalQuads;
        }

        // 创建可变副本
        List<BakedQuad> quads = new ArrayList<>(originalQuads.size());

        for (BakedQuad quad : originalQuads) {
            if (quad == null) {
                continue;
            }

            TextureAtlasSprite originalSprite = quad.getSprite();
            if (originalSprite == null) {
                quads.add(quad);
                continue;
            }

            ResourceLocation originalLocation = originalSprite.contents().name();
            Map<T, ResourceLocation> textureMap = placeholderTextureMaps.get(originalLocation);

            // 不是占位符纹理，保留原 quad
            if (textureMap == null) {
                quads.add(quad);
                continue;
            }

            Direction quadFace = quad.getDirection();
            // 对于非定向 quad（face == null），跳过纹理替换但保留 quad
            if (quadFace == null) {
                quads.add(quad);
                LOGGER.debug("Skipping non-directional quad with placeholder texture: {}", originalLocation);
                continue;
            }

            T stateKey = getStateForFace(quadFace, states);
            ResourceLocation targetLocation = textureMap.get(stateKey);

            // 状态映射为 null（如 OFF 状态），隐藏该 quad
            if (targetLocation == null) {
                LOGGER.trace("Hiding quad for state {} on face {} (placeholder: {})",
                        stateKey, quadFace, originalLocation);
                continue;
            }

            // 使用缓存的纹理精灵
            TextureAtlasSprite targetSprite = getOrCreateCachedSprite(targetLocation);
            if (targetSprite == null) {
                // 纹理加载失败，保留原 quad 作为降级方案
                LOGGER.warn("Using fallback for missing texture: {} on face {}", targetLocation, quadFace);
                quads.add(quad);
                continue;
            }

            // 克隆并修改纹理坐标
            BakedQuad newQuad = BakedQuadHelper.clone(quad);
            int[] vertexData = newQuad.getVertices();
            float uOffset = targetSprite.getU0() - originalSprite.getU0();
            float vOffset = targetSprite.getV0() - originalSprite.getV0();

            // 仅在有实际偏移时才更新顶点数据
            if (uOffset != 0.0f || vOffset != 0.0f) {
                for (int vertex = 0; vertex < 4; vertex++) {
                    BakedQuadHelper.setU(vertexData, vertex,
                            BakedQuadHelper.getU(vertexData, vertex) + uOffset);
                    BakedQuadHelper.setV(vertexData, vertex,
                            BakedQuadHelper.getV(vertexData, vertex) + vOffset);
                }
            }

            quads.add(newQuad);
        }

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
            try {
                return stateProvider.apply(state);
            } catch (Exception e) {
                LOGGER.error("Error getting states from BlockState: {}", e.getMessage());
                return null;
            }
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