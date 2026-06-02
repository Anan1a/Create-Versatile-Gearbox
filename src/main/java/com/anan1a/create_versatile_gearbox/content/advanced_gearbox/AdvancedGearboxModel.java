package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * 高级齿轮箱方块的定制烘焙模型，根据轴的运行状态（正向、反向或关闭）动态切换每个面的纹理。
 * <p>
 * 使用 {@link DynamicTextureModel} 在运行时高效地进行纹理重映射，无需定义多个 blockstate 变体
 * 或 model override。注册了两个纹理条目：
 * <ul>
 *   <li><b>核心条目</b>：根据旋转方向，在正向核心纹理（{@link AdvancedGearboxShaftState#FWD}）
 *       和反向核心纹理（{@link AdvancedGearboxShaftState#REV}）之间切换。</li>
 *   <li><b>外壳条目</b>：当轴处于 {@link AdvancedGearboxShaftState#OFF} 状态时
 *       （即没有轴），将关闭外壳纹理替换为安山岩机壳纹理。</li>
 * </ul>
 * <p>
 * <b>数据来源</b>：从 {@link ModelData} 读取每面状态，由 {@link AdvancedGearboxBlockEntity#getModelData()}
 * 提供。使用 {@link #FACE_STATES_CACHE} 静态缓存解决渲染管线中 ModelData 传递断裂的问题。
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedGearboxModel extends BakedModelWrapper<BakedModel> {

    /** ModelData 键：存储六个面的轴状态数组 */
    public static final ModelProperty<AdvancedGearboxShaftState[]> FACE_STATES = new ModelProperty<>();

    /**
     * 面状态静态缓存：解决渲染管线中 ModelData 传递断裂问题。
     * <p>
     * 由 {@link AdvancedGearboxBlockEntity} 的 {@code setShaftState()}、{@code read()}、{@code getModelData()}
     * 方法写入，由 {@link #resolveState(BlockState, Direction, ModelData)} 和 Mixin 读取。
     */
    public static final ConcurrentHashMap<BlockPos, AdvancedGearboxShaftState[]> FACE_STATES_CACHE = new ConcurrentHashMap<>();

    /**
     * 更新面状态缓存。
     *
     * @param pos    方块位置（将被转为不可变）
     * @param states 六个面的轴状态数组
     */
    public static void updateCache(BlockPos pos, AdvancedGearboxShaftState[] states) {
        if (pos != null && states != null) {
            FACE_STATES_CACHE.put(pos.immutable(), states);
        }
    }

    /**
     * 从缓存中移除指定位置的面状态。
     *
     * @param pos 方块位置
     */
    public static void removeFromCache(BlockPos pos) {
        if (pos != null) {
            FACE_STATES_CACHE.remove(pos);
        }
    }

    /** 高级齿轮箱方块纹理的基础路径。 */
    private static final String TEXTURE_BASE = "block/advanced_gearbox/";

    /** 关闭外壳替换使用的纹理（来自 Create 的安山岩机壳）。 */
    private static final ResourceLocation TEXTURE_ANDESITE_CASING = ResourceLocation.fromNamespaceAndPath("create", "block/andesite_casing");
    /** 正向核心状态的占位纹理。 */
    private static final ResourceLocation TEXTURE_FWD_CORE = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "fwd_core");
    /** 反向核心状态的目标纹理。 */
    private static final ResourceLocation TEXTURE_REV_CORE = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "rev_core");
    /** 有轴存在时外壳的占位纹理。 */
    private static final ResourceLocation TEXTURE_OFF_SHELL = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "off_shell");

    /**
     * 注册用于动态重映射的所有纹理条目：
     * <ul>
     *   <li><b>核心条目</b>：根据旋转方向，在正向核心纹理（FWD）和反向核心纹理（REV）之间切换</li>
     *   <li><b>外壳条目</b>：当轴处于 OFF 状态时，将外壳纹理替换为安山岩机壳</li>
     * </ul>
     */
    private static final List<DynamicTextureModel.TextureEntry<AdvancedGearboxShaftState>> TEXTURE_ENTRIES = List.of(
            // 齿轮箱核心纹理映射：FWD→fwd_core，REV→rev_core
            new DynamicTextureModel.TextureEntry<>(
                    TEXTURE_FWD_CORE,
                    Map.of(
                        AdvancedGearboxShaftState.FWD, TEXTURE_FWD_CORE,
                        AdvancedGearboxShaftState.REV, TEXTURE_REV_CORE
                    )
            ),
            // 齿轮箱外壳纹理映射：OFF→andesite_casing（隐藏轴）
            new DynamicTextureModel.TextureEntry<>(
                    TEXTURE_OFF_SHELL,
                    Map.of(AdvancedGearboxShaftState.OFF, TEXTURE_ANDESITE_CASING)
            )
    );

    /** 执行每面纹理重映射的底层动态纹理模型。 */
    private final DynamicTextureModel<AdvancedGearboxShaftState> dynamicTextureModel;

    /**
     * 构造一个新的高级齿轮箱模型，包装给定的模板。
     *
     * @param template 来自 blockstate JSON 的原始烘焙模型，
     *                 包含将被动态重映射的占位纹理
     */
    public AdvancedGearboxModel(BakedModel template) {
        super(template);
        this.dynamicTextureModel = new DynamicTextureModel<>(
                template,
                TEXTURE_ENTRIES,
                this::resolveState
        );
    }

    /**
     * 返回模型数据。由于渲染管线中 ModelData 传递存在问题，实际数据由 Mixin 在
     * {@link #FACE_STATES_CACHE} 中读取并注入。
     *
     * @param level      方块访问器
     * @param pos        方块位置
     * @param state      当前方块状态
     * @param modelData  输入的 ModelData（透传）
     * @return 透传的 ModelData
     */
    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        return modelData;
    }

    /**
     * 将 quad 生成委托给 {@link DynamicTextureModel}，后者拦截模板中的 quad
     * 并根据每面轴状态重映射纹理。
     *
     * @param state      当前方块的 BlockState（由 Minecraft 渲染系统传入，反映方块属性）
     * @param side       当前正在渲染的面方向（Minecraft 依次传入每个面）
     * @param rand       随机数源（透传给模板模型）
     * @param extraData  ModelData 运行时数据容器，来自 BlockEntity.getModelData()，
     *                   包含每面轴状态等渲染所需数据
     * @param renderType 渲染类型（例如 solid、cutout）
     * @return 经过动态纹理重映射的已烘焙 quad 列表
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        return dynamicTextureModel.getQuads(state, side, rand, extraData, renderType);
    }

    /**
     * 解析指定面的轴状态。从 ModelData 读取每面状态，由 Mixin 注入。
     *
     * @param state 当前的方块状态
     * @param face  面方向
     * @param data  ModelData 数据容器，包含每面轴状态
     * @return 解析后的轴状态，如果数据无效则返回 {@link AdvancedGearboxShaftState#OFF}
     */
    private AdvancedGearboxShaftState resolveState(BlockState state, Direction face, ModelData data) {
        if (data != null && data.has(FACE_STATES)) {
            AdvancedGearboxShaftState[] states = data.get(FACE_STATES);
            if (states != null && states.length == 6) {
                return states[face.get3DDataValue()];
            }
        }
        return AdvancedGearboxShaftState.OFF;
    }
}
