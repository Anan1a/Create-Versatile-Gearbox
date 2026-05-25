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
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * 多功能齿轮箱模型渲染器
 * 
 * <p>功能说明：
 * 根据六个面的轴状态（FWD/REV/OFF）动态渲染不同的纹理，实现齿轮箱的动态视觉效果。
 * 
 * <p>状态映射：
 * - FWD (正向旋转): 显示 fwd_core 纹理（通常为顺时针旋转的齿轮）
 * - REV (反向旋转): 显示 rev_core 纹理（通常为逆时针旋转的齿轮）
 * - OFF (关闭/无轴): 显示 off_shell 纹理，然后替换为 Andesite 机壳纹理
 * 
 * <p>面状态数组顺序：[DOWN, UP, NORTH, SOUTH, WEST, EAST]
 * 与 {@link #directionToIndex(Direction)} 的索引顺序对应。
 * 
 * <p>渲染流程：
 * 1. 从 ModelData 或 BlockState 获取六个面的轴状态
 * 2. 遍历模型的所有四边形
 * 3. 匹配占位符纹理并替换为对应的目标纹理
 * 4. 重映射 UV 坐标以适配新纹理
 * 
 * <p>依赖关系：
 * - {@link DynamicTextureModel}: 提供动态纹理替换核心逻辑
 * - {@link VersatileGearboxBlock}: 提供轴状态获取方法
 * - {@link VersatileGearboxShaftState}: 轴状态枚举定义
 * 
 * @see DynamicTextureModel
 * @see VersatileGearboxBlock
 * @see VersatileGearboxShaftState
 */
@OnlyIn(Dist.CLIENT)
public class VersatileGearboxModel extends BakedModelWrapper<BakedModel> {

    /**
     * 存储六个面轴状态的 ModelData 属性键
     * <p>用于在渲染时从 BlockEntity 获取实时状态数据，实现动态渲染。
     */
    public static final ModelProperty<VersatileGearboxShaftState[]> FACE_STATES = new ModelProperty<>();

    /** 纹理路径基础目录 */
    private static final String TEXTURE_BASE = "block/versatile_gearbox/";

    /** Create 模组的 Andesite 机壳纹理（用于 OFF 状态） */
    private static final ResourceLocation TEXTURE_ANDESITE_CASING = ResourceLocation.fromNamespaceAndPath("create", "block/andesite_casing");
    
    /** 正向旋转核心纹理 */
    private static final ResourceLocation TEXTURE_FWD_CORE = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "fwd_core");
    
    /** 反向旋转核心纹理 */
    private static final ResourceLocation TEXTURE_REV_CORE = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "rev_core");
    
    /** 关闭状态外壳纹理（占位符） */
    private static final ResourceLocation TEXTURE_OFF_SHELL = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "off_shell");

    /**
     * 核心纹理条目：处理旋转状态(FWD/REV)的纹理替换
     * <p>占位符: TEXTURE_FWD_CORE
     * <p>映射: FWD → FWD_CORE, REV → REV_CORE
     */
    private static final DynamicTextureModel.TextureEntry<VersatileGearboxShaftState> CORE_ENTRY = new DynamicTextureModel.TextureEntry<>(
            TEXTURE_FWD_CORE,
            Map.of(
                VersatileGearboxShaftState.FWD, TEXTURE_FWD_CORE,
                VersatileGearboxShaftState.REV, TEXTURE_REV_CORE
            )
    );

    /**
     * 外壳纹理条目：处理关闭状态的纹理替换
     * <p>占位符: TEXTURE_OFF_SHELL
     * <p>映射: OFF → ANDESITE_CASING
     */
    private static final DynamicTextureModel.TextureEntry<VersatileGearboxShaftState> SHELL_ENTRY = new DynamicTextureModel.TextureEntry<>(
            TEXTURE_OFF_SHELL,
            Map.of(VersatileGearboxShaftState.OFF, TEXTURE_ANDESITE_CASING)
    );

    /** 所有纹理条目的集合，传递给 DynamicTextureModel */
    private static final List<DynamicTextureModel.TextureEntry<VersatileGearboxShaftState>> TEXTURE_ENTRIES = List.of(CORE_ENTRY, SHELL_ENTRY);

    /**
     * 动态纹理模型实例，负责实际的纹理替换逻辑
     * <p>通过组合模式替代继承，实现代码解耦。
     */
    private final DynamicTextureModel<VersatileGearboxShaftState> dynamicTextureModel;

    /**
     * 构造函数，初始化模型包装器和动态纹理处理器
     * 
     * @param template 原始烘焙模型，作为纹理替换的基础
     */
    public VersatileGearboxModel(BakedModel template) {
        super(template);
        this.dynamicTextureModel = new DynamicTextureModel<>(
                template,
                TEXTURE_ENTRIES,
                VersatileGearboxModel::getStatesFromBlock,      // BlockState → 状态数组
                this::getStateForFace,                          // 面方向 → 状态
                this::getStatesFromModelData                    // ModelData → 状态数组
        );
    }

    /**
     * 获取四边形列表，实现动态纹理替换
     * <p>将渲染请求委托给内部的 DynamicTextureModel 处理。
     * 
     * @param state       当前方块状态
     * @param side        渲染面（可为 null 表示所有面）
     * @param rand        随机数源
     * @param extraData   额外模型数据（包含实时轴状态）
     * @param renderType  渲染类型
     * @return 处理后的四边形列表，纹理已根据状态替换
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        return dynamicTextureModel.getQuads(state, side, rand, extraData, renderType);
    }

    /**
     * 从 ModelData 获取六个面的轴状态数组
     * <p>优先使用 ModelData 中的实时状态，保证渲染的即时性。
     * 
     * @param data ModelData 实例
     * @return 六个面的轴状态数组（顺序：DOWN, UP, NORTH, SOUTH, WEST, EAST），无效时返回 null
     */
    private VersatileGearboxShaftState[] getStatesFromModelData(ModelData data) {
        // 验证 ModelData 有效性
        if (data != null && data.has(FACE_STATES)) {
            VersatileGearboxShaftState[] states = data.get(FACE_STATES);
            // 验证状态数组：必须非空且长度为6，且无 null 元素
            if (states != null && states.length == 6) {
                for (VersatileGearboxShaftState s : states) {
                    if (s == null) return null;
                }
                return states;
            }
        }
        return null;
    }

    /**
     * 根据面方向获取对应的轴状态
     * 
     * @param face   面方向
     * @param states 六个面的状态数组
     * @return 该面对应的轴状态，无效时返回 OFF
     */
    private VersatileGearboxShaftState getStateForFace(Direction face, VersatileGearboxShaftState[] states) {
        // 验证状态数组有效性
        if (states == null || states.length != 6) {
            return VersatileGearboxShaftState.OFF;
        }
        // 将方向转换为索引并获取状态
        int index = face.get3DDataValue();
        return (index >= 0 && index < 6) ? states[index] : VersatileGearboxShaftState.OFF;
    }

    /**
     * 从 BlockState 获取六个面的轴状态（回退方法）
     * <p>当 ModelData 不可用时使用此方法，从方块状态属性中提取轴状态。
     * <p>优化说明：使用 Direction.values() 循环遍历，避免重复代码，提升可维护性。
     * 
     * @param state 方块状态
     * @return 六个面的轴状态数组，非 VersatileGearboxBlock 时返回 null
     */
    private static VersatileGearboxShaftState[] getStatesFromBlock(BlockState state) {
        // 验证方块类型：确保传入的状态属于多功能齿轮箱方块
        if (!(state.getBlock() instanceof VersatileGearboxBlock block)) {
            return null;
        }
        // 按固定顺序收集六个面的轴状态
        // 数组索引顺序：DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5
        // 此顺序与 Direction.get3DDataValue() 返回值一一对应，确保索引映射的一致性
        return new VersatileGearboxShaftState[]{
                block.getShaftState(Direction.DOWN, state),    // 底面
                block.getShaftState(Direction.UP, state),      // 顶面
                block.getShaftState(Direction.NORTH, state),   // 北面（负Z方向）
                block.getShaftState(Direction.SOUTH, state),   // 南面（正Z方向）
                block.getShaftState(Direction.WEST, state),    // 西面（负X方向）
                block.getShaftState(Direction.EAST, state)     // 东面（正X方向）
        };
    }
}
