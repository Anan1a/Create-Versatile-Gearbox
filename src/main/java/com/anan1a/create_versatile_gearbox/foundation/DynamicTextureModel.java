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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态纹理重映射模型的抽象基类。
 * <p>
 * 在渲染时拦截模板模型的 quad，将匹配已注册占位纹理的 quad 重映射为目标纹理。
 * 子类只需实现：
 * <ul>
 *   <li>{@link #resolveState(BlockState, Direction, ModelData)} — 为每个面提供状态键</li>
 *   <li>{@link #gatherModelData}（按需） — 若 {@code resolveState} 需要从 {@link ModelData} 读取数据</li>
 * </ul>
 * <p>
 * <b>关于 gatherModelData 的必要性：</b>{@link BakedModelWrapperWithData} 在渲染前会创建一个
 * 全新的空 {@link Builder} 并调用 {@link #gatherModelData}。即使 {@code blockEntityData}
 * （来自 {@code BE.getModelData()}）已包含所需数据，也必须在这个方法中显式写入 builder，
 * 否则数据不会进入最终的 {@link ModelData}，{@link #resolveState} 也就读不到。
 *
 * @param <T> 状态键类型，用于决定每个占位符应使用哪个纹理
 */
@OnlyIn(Dist.CLIENT)
public abstract class DynamicTextureModel<T> extends BakedModelWrapperWithData {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTextureModel.class);
    private static final int DEFAULT_QUAD_CAPACITY = 32;
    private static final float MIN_TEXTURE_SIZE = 1e-6f;

    /** 占位纹理位置到其对应 TextureEntry 的不可变映射表。 */
    private final Map<ResourceLocation, TextureEntry<T>> placeholderEntryMap;

    /**
     * 全局共享的纹理缓存，所有 DynamicTextureModel 实例共用一个。
     * 首次访问时从方块纹理图集加载，后续直接返回。
     */
    private static final ConcurrentHashMap<ResourceLocation, TextureAtlasSprite> GLOBAL_TEXTURE_CACHE = new ConcurrentHashMap<>();

    /** 线程本地可复用的 quad 结果列表，避免每帧渲染时分配新对象。 */
    private final ThreadLocal<ArrayList<BakedQuad>> resultListCache = ThreadLocal.withInitial(
            () -> new ArrayList<>(DEFAULT_QUAD_CAPACITY)
    );

    /**
     * @param template       提供 quad 来源的原始已烘焙模型
     * @param textureEntries 定义占位符到目标纹理映射关系的纹理条目列表
     */
    public DynamicTextureModel(BakedModel template, List<TextureEntry<T>> textureEntries) {
        super(template);
        Objects.requireNonNull(textureEntries, "textureEntries cannot be null");
        this.placeholderEntryMap = Collections.unmodifiableMap(buildPlaceholderMap(textureEntries));
    }

    /**
     * 根据面方向和 ModelData 解析当前状态键。由子类实现。
     *
     * @param state 当前方块状态
     * @param face  正在渲染的面方向
     * @param data  当前的 ModelData（含 {@link #gatherModelData} 写入的数据）
     * @return 状态键，该面无需纹理替换则返回 null
     */
    protected abstract T resolveState(BlockState state, Direction face, ModelData data);

    /**
     * 收集渲染所需的 ModelData。
     * <p>
     * 默认实现直接返回 builder，不添加任何数据。如果需要向 {@link ModelData} 写入
     * 供 {@link #resolveState} 读取的数据，子类应覆盖此方法，例如：
     * <pre>{@code return builder.with(MY_PROPERTY, blockEntityData.get(MY_PROPERTY));}</pre>
     */
    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter world,
                                      BlockPos pos, BlockState state, ModelData blockEntityData) {
        return builder;
    }

    /**
     * 将 quad 生成委托给动态纹理重映射逻辑。
     * <p>
     * 处理流程：获取模板模型的原始 quad → 检查精灵是否为已注册占位符 →
     * 通过 {@link #resolveState} 获取状态键 → 查找目标纹理 →
     * 克隆 quad 并重映射 UV 坐标。
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        // 1) 获取模板模型的原始 quad 列表
        List<BakedQuad> originalQuads = originalModel.getQuads(state, side, rand, extraData, renderType);

        // 2) 复用线程本地列表，避免每帧分配
        ArrayList<BakedQuad> result = resultListCache.get();
        result.clear();
        result.ensureCapacity(originalQuads.size());

        // 3) 将实例字段缓存到局部变量（性能优化，减少字段读取开销）
        Map<ResourceLocation, TextureEntry<T>> placeholderMap = this.placeholderEntryMap;

        // 4) 遍历每个 quad，决定是否替换纹理
        for (BakedQuad quad : originalQuads) {
            // 4a) 跳过空 quad
            if (quad == null) {
                result.add(quad);
                continue;
            }

            // 4b) 获取 quad 的纹理精灵 — 每个 quad 绑定一个 TextureAtlasSprite
            TextureAtlasSprite originalSprite = quad.getSprite();
            if (originalSprite == null) {
                result.add(quad);
                continue;
            }

            // 4c) 检查该精灵是否匹配已注册的占位纹理
            // placeholderMap: 占位纹理 ResourceLocation → TextureEntry
            // 不匹配说明是普通纹理（如边框、装饰），无需替换
            TextureEntry<T> entry = placeholderMap.get(originalSprite.contents().name());
            if (entry == null) {
                result.add(quad);
                continue;
            }

            // 4d) 获取 quad 的面方向 — 注意 side 是渲染视角方向，quadFace 才是实际面
            Direction quadFace = quad.getDirection();
            if (quadFace == null) {
                result.add(quad);
                continue;
            }

            // 4e) 通过子类的 resolveState 获取该面的当前状态键
            T stateKey = resolveState(state, quadFace, extraData);
            if (stateKey == null) {
                // 返回 null 表示该面不需要纹理替换
                result.add(quad);
                continue;
            }

            // 4f) 根据状态键查找目标纹理位置
            ResourceLocation targetLocation = entry.getTexture(stateKey);
            if (targetLocation == null) {
                // 该状态没有对应映射 → 丢弃此 quad（不显示占位纹理）
                continue;
            }

            // 4g) 加载（或从缓存获取）目标 TextureAtlasSprite
            TextureAtlasSprite targetSprite = getOrCreateCachedSprite(targetLocation);
            if (targetSprite == null) {
                // 纹理加载失败 → 保留原始 quad（至少能显示占位纹理）
                result.add(quad);
                continue;
            }

            // 4h) 克隆 quad 并将 UV 重映射到目标精灵
            BakedQuad newQuad = cloneQuadWithTexture(quad, originalSprite, targetSprite);
            result.add(newQuad);
        }

        return result;
    }

    // ===== 内部工具方法 =====

    /**
     * 将纹理条目列表构建为占位纹理 → TextureEntry 的查找映射表。
     * 结果不可变，供 {@link #getQuads} 中 O(1) 查找使用。
     */
    private static <T> Map<ResourceLocation, TextureEntry<T>> buildPlaceholderMap(List<TextureEntry<T>> entries) {
        Map<ResourceLocation, TextureEntry<T>> map = new HashMap<>(entries.size());
        for (TextureEntry<T> entry : entries) {
            Objects.requireNonNull(entry, "texture entry cannot be null");
            Objects.requireNonNull(entry.placeholder(), "placeholder cannot be null");
            map.put(entry.placeholder(), entry);
        }
        return map;
    }

    /**
     * 从全局缓存中获取 TextureAtlasSprite，如未缓存则从方块纹理图集加载。
     * 使用 {@link ConcurrentHashMap#computeIfAbsent} 确保线程安全的懒加载，
     * 所有 {@link DynamicTextureModel} 实例共用同一缓存。
     *
     * @param location 目标纹理的资源位置
     * @return 加载的精灵，加载失败时返回 null
     */
    private static TextureAtlasSprite getOrCreateCachedSprite(ResourceLocation location) {
        return GLOBAL_TEXTURE_CACHE.computeIfAbsent(location, loc -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                LOGGER.warn("Minecraft instance not available for texture loading");
                return null;
            }
            try {
                return mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(loc);
            } catch (Exception e) {
                LOGGER.error("Failed to load texture: {}", loc, e);
                return null;
            }
        });
    }

    /**
     * 克隆 quad 并将 UV 坐标从原始精灵区域线性缩放到目标精灵的对应区域，
     * 保留纹理特征在精灵内的相对位置。
     * <p>
     * UV 重映射公式（以 U 为例，V 同理）：
     * <pre>
     * scaleU = tgtW / origW
     * offsetU = tgtU0 - origU0 × scaleU
     * newU = srcU × scaleU + offsetU
     * </pre>
     * 直观理解：将 srcU 从 original 的 UV 空间映射到 target 的 UV 空间。
     * 例如原始精灵中位于 (u0%, v0%) 的纹理点，映射后也出现在目标精灵的 (u0%, v0%) 位置。
     * 结果会被 clamp 到目标精灵的 UV 边界内，防止纹理溢出。
     */
    private static BakedQuad cloneQuadWithTexture(BakedQuad quad, TextureAtlasSprite original, TextureAtlasSprite target) {
        // 同一精灵则无需重映射
        if (original == target) {
            return quad;
        }

        // 复制顶点数据（后续在原数据上直接修改 UV）
        int[] vertexData = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);

        // 获取两个精灵的 UV 边界
        // U0/V0 = 左上角 UV，U1/V1 = 右下角 UV（均为图集 UV 坐标，非归一化）
        float origU0 = original.getU0(), origU1 = original.getU1();
        float origV0 = original.getV0(), origV1 = original.getV1();
        float tgtU0 = target.getU0(), tgtU1 = target.getU1();
        float tgtV0 = target.getV0(), tgtV1 = target.getV1();

        // 精灵在各自 UV 空间的宽高
        float origW = origU1 - origU0;
        float origH = origV1 - origV0;
        float tgtW = tgtU1 - tgtU0;
        float tgtH = tgtV1 - tgtV0;

        // 纹理尺寸过小时跳过重映射，防止除零或数值不稳定
        if (origW <= MIN_TEXTURE_SIZE || origH <= MIN_TEXTURE_SIZE
                || tgtW <= MIN_TEXTURE_SIZE || tgtH <= MIN_TEXTURE_SIZE) {
            return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), target, quad.isShade());
        }

        // 计算 UV 线性变换参数
        // scale:       目标精灵宽度/高度 ÷ 原始精灵宽度/高度（缩放因子）
        // offset:      将原始精灵的 UV 原点偏移到目标精灵的 UV 原点
        float scaleU = tgtW / origW;
        float scaleV = tgtH / origH;
        float offsetU = tgtU0 - origU0 * scaleU;
        float offsetV = tgtV0 - origV0 * scaleV;

        // 遍历 4 个顶点，逐个重映射 UV
        for (int i = 0; i < 4; i++) {
            float srcU = BakedQuadHelper.getU(vertexData, i);
            float srcV = BakedQuadHelper.getV(vertexData, i);
            // newUV = srcUV × scale + offset（线性映射，保持相对位置）
            float newU = clamp(srcU * scaleU + offsetU, tgtU0, tgtU1);
            float newV = clamp(srcV * scaleV + offsetV, tgtV0, tgtV1);
            BakedQuadHelper.setU(vertexData, i, newU);
            BakedQuadHelper.setV(vertexData, i, newV);
        }

        return new BakedQuad(vertexData, quad.getTintIndex(), quad.getDirection(), target, quad.isShade());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 定义动态纹理映射条目。将占位纹理位置与一个从状态键到目标纹理位置的映射表配对。
     *
     * @param <T>         状态键类型
     * @param placeholder 模型中占位纹理的资源位置
     * @param textureMap  从状态键到目标纹理资源位置的映射表
     */
    public record TextureEntry<T>(
            ResourceLocation placeholder,
            Map<T, ResourceLocation> textureMap
    ) {
        /** 根据状态键查找目标纹理位置。 */
        public ResourceLocation getTexture(T state) {
            return textureMap.get(state);
        }

        /** 此条目是否有效（非空占位符且至少一个映射）。 */
        public boolean isValid() {
            return placeholder != null && textureMap != null && !textureMap.isEmpty();
        }
    }
}
