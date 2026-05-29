package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.List;
import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;
import com.anan1a.create_versatile_gearbox.foundation.FaceStateContainer;

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
 * <b>双数据源读取策略</b><br>
 * 渲染时，每个面的状态通过 {@link #resolveState(BlockState, Direction, ModelData)} 决定，
 * 优先级如下：
 * <ol>
 *   <li><b>ModelData</b>（高优先级）— 来自 {@link AdvancedGearboxBlockEntity#getModelData()}，
 *       本质是 {@link FaceStateContainer#toArray()} 导出的运行时数组。
 *       数据源是 BlockEntity 的 NBT，突破 BlockState 枚举数量限制。</li>
 *   <li><b>BlockState 属性</b>（低优先级/回退）— 当 ModelData 不可用时（如无 BlockEntity、
 *       在 UI 中预览、Ponder 场景等），直接从 BlockState 的 EnumProperty 读取。</li>
 * </ol>
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedGearboxModel extends BakedModelWrapper<BakedModel> {

    /**
     * 模型属性 Key：用于在渲染时从 BlockEntity 的 ModelData 中提取面状态数组。
     * <p>
     * <b>完整数据流</b>
     * <pre>
     * BlockEntity.faceStates (FaceStateContainer)
     *   └─ .toArray() ──→ ModelData (FACE_STATES → AdvancedGearboxShaftState[6])
     *                        └─ data.get(FACE_STATES) ──→ resolveState() → DynamicTextureModel
     * </pre>
     * <p>
     * 存储方：{@link AdvancedGearboxBlockEntity#getModelData()} 中
     * {@code ModelData.builder().with(FACE_STATES, faceStates.toArray()).build()}
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
     * 解析指定面的轴状态。
     * <p>
     * <b>读取优先级</b>
     * <ol>
     *   <li>{@link #getStatesFromModelData(ModelData)} —
     *       从 ModelData（NBT 来源）读取，是主要数据源</li>
     *   <li>{@link #getStatesFromBlock(BlockState)} —
     *       当 ModelData 不可用时的回退方案，从 BlockState 属性读取</li>
     *   <li>{@link AdvancedGearboxShaftState#OFF} —
     *       两个数据源都失效时的最终保底值</li>
     * </ol>
     *
     * @param state 当前的方块状态
     * @param face  面方向
     * @param data  方块实体的模型数据
     * @return 解析后的轴状态
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
     * 从 ModelData 中提取面状态数组。
     * <p>
     * 执行三次校验确保数据完整性：
     * <ol>
     *   <li>{@code data.has(FACE_STATES)} — ModelData 中是否包含此属性</li>
     *   <li>{@code states.length == 6} — 数组长度必须恰好为 6（D/U/N/S/W/E）</li>
     *   <li>逐元素 null 检查 — 6 个状态都不能为 null（防止 NPE）</li>
     * </ol>
     * 任一条件不满足即返回 null，触发 {@link #resolveState} 回退到 BlockState。
     *
     * @param data 模型数据容器
     * @return 6 个轴状态的数组，数据无效时返回 null
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
     * 从 BlockState 属性读取面状态（回退方案）。
     * <p>
     * 适用场景：
     * <ul>
     *   <li>方块没有 BlockEntity（如被活塞推动过程中被破坏）</li>
     *   <li>在 UI 中预览方块（如 JEI/REI 显示）</li>
     *   <li>Ponder 场景中静态展示</li>
     *   <li>ModelData 数据损坏或未设置</li>
     * </ul>
     *
     * @param state 当前的方块状态
     * @return 6 个轴状态的数组（D/U/N/S/W/E 顺序），
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
