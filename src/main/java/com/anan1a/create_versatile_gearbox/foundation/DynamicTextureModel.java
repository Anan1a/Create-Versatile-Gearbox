package com.anan1a.create_versatile_gearbox.foundation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态纹理模型
 *
 * <p>功能说明：
 * 根据方块状态动态替换模型中的纹理。渲染时遍历四边形，将匹配占位符纹理
 * 的四边形替换为对应的目标纹理，实现同一模型不同状态的视觉效果。
 *
 * <p>使用方式：
 * 1. 定义状态类型 T（如枚举）
 * 2. 创建实例并传入策略接口实现
 * 3. 配置占位符纹理到目标纹理的映射关系
 *
 * <p>性能优化：
 * - placeholderEntryMap: O(1) 占位符查找
 * - textureCache: 避免重复加载纹理
 * - 移除冗余的预检查遍历
 *
 * @param <T> 状态类型
 */
@OnlyIn(Dist.CLIENT)
public class DynamicTextureModel<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTextureModel.class);

    /** 占位符到纹理条目的映射，用于O(1)查找 */
    private final Map<ResourceLocation, TextureEntry<T>> placeholderEntryMap = new HashMap<>();

    /** 纹理精灵缓存，避免每帧重复查询 */
    private final Map<ResourceLocation, TextureAtlasSprite> textureCache = new HashMap<>();

    /** 从BlockState获取状态数组的函数 */
    private final Function<BlockState, T[]> stateProvider;

    /** 根据面方向获取状态的策略 */
    private final BiFunction<Direction, T[], T> stateForFaceProvider;

    /** 从ModelData获取状态数组的策略 */
    private final Function<ModelData, T[]> modelDataStateProvider;

    /** 包装的原始模型 */
    private final BakedModel template;

    public DynamicTextureModel(
            BakedModel template,
            List<TextureEntry<T>> textureEntries,
            Function<BlockState, T[]> stateProvider,
            BiFunction<Direction, T[], T> stateForFaceProvider,
            Function<ModelData, T[]> modelDataStateProvider
    ) {
        this.template = template;
        this.stateProvider = stateProvider;
        this.stateForFaceProvider = stateForFaceProvider;
        this.modelDataStateProvider = modelDataStateProvider;

        // 构建占位符→条目映射，支持O(1)查找
        for (TextureEntry<T> entry : textureEntries) {
            placeholderEntryMap.put(entry.placeholder(), entry);
        }
    }

    /**
     * 获取四边形列表，实现动态纹理替换
     */
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        List<BakedQuad> originalQuads = template.getQuads(state, side, rand, extraData, renderType);
        T[] states = getStates(state, extraData);

        // 无有效状态或为空，直接返回原四边形
        if (states == null || states.length == 0) {
            return originalQuads;
        }

        List<BakedQuad> result = new ArrayList<>(originalQuads.size());

        for (BakedQuad quad : originalQuads) {
            if (quad == null) {
                result.add(quad);
                continue;
            }

            TextureAtlasSprite originalSprite = quad.getSprite();
            if (originalSprite == null) {
                result.add(quad);
                continue;
            }

            // O(1)查找对应的纹理条目
            TextureEntry<T> entry = placeholderEntryMap.get(originalSprite.contents().name());
            if (entry == null) {
                result.add(quad);
                continue;
            }

            Direction quadFace = quad.getDirection();
            if (quadFace == null) {
                result.add(quad);
                continue;
            }

            // 根据面方向获取目标纹理
            T stateKey = stateForFaceProvider.apply(quadFace, states);
            ResourceLocation targetLocation = entry.getTexture(stateKey);
            if (targetLocation == null) {
                continue; // 该状态无映射，跳过此四边形（实现隐藏效果）
            }

            // 获取或缓存目标纹理
            TextureAtlasSprite targetSprite = getOrCreateCachedSprite(targetLocation);
            if (targetSprite == null) {
                result.add(quad);
                continue;
            }

            // 克隆并重映射UV
            BakedQuad newQuad = cloneQuadWithTexture(quad, originalSprite, targetSprite);
            result.add(newQuad);
        }

        return result;
    }

    /**
     * 获取包装的原始模型
     */
    public BakedModel getTemplate() {
        return template;
    }

    /**
     * 获取或缓存的纹理精灵
     */
    private TextureAtlasSprite getOrCreateCachedSprite(ResourceLocation location) {
        TextureAtlasSprite cached = textureCache.get(location);
        if (cached != null) {
            return cached;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }

        try {
            TextureAtlasSprite sprite = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(location);
            if (sprite != null) {
                textureCache.put(location, sprite);
            }
            return sprite;
        } catch (Exception e) {
            LOGGER.error("Error loading texture {}: {}", location, e.getMessage());
            return null;
        }
    }

    /**
     * 克隆四边形并进行UV重映射
     *
     * <p>原理说明：
     * Minecraft将所有纹理打包到图集中，每个纹理有独立的UV范围（如0.5-0.6）。
     * BakedQuad顶点存储的是图集中的绝对UV值。
     * 替换纹理时需要将原UV映射到新纹理的UV空间，保持纹理坐标相对位置不变。
     *
     * <p>映射公式：
     * 相对位置 = (原始UV - 原纹理起点) / 原纹理宽度
     * 新UV = 目标纹理起点 + 相对位置 * 目标纹理宽度
     */
    private BakedQuad cloneQuadWithTexture(BakedQuad quad, TextureAtlasSprite original, TextureAtlasSprite target) {
        if (original == target) {
            return quad;
        }

        int[] vertexData = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);

        // 获取纹理在图集中的边界
        float origU0 = original.getU0(), origU1 = original.getU1();
        float origV0 = original.getV0(), origV1 = original.getV1();
        float tgtU0 = target.getU0(), tgtU1 = target.getU1();
        float tgtV0 = target.getV0(), tgtV1 = target.getV1();

        // 计算纹理尺寸
        float origW = origU1 - origU0, origH = origV1 - origV0;
        float tgtW = tgtU1 - tgtU0, tgtH = tgtV1 - tgtV0;

        // 尺寸无效时直接返回（不修改UV）
        if (origW <= 0 || origH <= 0 || tgtW <= 0 || tgtH <= 0) {
            return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), target, quad.isShade());
        }

        // 遍历四个顶点，重映射UV坐标
        for (int i = 0; i < 4; i++) {
            float srcU = BakedQuadHelper.getU(vertexData, i);
            float srcV = BakedQuadHelper.getV(vertexData, i);

            // 计算相对位置并裁剪到[0,1]
            float relU = clamp((srcU - origU0) / origW, 0f, 1f);
            float relV = clamp((srcV - origV0) / origH, 0f, 1f);

            // 映射到目标纹理坐标
            BakedQuadHelper.setU(vertexData, i, tgtU0 + relU * tgtW);
            BakedQuadHelper.setV(vertexData, i, tgtV0 + relV * tgtH);
        }

        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), target, quad.isShade());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 获取状态数组，优先使用ModelData，必要时回退到BlockState
     */
    private T[] getStates(BlockState state, ModelData extraData) {
        if (modelDataStateProvider != null) {
            T[] fromModelData = modelDataStateProvider.apply(extraData);
            if (fromModelData != null && fromModelData.length > 0) {
                return fromModelData;
            }
        }

        if (stateProvider != null) {
            try {
                return stateProvider.apply(state);
            } catch (Exception e) {
                LOGGER.error("Error getting states from BlockState: {}", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 纹理条目：封装占位符和状态→纹理映射
     *
     * @param <T> 状态类型
     */
    public record TextureEntry<T>(
            /** 占位符纹理ResourceLocation */
            ResourceLocation placeholder,
            /** 状态→目标纹理路径的映射 */
            Map<T, ResourceLocation> textureMap
    ) {
        public ResourceLocation getTexture(T state) {
            return textureMap.get(state);
        }
    }
}
