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
 * 每个面的状态首先从 {@link ModelData}（由方块实体设置）解析，如果模型数据不可用，
 * 则回退到 {@link BlockState} 属性。
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedGearboxModel extends BakedModelWrapper<BakedModel> {

    /**
     * 用于在渲染时从方块实体向模型传递每面轴状态的模型属性。
     * 存储一个包含 6 个 {@link AdvancedGearboxShaftState} 值的数组，
     * 每个 {@link Direction} 对应一个。
     * <p>
     * 存储方：{@link AdvancedGearboxBlockEntity#getModelData()} 中
     * {@code ModelData.builder().with(FACE_STATES, faceStates).build()}
     * <p>
     * 读取方：本类 {@link #getStatesFromModelData(ModelData)} 中
     * {@code data.has(FACE_STATES)} / {@code data.get(FACE_STATES)}
     */
    public static final ModelProperty<AdvancedGearboxShaftState[]> FACE_STATES = new ModelProperty<>();

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

    /** 齿轮箱核心的纹理条目：将 FWD 映射到 fwd_core，REV 映射到 rev_core。 */
    private static final DynamicTextureModel.TextureEntry<AdvancedGearboxShaftState> CORE_ENTRY = new DynamicTextureModel.TextureEntry<>(
            TEXTURE_FWD_CORE,
            Map.of(
                AdvancedGearboxShaftState.FWD, TEXTURE_FWD_CORE,
                AdvancedGearboxShaftState.REV, TEXTURE_REV_CORE
            )
    );

    /** 齿轮箱外壳的纹理条目：将 OFF 映射到安山岩机壳（隐藏轴）。 */
    private static final DynamicTextureModel.TextureEntry<AdvancedGearboxShaftState> SHELL_ENTRY = new DynamicTextureModel.TextureEntry<>(
            TEXTURE_OFF_SHELL,
            Map.of(AdvancedGearboxShaftState.OFF, TEXTURE_ANDESITE_CASING)
    );

    /** 注册用于动态重映射的所有纹理条目。 */
    private static final List<DynamicTextureModel.TextureEntry<AdvancedGearboxShaftState>> TEXTURE_ENTRIES = List.of(CORE_ENTRY, SHELL_ENTRY);

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
     * 解析指定面的轴状态。首先检查 ModelData，然后回退到 BlockState 属性。
     *
     * @param state 当前的方块状态
     * @param face  面方向
     * @param data  方块实体的模型数据
     * @return 解析后的轴状态，如果不可用则返回 {@link AdvancedGearboxShaftState#OFF}
     */
    private AdvancedGearboxShaftState resolveState(BlockState state, Direction face, ModelData data) {
        AdvancedGearboxShaftState[] states = getStatesFromModelData(data);
        if (states == null) {
            states = getStatesFromBlock(state);
        }
        if (states == null || states.length != 6) {
            return AdvancedGearboxShaftState.OFF;
        }
        int index = face.get3DDataValue();
        return (index >= 0 && index < 6) ? states[index] : AdvancedGearboxShaftState.OFF;
    }

    /**
     * 从方块实体的模型数据中提取每面轴状态。
     * 验证数组非空、恰好包含 6 个元素（每个面一个）且不包含空条目。
     *
     * @param data 模型数据容器
     * @return 包含 6 个轴状态的数组，如果数据缺失或无效则返回 null
     */
    private AdvancedGearboxShaftState[] getStatesFromModelData(ModelData data) {
        if (data != null && data.has(FACE_STATES)) {
            AdvancedGearboxShaftState[] states = data.get(FACE_STATES);
            if (states != null && states.length == 6) {
                for (AdvancedGearboxShaftState s : states) {
                    if (s == null) return null;
                }
                return states;
            }
        }
        return null;
    }

    /**
     * 回退方案：直接从方块状态属性中读取轴状态。
     * 在没有方块实体或模型数据可用时使用。
     *
     * @param state 当前的方块状态
     * @return 包含 6 个轴状态的数组（D=0, U=1, N=2, S=3, W=4, E=5），
     *         如果方块不是 {@link AdvancedGearboxBlock} 则返回 null
     */
    private static AdvancedGearboxShaftState[] getStatesFromBlock(BlockState state) {
        if (!(state.getBlock() instanceof AdvancedGearboxBlock)) {
            return null;
        }
        return new AdvancedGearboxShaftState[]{
                AdvancedGearboxBlock.getShaftState(Direction.DOWN, state),
                AdvancedGearboxBlock.getShaftState(Direction.UP, state),
                AdvancedGearboxBlock.getShaftState(Direction.NORTH, state),
                AdvancedGearboxBlock.getShaftState(Direction.SOUTH, state),
                AdvancedGearboxBlock.getShaftState(Direction.WEST, state),
                AdvancedGearboxBlock.getShaftState(Direction.EAST, state)
        };
    }
}