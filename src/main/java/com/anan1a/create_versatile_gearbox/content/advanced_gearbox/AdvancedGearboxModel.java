package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.List;
import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;
import com.anan1a.create_versatile_gearbox.foundation.container.CompositeFaceContainer;

import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * 高级齿轮箱方块的定制烘焙模型，根据每面轴状态动态切换纹理。
 * <p>
 * 状态来源：{@link ModelData}，通过 {@link #gatherModelData} 从 BE 的
 * {@code getModelData()} 获取每面轴状态数组。
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedGearboxModel extends DynamicTextureModel<AdvancedGearboxShaftState> {

    /**
     * ModelData 属性键，存储每面轴状态数组。
     * BE 的 {@code getModelData()} 已填充此属性，
     * {@link #gatherModelData} 透传到 {@link ModelData}，
     * {@link #resolveState} 再从中读取当前面的状态。
     */
    public static final ModelProperty<Map<String, Object>[]> FACE_STATES =
            new ModelProperty<>();

    /** 高级齿轮箱方块纹理的基础路径。 */
    private static final String TEXTURE_BASE = "block/advanced_gearbox/";

    /** 关闭外壳替换使用的纹理（来自 Create 的黄铜机壳）。 */
    private static final ResourceLocation TEXTURE_BRASS_CASING =
            ResourceLocation.fromNamespaceAndPath("create", "block/brass_casing");
    /** 正向核心占位纹理。 */
    private static final ResourceLocation TEXTURE_FWD_CORE =
            ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "fwd_core");
    /** 反向核心目标纹理。 */
    private static final ResourceLocation TEXTURE_REV_CORE =
            ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "rev_core");
    /** 有轴外壳占位纹理（OFF 时替换为黄铜机壳，CFG 时替换为 1）。 */
    private static final ResourceLocation TEXTURE_OFF_SHELL =
            ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "off_shell");

    /** 注册两个动态纹理条目：核心（FWD/REV 切换）和外壳（OFF 时隐藏轴）。 */
    private static final List<TextureEntry<AdvancedGearboxShaftState>> TEXTURE_ENTRIES = List.of(
            new TextureEntry<>(
                    TEXTURE_FWD_CORE,
                    Map.of(
                            AdvancedGearboxShaftState.FWD, TEXTURE_FWD_CORE,
                            AdvancedGearboxShaftState.REV, TEXTURE_REV_CORE,
                            AdvancedGearboxShaftState.VAR,
                            ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "core")
                    )
            ),
            new TextureEntry<>(
                    TEXTURE_OFF_SHELL,
                    Map.of(
                            AdvancedGearboxShaftState.OFF, TEXTURE_BRASS_CASING,
                            AdvancedGearboxShaftState.CFG,
                            ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "1")
                    )
            )
    );

    public AdvancedGearboxModel(BakedModel template) {
        super(template, TEXTURE_ENTRIES);
    }

    /**
     * 将 BE 层级的 {@code blockEntityData} 中的面状态数组透传到模型层级的 ModelData，
     * 使 {@link #resolveState} 能在 {@code getQuads} 中读取到。
     * <p>
     * 之所以需要这一步，是因为 {@link com.simibubi.create.foundation.model.BakedModelWrapperWithData}
     * 在渲染前会创建一个全新的空 Builder，BE 的数据不会自动合并进来。
     */
    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter world,
                                      BlockPos pos, BlockState state, ModelData blockEntityData) {
        return builder.with(FACE_STATES, blockEntityData.get(FACE_STATES));
    }

    /**
     * 根据 ModelData 中的面状态数组，返回指定面的当前轴状态。
     * <p>
     * {@code data} 中的面状态数组来自 {@link #gatherModelData}，
     * 后者从 BE 的 {@code getModelData()} 透传而来。
     *
     * @param state 方块状态（未使用，保留以匹配父类签名）
     * @param face  当前渲染的面方向
     * @param data  模型层级的 ModelData（含 FACE_STATES 属性）
     * @return 该面的轴状态，数据无效时默认返回 OFF
     */
    @Override
    protected AdvancedGearboxShaftState resolveState(BlockState state, Direction face, ModelData data) {
        if (data != null && data.has(FACE_STATES)) {
            // 从 ModelData 中取出由 gatherModelData 透传的面映射数据数组
            Map<String, Object>[] fieldData = data.get(FACE_STATES);
            // 按字段键名 "FaceState" 和方向从映射数组中查找该面的枚举状态
            AdvancedGearboxShaftState shaftState = CompositeFaceContainer.getFieldValue(fieldData, "FaceState", face);
            if (shaftState != null) {
                return shaftState;
            }
        }
        return AdvancedGearboxShaftState.OFF;
    }
}
