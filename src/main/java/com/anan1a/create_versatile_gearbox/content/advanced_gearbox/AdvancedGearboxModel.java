package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.List;
import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import com.simibubi.create.foundation.model.BakedModelWrapperWithData;

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
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
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
 * <b>数据来源</b>：通过 {@link #gatherModelData} 在模型层级直接从世界读取 BlockEntity，
 * 使用 {@link BakedModelWrapperWithData} 提供的模型级 {@code getModelData()} 钩子。
 * 所有渲染路径（renderBatched / renderSingleBlock）均经过此方法，无需 Mixin 或 ThreadLocal。
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedGearboxModel extends BakedModelWrapperWithData {

    /**
     * ModelData 属性键，用于存储面状态数组。
     * 在 {@link #gatherModelData} 中从 BlockEntity 读取后填入，
     * 供 {@link #resolveState} 在 {@code getQuads()} 中读取。
     */
    public static final ModelProperty<AdvancedGearboxShaftState[]> FACE_STATES =
            new ModelProperty<>();

    /** 高级齿轮箱方块纹理的基础路径。 */
    private static final String TEXTURE_BASE = "block/advanced_gearbox/";

    /** 关闭外壳替换使用的纹理（来自 Create 的安山岩机壳）。 */
    private static final ResourceLocation TEXTURE_BRASS_CASING = ResourceLocation.fromNamespaceAndPath("create", "block/brass_casing");
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
                        AdvancedGearboxShaftState.REV, TEXTURE_REV_CORE,
                        AdvancedGearboxShaftState.VAR, ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "core")
                    )
            ),
            // 齿轮箱外壳纹理映射：OFF→andesite_casing（隐藏轴）
            new DynamicTextureModel.TextureEntry<>(
                    TEXTURE_OFF_SHELL,
                    Map.of(AdvancedGearboxShaftState.OFF, TEXTURE_BRASS_CASING)
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
     * 在模型层级收集渲染数据。由 NeoForge 在每个方块渲染前调用，
     * 覆盖所有渲染路径（renderBatched、renderSingleBlock 等）。
     * <p>
     * 只需透传 BE 层级已准备好的面状态数据 — NeoForge 渲染管线在调用此方法前，
     * 已先调用 {@code BlockEntity.getModelData()}，结果作为
     * {@code blockEntityData} 参数传入。数据来源已在 BE 层级统一。
     *
     * @param builder          ModelData 构建器
     * @param world            世界访问器（未使用，数据来自 BE 层级钩子）
     * @param pos              方块位置（未使用）
     * @param state            方块状态（未使用）
     * @param blockEntityData  BE 层级 {@code getModelData()} 的结果，
     *                         已包含 {@link #FACE_STATES}
     * @return ModelData 构建器（已加入面状态数据）
     */
    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter world,
                                       BlockPos pos, BlockState state, ModelData blockEntityData) {
        // blockEntityData 是 NeoForge 从 BE.getModelData() 获取后传入的，
        // 已完成 FACE_STATES 填充，直接透传
        return builder.with(FACE_STATES, blockEntityData.get(FACE_STATES));
    }

    /**
     * 将 quad 生成委托给 {@link DynamicTextureModel}，后者拦截模板中的 quad
     * 并根据每面轴状态重映射纹理。
     *
     * @param state      当前方块的 BlockState（由 Minecraft 渲染系统传入，反映方块属性）
     * @param side       当前正在渲染的面方向（Minecraft 依次传入每个面）
     * @param rand       随机数源（透传给模板模型）
     * @param extraData  ModelData 运行时数据容器，由 {@link #gatherModelData} 构建，
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
     * 解析指定面的轴状态。仅从 ModelData 读取。
     * <p>
     * ModelData 由 {@link #gatherModelData} 在所有渲染路径中统一构建，
     * 无需双通道回退或 ThreadLocal。
     *
     * @param state 当前的方块状态（未使用，保留以匹配 {@link DynamicTextureModel} 函数签名）
     * @param face  面方向
     * @param data  ModelData，来自 {@code getQuads} 的 extraData 参数
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
