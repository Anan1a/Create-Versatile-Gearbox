package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

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
 * 万向齿轮箱方块的定制烘焙模型，根据轴的运行状态（正向、反向或关闭）动态切换每个面的纹理。
 * <p>
 * 使用 {@link DynamicTextureModel} 在运行时高效地进行纹理重映射，无需定义多个 blockstate 变体
 * 或 model override。注册了两个纹理条目：
 * <ul>
 *   <li><b>核心条目</b>：根据旋转方向，在正向核心纹理（{@link VersatileGearboxShaftState#FWD}）
 *       和反向核心纹理（{@link VersatileGearboxShaftState#REV}）之间切换。</li>
 *   <li><b>外壳条目</b>：当轴处于 {@link VersatileGearboxShaftState#OFF} 状态时
 *       （即没有轴），将关闭外壳纹理替换为安山岩机壳纹理。</li>
 * </ul>
 * <p>
 * 直接从 {@link BlockState} 属性读取每面状态，这是旧方块的标准实现方式。
 */
@OnlyIn(Dist.CLIENT)
public class VersatileGearboxModel extends BakedModelWrapper<BakedModel> {
    /** 万向齿轮箱方块纹理的基础路径。 */
    private static final String TEXTURE_BASE = "block/versatile_gearbox/";

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
    private static final List<DynamicTextureModel.TextureEntry<VersatileGearboxShaftState>> TEXTURE_ENTRIES = List.of(
            // 齿轮箱核心纹理映射：FWD→fwd_core，REV→rev_core
            new DynamicTextureModel.TextureEntry<>(
                    TEXTURE_FWD_CORE,
                    Map.of(
                        VersatileGearboxShaftState.FWD, TEXTURE_FWD_CORE,
                        VersatileGearboxShaftState.REV, TEXTURE_REV_CORE
                    )
            ),
            // 齿轮箱外壳纹理映射：OFF→andesite_casing（隐藏轴）
            new DynamicTextureModel.TextureEntry<>(
                    TEXTURE_OFF_SHELL,
                    Map.of(VersatileGearboxShaftState.OFF, TEXTURE_ANDESITE_CASING)
            )
    );

    /** 执行每面纹理重映射的底层动态纹理模型。 */
    private final DynamicTextureModel<VersatileGearboxShaftState> dynamicTextureModel;

    /**
     * 构造一个新的万向齿轮箱模型，包装给定的模板。
     *
     * @param template 来自 blockstate JSON 的原始烘焙模型，
     *                 包含将被动态重映射的占位纹理
     */
    public VersatileGearboxModel(BakedModel template) {
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
     * 解析指定面的轴状态。直接从 BlockState 属性读取。
     *
     * @param state 当前的方块状态
     * @param face  面方向
     * @param data  未使用（为保持与 DynamicTextureModel 接口兼容）
     * @return 解析后的轴状态，如果方块类型不正确则返回 {@link VersatileGearboxShaftState#OFF}
     */
    private VersatileGearboxShaftState resolveState(BlockState state, Direction face, ModelData data) {
        // 直接从 BlockState 属性读取轴状态
        // 如果方块不是 VersatileGearboxBlock，返回 OFF
        if (!(state.getBlock() instanceof VersatileGearboxBlock)) {
            return VersatileGearboxShaftState.OFF;
        }
        return VersatileGearboxBlock.getShaftState(face, state);
    }
}
