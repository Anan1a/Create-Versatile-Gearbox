package com.anan1a.create_versatile_gearbox.foundation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * <p>设计模式参考：
 * - TextureEntry模式：封装纹理映射配置
 * - 函数式接口：实现灵活的状态获取和纹理映射行为
 * - 多级缓存机制：避免重复查找纹理精灵
 *
 * <p>性能优化：
 * - 使用 HashSet 加速占位符查找（O(1)）
 * - 纹理缓存机制，减少运行时开销
 * - 渲染类型过滤，避免处理不需要的 quad
 *
 * <p>纹理替换机制（改进版）：
 * - 保留原始归一化 UV 坐标（0-1），只替换 sprite 引用
 * - 归一化 UV 直接表示纹理内的相对位置，与纹理图集位置无关
 * - 兼容 Create 的纹理连接机制（CTM），正确支持跨面纹理连接
 *
 * @param <T> 状态类型（如 VersatileGearboxShaftState、Enum 等）
 */
@OnlyIn(Dist.CLIENT)
public abstract class DynamicTextureModel<T> extends BakedModelWrapper<BakedModel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTextureModel.class);

    /**
     * 占位符纹理条目列表
     */
    protected final List<TextureEntry<T>> textureEntries;

    /**
     * 占位符集合，用于快速查找（O(1) 复杂度）
     */
    private final Set<ResourceLocation> placeholderSet = new HashSet<>();

    /**
     * 从 BlockState 获取状态数组的函数
     */
    protected final Function<BlockState, T[]> stateProvider;

    /**
     * 纹理缓存：避免每帧重复查找 TextureAtlasSprite
     */
    private final Map<ResourceLocation, TextureAtlasSprite> textureCache = new java.util.HashMap<>();

    /**
     * 构造函数（使用完整路径，不自动拼接）
     *
     * @param template        原始烘焙模型
     * @param textureEntries  纹理条目列表
     * @param stateProvider   从 BlockState 获取状态数组的函数
     */
    protected DynamicTextureModel(
            BakedModel template,
            List<TextureEntry<T>> textureEntries,
            Function<BlockState, T[]> stateProvider
    ) {
        super(template);
        this.textureEntries = textureEntries;
        this.stateProvider = stateProvider;

        for (TextureEntry<T> entry : textureEntries) {
            placeholderSet.add(entry.placeholder());
        }
    }

    /**
     * 构造函数（使用完整路径的Map形式，兼容旧API）
     *
     * @param template                 原始烘焙模型
     * @param placeholderTextureMaps   占位符→(状态→纹理路径)的映射
     * @param stateProvider            从 BlockState 获取状态数组的函数
     */
    protected DynamicTextureModel(
            BakedModel template,
            Map<ResourceLocation, Map<T, ResourceLocation>> placeholderTextureMaps,
            Function<BlockState, T[]> stateProvider
    ) {
        super(template);
        this.textureEntries = new ArrayList<>();

        placeholderTextureMaps.forEach((placeholder, textureMap) -> {
            this.textureEntries.add(new TextureEntry<>(placeholder, textureMap));
            this.placeholderSet.add(placeholder);
        });
        this.stateProvider = stateProvider;
    }

    /**
     * 构造函数（使用局部路径，自动拼接）
     *
     * @param template                原始烘焙模型
     * @param modId                   模组ID
     * @param textureBase             纹理基础路径（如 "block/example/"）
     * @param placeholderTextureMaps  占位符→(状态→纹理路径)的映射
     * @param stateProvider           从 BlockState 获取状态数组的函数
     */
    protected DynamicTextureModel(
            BakedModel template,
            String modId,
            String textureBase,
            Map<String, Map<T, String>> placeholderTextureMaps,
            Function<BlockState, T[]> stateProvider
    ) {
        super(template);
        this.textureEntries = new ArrayList<>();

        placeholderTextureMaps.forEach((placeholderName, texturePathMap) -> {
            ResourceLocation placeholder = createLocation(modId, textureBase, placeholderName);
            Map<T, ResourceLocation> textureMap = buildTextureMap(modId, textureBase, texturePathMap);
            this.textureEntries.add(new TextureEntry<>(placeholder, textureMap));
            this.placeholderSet.add(placeholder);
        });
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
     */
    private TextureAtlasSprite getOrCreateCachedSprite(ResourceLocation targetLocation) {
        TextureAtlasSprite cached = textureCache.get(targetLocation);
        if (cached != null) {
            return cached;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return null;
        }

        try {
            Function<ResourceLocation, TextureAtlasSprite> atlas = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
            TextureAtlasSprite sprite = atlas.apply(targetLocation);
            if (sprite != null) {
                textureCache.put(targetLocation, sprite);
            }
            return sprite;
        } catch (Exception e) {
            LOGGER.error("Error loading texture {}: {}", targetLocation, e.getMessage());
            return null;
        }
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

        if (!needsTextureModification(originalQuads)) {
            return originalQuads;
        }

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

            if (!placeholderSet.contains(originalLocation)) {
                quads.add(quad);
                continue;
            }

            TextureEntry<T> entry = findEntry(originalLocation);
            if (entry == null) {
                quads.add(quad);
                continue;
            }

            Direction quadFace = quad.getDirection();
            if (quadFace == null) {
                quads.add(quad);
                continue;
            }

            T stateKey = getStateForFace(quadFace, states);
            ResourceLocation targetLocation = entry.getTexture(stateKey);

            if (targetLocation == null) {
                continue;
            }

            TextureAtlasSprite targetSprite = getOrCreateCachedSprite(targetLocation);
            if (targetSprite == null) {
                quads.add(quad);
                continue;
            }

            BakedQuad newQuad = cloneQuadWithTexture(quad, originalSprite, targetSprite);
            quads.add(newQuad);
        }

        return quads;
    }

    /**
     * 检查是否需要纹理修改
     */
    private boolean needsTextureModification(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (quad == null) continue;
            TextureAtlasSprite sprite = quad.getSprite();
            if (sprite != null && placeholderSet.contains(sprite.contents().name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 克隆四边形并进行UV重映射以适配新纹理
     * 
     * <p>问题背景：
     * Minecraft 将所有纹理打包到一个大图集中，每个纹理在图集中有不同的位置。
     * BakedQuad 的顶点 UV 坐标存储的是纹理在图集中的绝对位置（0-1范围）。
     * 当替换纹理时，需要将原纹理的UV坐标映射到新纹理的坐标空间。
     * 
     * <p>映射算法：
     * 1. 计算原始UV在原纹理中的相对位置（0=左/上, 1=右/下）
     * 2. 将相对位置应用到目标纹理在图集中的位置
     * 
     * <p>示例：
     * - 原纹理在图集位置：U0=0.5, U1=0.6（宽度0.1）
     * - 目标纹理在图集位置：U0=0.7, U1=0.8（宽度0.1）
     * - 原始UV=0.55 → 相对位置=0.5 → 新UV=0.75
     * 
     * @param quad           原始四边形
     * @param originalSprite 原占位符纹理（用于计算UV偏移）
     * @param targetSprite   目标替换纹理
     * @return 替换纹理后的新四边形
     */
    private BakedQuad cloneQuadWithTexture(BakedQuad quad, TextureAtlasSprite originalSprite, TextureAtlasSprite targetSprite) {
        // 相同纹理无需处理
        if (originalSprite == targetSprite) {
            return quad;
        }

        // 复制顶点数据（避免修改原数据）
        int[] vertexData = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);

        // 获取原纹理在图集中的边界
        float origMinU = originalSprite.getU0();
        float origMaxU = originalSprite.getU1();
        float origMinV = originalSprite.getV0();
        float origMaxV = originalSprite.getV1();
        
        // 获取目标纹理在图集中的边界
        float tgtMinU = targetSprite.getU0();
        float tgtMaxU = targetSprite.getU1();
        float tgtMinV = targetSprite.getV0();
        float tgtMaxV = targetSprite.getV1();

        // 计算纹理尺寸（防止除零）
        float origWidth = origMaxU - origMinU;
        float origHeight = origMaxV - origMinV;
        float tgtWidth = tgtMaxU - tgtMinU;
        float tgtHeight = tgtMaxV - tgtMinV;

        // 边界检查：如果任一纹理尺寸无效，直接替换sprite（不修改UV）
        if (origWidth <= 0 || origHeight <= 0 || tgtWidth <= 0 || tgtHeight <= 0) {
            return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), targetSprite, quad.isShade());
        }

        // 遍历四个顶点，逐一重新映射UV
        for (int vertex = 0; vertex < 4; vertex++) {
            // 获取原始UV（图集中的绝对坐标）
            float srcU = BakedQuadHelper.getU(vertexData, vertex);
            float srcV = BakedQuadHelper.getV(vertexData, vertex);

            // 步骤1：计算在原纹理中的相对位置（0-1）
            float relU = (srcU - origMinU) / origWidth;
            float relV = (srcV - origMinV) / origHeight;
            
            // 安全裁剪：确保相对位置在有效范围内
            relU = clamp(relU, 0f, 1f);
            relV = clamp(relV, 0f, 1f);

            // 步骤2：将相对位置映射到目标纹理的图集坐标
            float newU = tgtMinU + relU * tgtWidth;
            float newV = tgtMinV + relV * tgtHeight;

            // 更新顶点UV
            BakedQuadHelper.setU(vertexData, vertex, newU);
            BakedQuadHelper.setV(vertexData, vertex, newV);
        }

        // 创建新四边形（使用新的sprite和更新后的UV）
        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), targetSprite, quad.isShade());
    }

    /**
     * 将值限制在指定范围内
     * 
     * @param value 输入值
     * @param min   最小值
     * @param max   最大值
     * @return 裁剪后的值
     */
    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 查找匹配的纹理条目
     */
    private TextureEntry<T> findEntry(ResourceLocation location) {
        for (TextureEntry<T> entry : textureEntries) {
            if (entry.placeholder().equals(location)) {
                return entry;
            }
        }
        return null;
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

    /**
     * 纹理条目：封装占位符纹理和状态→纹理映射
     * <p>
     * 参考 CopycatModelCore.ModelEntry 设计模式
     *
     * @param <T> 状态类型
     */
    public record TextureEntry<T>(
            ResourceLocation placeholder,
            Map<T, ResourceLocation> textureMap
    ) {
        public ResourceLocation getTexture(T state) {
            return textureMap.get(state);
        }

        public boolean containsState(T state) {
            return textureMap.containsKey(state);
        }
    }
}
