package com.anan1a.create_versatile_gearbox.foundation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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
 * 一个通用的动态纹理重映射模型。在渲染时，根据运行时解析的状态，将已烘焙模型上的占位纹理
 * 替换为目标纹理。
 * <p>
 * 此工具适用于需要在运行时动态改变外观的方块，无需定义多个 blockstate 变体或复杂的 model override。
 * 它通过拦截 quad 烘焙流程，将 UV 坐标从占位精灵区域重映射到目标精灵的对应区域。
 *
 * @param <T> 状态键类型，用于决定每个占位符应使用哪个纹理
 */
@OnlyIn(Dist.CLIENT)
public class DynamicTextureModel<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTextureModel.class);

    /** quad 结果列表的默认初始容量，用于减少重复分配。 */
    private static final int DEFAULT_QUAD_CAPACITY = 32;

    /**
     * 纹理最小尺寸阈值。小于此值的纹理被视为无效纹理，将跳过 UV 重映射，
     * 以避免除零错误或渲染异常。
     */
    private static final float MIN_TEXTURE_SIZE = 1e-6f;

    /** 占位纹理位置到其对应 TextureEntry 的不可变映射表。 */
    private final Map<ResourceLocation, TextureEntry<T>> placeholderEntryMap;

    /** 已加载目标精灵的线程安全缓存，以 ResourceLocation 为键。 */
    private final ConcurrentHashMap<ResourceLocation, TextureAtlasSprite> textureCache;

    /** 状态解析器，用于在渲染时确定每个面应使用哪个状态键。 */
    private final StateResolver<T> stateResolver;

    /** 原始已烘焙模型，其 quad 将被拦截并重映射。 */
    private final BakedModel template;

    /** 线程本地可复用的 quad 结果列表，避免每帧渲染时产生分配开销。 */
    private final ThreadLocal<ArrayList<BakedQuad>> resultListCache = ThreadLocal.withInitial(
            () -> new ArrayList<>(DEFAULT_QUAD_CAPACITY)
    );

    /**
     * 构造一个新的动态纹理模型。
     *
     * @param template       提供 quad 来源的原始已烘焙模型
     * @param textureEntries 定义占位符到目标纹理映射关系的纹理条目列表
     * @param stateResolver  为每个面提供当前状态键的解析器
     * @throws NullPointerException 如果任一参数为 null
     */
    public DynamicTextureModel(
            BakedModel template,
            List<TextureEntry<T>> textureEntries,
            StateResolver<T> stateResolver
    ) {
        this.template = Objects.requireNonNull(template, "template cannot be null");
        this.stateResolver = Objects.requireNonNull(stateResolver, "stateResolver cannot be null");

        this.placeholderEntryMap = Collections.unmodifiableMap(buildPlaceholderMap(textureEntries));
        this.textureCache = new ConcurrentHashMap<>();
    }

    /**
     * 根据占位纹理位置构建一个从 TextureEntry 的不可变查找映射表。
     * 每个条目的占位纹理所用作映射表的键，以便在渲染时实现 O(1) 高效查找。
     *
     * @param entries 纹理条目列表
     * @param <T>     状态键类型
     * @return 占位纹理到 TextureEntry 的不可变映射表
     * @throws NullPointerException 如果 entries 或其中任一 entry/placeholder 为 null
     */
    private static <T> Map<ResourceLocation, TextureEntry<T>> buildPlaceholderMap(List<TextureEntry<T>> entries) {
        Objects.requireNonNull(entries, "textureEntries cannot be null");
        Map<ResourceLocation, TextureEntry<T>> map = new HashMap<>(entries.size());
        for (TextureEntry<T> entry : entries) {
            Objects.requireNonNull(entry, "texture entry cannot be null");
            Objects.requireNonNull(entry.placeholder(), "placeholder cannot be null");
            map.put(entry.placeholder(), entry);
        }
        return map;
    }

    /**
     * 核心渲染方法。拦截模板模型的 quad，将精灵匹配到已注册占位符的 quad
     * 替换为重映射到目标纹理的新 quad。
     * <p>
     * 每个 quad 的处理流程：
     * <ol>
     *   <li>获取 quad 的精灵</li>
     *   <li>检查精灵名称是否匹配已知占位符</li>
     *   <li>通过 StateResolver 解析该 quad 面对应的当前状态键</li>
     *   <li>根据状态键查找目标纹理位置</li>
     *   <li>加载（或从缓存中获取）目标 TextureAtlasSprite</li>
     *   <li>克隆 quad 并重映射 UV 坐标以匹配目标精灵</li>
     * </ol>
     *
     * @param state      当前的方块状态
     * @param side       正在渲染的面方向
     * @param rand       随机数源（透传给模板模型）
     * @param extraData  附加到方块实体的模型数据
     * @param renderType 渲染类型（例如 solid、cutout、translucent）
     * @return 经过动态纹理重映射的已烘焙 quad 列表
     */
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        List<BakedQuad> originalQuads = template.getQuads(state, side, rand, extraData, renderType);

        ArrayList<BakedQuad> result = resultListCache.get();
        result.clear();
        result.ensureCapacity(originalQuads.size());

        Map<ResourceLocation, TextureEntry<T>> placeholderMap = this.placeholderEntryMap;
        ConcurrentHashMap<ResourceLocation, TextureAtlasSprite> cache = this.textureCache;
        StateResolver<T> resolver = this.stateResolver;

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

            TextureEntry<T> entry = placeholderMap.get(originalSprite.contents().name());
            if (entry == null) {
                result.add(quad);
                continue;
            }

            Direction quadFace = quad.getDirection();
            if (quadFace == null) {
                result.add(quad);
                continue;
            }

            T stateKey = resolver.getState(state, quadFace, extraData);
            if (stateKey == null) {
                result.add(quad);
                continue;
            }

            ResourceLocation targetLocation = entry.getTexture(stateKey);
            if (targetLocation == null) {
                continue;
            }

            TextureAtlasSprite targetSprite = getOrCreateCachedSprite(cache, targetLocation);
            if (targetSprite == null) {
                result.add(quad);
                continue;
            }

            BakedQuad newQuad = cloneQuadWithTexture(quad, originalSprite, targetSprite);
            result.add(newQuad);
        }

        return result;
    }

    /**
     * 返回原始的模板烘焙模型。
     *
     * @return 模板模型
     */
    public BakedModel getTemplate() {
        return template;
    }

    /**
     * 从缓存中获取 TextureAtlasSprite，如果尚未缓存则从方块纹理图集中加载。
     * 使用 computeIfAbsent 确保线程安全的懒加载。
     *
     * @param cache    并发精灵缓存
     * @param location 目标纹理的资源位置
     * @return 加载的精灵，如果加载失败则返回 null
     */
    private static TextureAtlasSprite getOrCreateCachedSprite(
            ConcurrentHashMap<ResourceLocation, TextureAtlasSprite> cache,
            ResourceLocation location
    ) {
        TextureAtlasSprite cached = cache.get(location);
        if (cached != null) {
            return cached;
        }
        return cache.computeIfAbsent(location, loc -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                LOGGER.warn("Minecraft instance not available for texture loading");
                return null;
            }
            try {
                return mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(loc);
            } catch (Exception e) {
                LOGGER.error("Failed to load texture: {}", loc, e);
                return null;
            }
        });
    }

    /**
     * 克隆一个已烘焙 quad 并将其 UV 坐标从原始精灵区域重映射到目标精灵的对应区域。
     * 这保留了纹理在精灵内的相对 UV 位置，使得原始精灵中位于 (u%, v%) 的纹理特征
     * 在目标精灵中出现在相同的相对位置。
     * <p>
     * 重映射使用线性缩放：
     * <pre>
     *   newU = srcU * (targetW / origW) + (targetU0 - origU0 * scaleU)
     *   newV = srcV * (targetH / origH) + (targetV0 - origV0 * scaleV)
     * </pre>
     * 结果会被 clamp 到目标精灵的 UV 范围内，以防止纹理溢出。
     *
     * @param quad     原始已烘焙 quad
     * @param original 原始精灵（用作 UV 参考边界）
     * @param target   要重映射到的目标精灵
     * @return 具有重映射 UV 的新 BakedQuad，如果精灵相同则返回原始 quad
     */
    private static BakedQuad cloneQuadWithTexture(BakedQuad quad, TextureAtlasSprite original, TextureAtlasSprite target) {
        if (original == target) {
            return quad;
        }

        int[] vertexData = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);

        float origU0 = original.getU0(), origU1 = original.getU1();
        float origV0 = original.getV0(), origV1 = original.getV1();
        float tgtU0 = target.getU0(), tgtU1 = target.getU1();
        float tgtV0 = target.getV0(), tgtV1 = target.getV1();

        float origW = origU1 - origU0;
        float origH = origV1 - origV0;
        float tgtW = tgtU1 - tgtU0;
        float tgtH = tgtV1 - tgtV0;

        if (origW <= MIN_TEXTURE_SIZE || origH <= MIN_TEXTURE_SIZE
                || tgtW <= MIN_TEXTURE_SIZE || tgtH <= MIN_TEXTURE_SIZE) {
            return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), target, quad.isShade());
        }

        float scaleU = tgtW / origW;
        float scaleV = tgtH / origH;
        float offsetU = tgtU0 - origU0 * scaleU;
        float offsetV = tgtV0 - origV0 * scaleV;

        for (int i = 0; i < 4; i++) {
            float srcU = BakedQuadHelper.getU(vertexData, i);
            float srcV = BakedQuadHelper.getV(vertexData, i);
            float newU = clamp(srcU * scaleU + offsetU, tgtU0, tgtU1);
            float newV = clamp(srcV * scaleV + offsetV, tgtV0, tgtV1);
            BakedQuadHelper.setU(vertexData, i, newU);
            BakedQuadHelper.setV(vertexData, i, newV);
        }

        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), target, quad.isShade());
    }

    /**
     * 将值限制在 [min, max] 范围内。
     *
     * @param value 要限制的值
     * @param min   下限
     * @param max   上限
     * @return 限制后的值，保证在 [min, max] 范围内
     */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 用于解析指定方块面动态状态键的函数式接口。
     * 实现类根据当前方块状态、面方向和模型数据，决定应显示哪种纹理变体。
     *
     * @param <T> 状态键类型
     */
    @FunctionalInterface
    public interface StateResolver<T> {
        /**
         * 解析指定面的状态键。
         *
         * @param state 当前方块状态
         * @param face  正在渲染的面方向
         * @param data  方块实体的模型数据
         * @return 状态键，如果不进行纹理替换则返回 null
         */
        T getState(BlockState state, Direction face, ModelData data);
    }

    /**
     * 定义动态纹理映射条目的 record。它将占位纹理位置与一个从状态键到目标纹理位置的映射表配对。
     * 当遇到使用占位精灵的 quad 时，将使用当前状态键对应的目标纹理来代替。
     *
     * @param <T>         状态键类型
     * @param placeholder 模型中占位纹理的资源位置
     * @param textureMap  从状态键到目标纹理资源位置的映射表
     */
    public record TextureEntry<T>(
            ResourceLocation placeholder,
            Map<T, ResourceLocation> textureMap
    ) {
        /**
         * 根据给定状态查找目标纹理位置。
         *
         * @param state 状态键
         * @return 目标纹理位置，如果该状态没有映射则返回 null
         */
        public ResourceLocation getTexture(T state) {
            return textureMap.get(state);
        }

        /**
         * 检查此条目是否有效（非空占位符且非空纹理映射表）。
         *
         * @return 如果条目包含占位符且至少有一个纹理映射，则返回 true
         */
        public boolean isValid() {
            return placeholder != null && textureMap != null && !textureMap.isEmpty();
        }
    }
}
