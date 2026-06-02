package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.List;
import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

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
 * <b>数据来源</b>：由 Mixin 在 {@code renderBatched} HEAD 处从 BlockEntity 直接读取面状态并写入
 * {@link #CURRENT_FACE_STATES}（ThreadLocal），模型在 {@link #resolveState} 中直接读取，
 * 避免替换原始 ModelData 导致连接纹理数据丢失。
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedGearboxModel extends BakedModelWrapper<BakedModel> {

    /** 当前渲染的面状态数组，由 Mixin 在 renderBatched HEAD 从 BE 直接读取后注入。 */
    public static final ThreadLocal<AdvancedGearboxShaftState[]> CURRENT_FACE_STATES = new ThreadLocal<>();

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
     * 解析指定面的轴状态。从 {@link #CURRENT_FACE_STATES} 读取，由 Mixin 在 renderBatched HEAD 注入。
     *
     * @param state 当前的方块状态（未使用，保留以匹配 {@link DynamicTextureModel} 函数签名）
     * @param face  面方向
     * @param data  ModelData（未使用，数据通过 ThreadLocal 桥接）
     * @return 解析后的轴状态，如果数据无效则返回 {@link AdvancedGearboxShaftState#OFF}
     */
    private AdvancedGearboxShaftState resolveState(BlockState state, Direction face, ModelData data) {
        AdvancedGearboxShaftState[] states = CURRENT_FACE_STATES.get();
        if (states != null && states.length == 6) {
            return states[face.get3DDataValue()];
        }
        return AdvancedGearboxShaftState.OFF;
    }
}
