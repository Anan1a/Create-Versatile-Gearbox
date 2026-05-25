package com.anan1a.create_versatile_gearbox.foundation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
 * 动态纹理模型 - 高性能版本
 * 
 * <p>功能说明：
 * 根据方块状态动态替换模型中的纹理。渲染时遍历四边形，将匹配占位符纹理
 * 的四边形替换为对应的目标纹理，实现同一模型不同状态的视觉效果。
 * 
 * <p>性能优化策略：
 * - 使用 ConcurrentHashMap 实现线程安全的纹理缓存
 * - 占位符映射在初始化时构建，支持 O(1) 查找
 * - 减少对象创建，复用临时集合
 * - UV 映射计算提取公共逻辑，避免重复计算
 * - 提前验证状态有效性，减少运行时检查
 * 
 * <p>使用方式：
 * 1. 定义状态类型 T（如枚举）
 * 2. 创建实例并传入策略接口实现
 * 3. 配置占位符纹理到目标纹理的映射关系
 * 
 * @param <T> 状态类型
 */
@OnlyIn(Dist.CLIENT)
public class DynamicTextureModel<T> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTextureModel.class);
    
    /** 
     * 默认四边形容量，用于预分配集合大小
     * 典型方块模型约有 24 个四边形（6面 × 4个）
     */
    private static final int DEFAULT_QUAD_CAPACITY = 32;
    
    /** 
     * 无效纹理尺寸阈值，小于等于此值视为无效纹理
     */
    private static final float MIN_TEXTURE_SIZE = 1e-6f;
    
    /**
     * 占位符到纹理条目的映射（O(1) 查找）
     * 初始化后只读，无需同步
     */
    private final Map<ResourceLocation, TextureEntry<T>> placeholderEntryMap;
    
    /**
     * 纹理精灵缓存（线程安全）
     * 使用 ConcurrentHashMap 保证多线程环境下的安全访问
     */
    private final ConcurrentHashMap<ResourceLocation, TextureAtlasSprite> textureCache;
    
    /** 从 BlockState 获取状态数组的函数 */
    private final Function<BlockState, T[]> stateProvider;
    
    /** 根据面方向获取状态的策略 */
    private final BiFunction<Direction, T[], T> stateForFaceProvider;
    
    /** 从 ModelData 获取状态数组的策略 */
    private final Function<ModelData, T[]> modelDataStateProvider;
    
    /** 包装的原始模型 */
    private final BakedModel template;
    
    /** 预分配的结果列表，减少 GC */
    private final ThreadLocal<ArrayList<BakedQuad>> resultListCache = ThreadLocal.withInitial(
            () -> new ArrayList<>(DEFAULT_QUAD_CAPACITY)
    );
    
    /**
     * 创建动态纹理模型
     * 
     * @param template              原始模型
     * @param textureEntries        纹理条目列表
     * @param stateProvider         从 BlockState 获取状态的函数
     * @param stateForFaceProvider  根据面方向获取状态的策略
     * @param modelDataStateProvider 从 ModelData 获取状态的策略
     * @throws NullPointerException 如果任何参数为 null
     */
    public DynamicTextureModel(
            BakedModel template,
            List<TextureEntry<T>> textureEntries,
            Function<BlockState, T[]> stateProvider,
            BiFunction<Direction, T[], T> stateForFaceProvider,
            Function<ModelData, T[]> modelDataStateProvider
    ) {
        this.template = Objects.requireNonNull(template, "template cannot be null");
        this.stateProvider = Objects.requireNonNull(stateProvider, "stateProvider cannot be null");
        this.stateForFaceProvider = Objects.requireNonNull(stateForFaceProvider, "stateForFaceProvider cannot be null");
        this.modelDataStateProvider = modelDataStateProvider; // 允许 null
        
        // 构建占位符映射，初始化后只读
        this.placeholderEntryMap = Collections.unmodifiableMap(buildPlaceholderMap(textureEntries));
        
        // 初始化线程安全的纹理缓存
        this.textureCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 构建占位符到纹理条目的映射
     * 
     * @param entries 纹理条目列表
     * @return 不可变的映射表
     */
    private static <T> Map<ResourceLocation, TextureEntry<T>> buildPlaceholderMap(List<TextureEntry<T>> entries) {
        Objects.requireNonNull(entries, "textureEntries cannot be null");
        
        // 预分配容量，避免扩容
        Map<ResourceLocation, TextureEntry<T>> map = new java.util.HashMap<>(entries.size());
        for (TextureEntry<T> entry : entries) {
            Objects.requireNonNull(entry, "texture entry cannot be null");
            Objects.requireNonNull(entry.placeholder(), "placeholder cannot be null");
            map.put(entry.placeholder(), entry);
        }
        return map;
    }
    
    /**
     * 获取四边形列表，实现动态纹理替换
     * 
     * <p>优化要点：
     * 1. 提前验证状态有效性，无效时直接返回原四边形
     * 2. 复用结果列表，减少对象创建
     * 3. 使用局部变量缓存，减少字段访问
     * 4. 合并条件判断，减少分支预测失败
     */
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        List<BakedQuad> originalQuads = template.getQuads(state, side, rand, extraData, renderType);
        
        // 获取状态数组，优先使用 ModelData
        T[] states = getStates(state, extraData);
        
        // 无有效状态，直接返回原四边形（快速路径）
        if (states == null || states.length == 0) {
            return originalQuads;
        }
        
        // 获取线程本地的结果列表，复用减少 GC
        ArrayList<BakedQuad> result = resultListCache.get();
        result.clear();
        result.ensureCapacity(originalQuads.size());
        
        // 缓存局部变量，减少字段访问开销
        Map<ResourceLocation, TextureEntry<T>> placeholderMap = this.placeholderEntryMap;
        BiFunction<Direction, T[], T> stateProvider = this.stateForFaceProvider;
        ConcurrentHashMap<ResourceLocation, TextureAtlasSprite> cache = this.textureCache;
        
        // 遍历四边形，进行纹理替换
        for (BakedQuad quad : originalQuads) {
            // 跳过空四边形
            if (quad == null) {
                result.add(quad);
                continue;
            }
            
            TextureAtlasSprite originalSprite = quad.getSprite();
            // 跳过无纹理的四边形
            if (originalSprite == null) {
                result.add(quad);
                continue;
            }
            
            // O(1) 查找对应的纹理条目
            TextureEntry<T> entry = placeholderMap.get(originalSprite.contents().name());
            if (entry == null) {
                // 无匹配条目，使用原四边形
                result.add(quad);
                continue;
            }
            
            Direction quadFace = quad.getDirection();
            if (quadFace == null) {
                result.add(quad);
                continue;
            }
            
            // 根据面方向获取目标状态和纹理
            T stateKey = stateProvider.apply(quadFace, states);
            ResourceLocation targetLocation = entry.getTexture(stateKey);
            
            if (targetLocation == null) {
                // 该状态无映射，跳过此四边形（实现隐藏效果）
                continue;
            }
            
            // 获取或缓存目标纹理
            TextureAtlasSprite targetSprite = getOrCreateCachedSprite(cache, targetLocation);
            if (targetSprite == null) {
                result.add(quad);
                continue;
            }
            
            // 克隆四边形并重映射 UV
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
     * 获取或缓存纹理精灵（线程安全）
     * 
     * <p>优化要点：
     * 1. 使用 ConcurrentHashMap 的 computeIfAbsent 保证原子性
     * 2. 异常处理集中，减少重复代码
     */
    private static TextureAtlasSprite getOrCreateCachedSprite(
            ConcurrentHashMap<ResourceLocation, TextureAtlasSprite> cache,
            ResourceLocation location
    ) {
        // 先尝试快速获取
        TextureAtlasSprite cached = cache.get(location);
        if (cached != null) {
            return cached;
        }
        
        // 使用 computeIfAbsent 保证线程安全
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
     * 克隆四边形并进行 UV 重映射
     * 
     * <p>原理说明：
     * Minecraft 将所有纹理打包到图集中，每个纹理有独立的 UV 范围（如 0.5-0.6）。
     * BakedQuad 顶点存储的是图集中的绝对 UV 值。
     * 替换纹理时需要将原 UV 映射到新纹理的 UV 空间，保持纹理坐标相对位置不变。
     * 
     * <p>映射公式：
     * 相对位置 = (原始 UV - 原纹理起点) / 原纹理宽度
     * 新 UV = 目标纹理起点 + 相对位置 × 目标纹理宽度
     * 
     * <p>优化要点：
     * 1. 提前检查纹理是否相同，避免不必要的计算
     * 2. 提取纹理边界到局部变量，减少方法调用
     * 3. 使用内联常量进行边界检查
     * 4. 循环展开优化（4 个顶点固定）
     */
    private static BakedQuad cloneQuadWithTexture(BakedQuad quad, TextureAtlasSprite original, TextureAtlasSprite target) {
        // 纹理相同，直接返回原四边形
        if (original == target) {
            return quad;
        }
        
        // 复制顶点数据
        int[] vertexData = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
        
        // 获取纹理边界（提取到局部变量，减少方法调用）
        float origU0 = original.getU0(), origU1 = original.getU1();
        float origV0 = original.getV0(), origV1 = original.getV1();
        float tgtU0 = target.getU0(), tgtU1 = target.getU1();
        float tgtV0 = target.getV0(), tgtV1 = target.getV1();
        
        // 计算纹理尺寸
        float origW = origU1 - origU0;
        float origH = origV1 - origV0;
        float tgtW = tgtU1 - tgtU0;
        float tgtH = tgtV1 - tgtV0;
        
        // 尺寸无效时直接返回（不修改 UV）
        if (origW <= MIN_TEXTURE_SIZE || origH <= MIN_TEXTURE_SIZE 
                || tgtW <= MIN_TEXTURE_SIZE || tgtH <= MIN_TEXTURE_SIZE) {
            return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), target, quad.isShade());
        }
        
        // 计算缩放因子（避免重复除法）
        float scaleU = tgtW / origW;
        float scaleV = tgtH / origH;
        
        // 计算偏移量
        float offsetU = tgtU0 - origU0 * scaleU;
        float offsetV = tgtV0 - origV0 * scaleV;
        
        // 遍历四个顶点，重映射 UV 坐标
        for (int i = 0; i < 4; i++) {
            float srcU = BakedQuadHelper.getU(vertexData, i);
            float srcV = BakedQuadHelper.getV(vertexData, i);
            
            // 线性映射：新UV = 原UV × 缩放 + 偏移
            // 同时进行边界裁剪
            float newU = clamp(srcU * scaleU + offsetU, tgtU0, tgtU1);
            float newV = clamp(srcV * scaleV + offsetV, tgtV0, tgtV1);
            
            BakedQuadHelper.setU(vertexData, i, newU);
            BakedQuadHelper.setV(vertexData, i, newV);
        }
        
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), target, quad.isShade());
    }
    
    /**
     * 浮点数值裁剪
     */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * 获取状态数组，优先使用 ModelData，必要时回退到 BlockState
     * 
     * <p>优化要点：
     * 1. 短路求值，优先检查 ModelData
     * 2. 异常捕获集中，减少性能影响
     */
    private T[] getStates(BlockState state, ModelData extraData) {
        // 优先从 ModelData 获取（通常更及时）
        if (modelDataStateProvider != null && extraData != null) {
            try {
                T[] fromModelData = modelDataStateProvider.apply(extraData);
                if (isValidStates(fromModelData)) {
                    return fromModelData;
                }
            } catch (Exception e) {
                LOGGER.debug("Error getting states from ModelData: {}", e.getMessage());
            }
        }
        
        // 回退到 BlockState
        if (state != null) {
            try {
                T[] fromState = stateProvider.apply(state);
                if (isValidStates(fromState)) {
                    return fromState;
                }
            } catch (Exception e) {
                LOGGER.error("Error getting states from BlockState: {}", e.getMessage());
            }
        }
        
        return null;
    }
    
    /**
     * 验证状态数组有效性
     */
    private static <T> boolean isValidStates(T[] states) {
        return states != null && states.length > 0;
    }
    
    /**
     * 纹理条目：封装占位符和状态→纹理映射
     * 
     * @param <T> 状态类型
     */
    public record TextureEntry<T>(
            /** 占位符纹理 ResourceLocation */
            ResourceLocation placeholder,
            /** 状态→目标纹理路径的映射 */
            Map<T, ResourceLocation> textureMap
    ) {
        /**
         * 根据状态获取目标纹理
         * 
         * @param state 状态
         * @return 目标纹理路径，无映射时返回 null
         */
        public ResourceLocation getTexture(T state) {
            return textureMap.get(state);
        }
        
        /**
         * 验证条目有效性
         */
        public boolean isValid() {
            return placeholder != null && textureMap != null && !textureMap.isEmpty();
        }
    }
}
