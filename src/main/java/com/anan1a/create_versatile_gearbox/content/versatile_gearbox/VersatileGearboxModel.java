package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.List;
import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;
import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * 多功能齿轮箱模型
 * <p>
 * 根据六个面的轴状态动态渲染不同的纹理。
 * 参考 CopycatModelCore 的设计模式，采用结构化的纹理配置方式。
 *
 * <p>性能优化：
 * - 使用静态 final 纹理常量，避免重复创建 ResourceLocation
 * - HashSet 加速占位符查找（基类实现）
 * - 纹理缓存机制，减少运行时开销（基类实现）
 */
public class VersatileGearboxModel extends DynamicTextureModel<VersatileGearboxShaftState> {

    /**
     * 状态属性：存储六个面的轴状态
     */
    public static final ModelProperty<VersatileGearboxShaftState[]> FACE_STATES = new ModelProperty<>();

    /**
     * 纹理基础路径常量
     */
    private static final String TEXTURE_BASE = "block/versatile_gearbox/";

    /**
     * 纹理路径常量 - FWD 状态纹理
     */
    private static final ResourceLocation TEXTURE_FWD = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "fwd");

    /**
     * 纹理路径常量 - REV 状态纹理
     */
    private static final ResourceLocation TEXTURE_REV = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "rev");

    /**
     * 纹理路径常量 - OFF 状态纹理
     */
    private static final ResourceLocation TEXTURE_OFF = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "off");

    /**
     * FWD/REV 状态的纹理条目
     * - FWD 状态显示 fwd 纹理
     * - REV 状态显示 rev 纹理
     * - OFF 状态不映射（自动隐藏）
     */
    private static final TextureEntry<VersatileGearboxShaftState> FWD_REV_ENTRY = new TextureEntry<>(
            TEXTURE_FWD,
            Map.of(
                VersatileGearboxShaftState.FWD, TEXTURE_FWD,
                VersatileGearboxShaftState.REV, TEXTURE_REV
            )
    );

    /**
     * OFF 状态的纹理条目
     * - OFF 状态显示 off 纹理
     * - FWD/REV 状态不映射（自动隐藏）
     */
    private static final TextureEntry<VersatileGearboxShaftState> OFF_ENTRY = new TextureEntry<>(
            TEXTURE_OFF,
            Map.of(
                VersatileGearboxShaftState.OFF, TEXTURE_OFF
            )
    );

    /**
     * 纹理条目列表
     */
    private static final List<TextureEntry<VersatileGearboxShaftState>> TEXTURE_ENTRIES = List.of(
            FWD_REV_ENTRY,
            OFF_ENTRY
    );

    /**
     * 构造函数
     *
     * @param template 原始烘焙模型
     */
    public VersatileGearboxModel(BakedModel template) {
        super(
            template,
            TEXTURE_ENTRIES,
            VersatileGearboxModel::getFaceStatesFromBlock
        );
    }

    /**
     * 从 ModelData 获取状态数组
     *
     * @param extraData 额外的数据
     * @return 状态数组
     */
    @Override
    protected VersatileGearboxShaftState[] getStatesFromModelData(ModelData extraData) {
        if (extraData != null && extraData.has(FACE_STATES)) {
            VersatileGearboxShaftState[] states = extraData.get(FACE_STATES);
            return validateStates(states);
        }
        return null;
    }

    /**
     * 验证状态数组的有效性
     *
     * @param states 状态数组
     * @return 有效的状态数组，如果无效则返回 null
     */
    private VersatileGearboxShaftState[] validateStates(VersatileGearboxShaftState[] states) {
        if (states != null && states.length == 6) {
            for (VersatileGearboxShaftState state : states) {
                if (state == null) {
                    return null;
                }
            }
            return states;
        }
        return null;
    }

    /**
     * 根据面方向获取对应的状态
     *
     * @param face   面方向
     * @param states 状态数组
     * @return 该面对应的状态
     */
    @Override
    protected VersatileGearboxShaftState getStateForFace(Direction face, VersatileGearboxShaftState[] states) {
        if (states == null || states.length != 6) {
            return VersatileGearboxShaftState.OFF;
        }

        int index = getDirectionIndex(face);
        if (index < 0 || index >= states.length) {
            return VersatileGearboxShaftState.OFF;
        }

        return states[index];
    }

    /**
     * 从 BlockState 获取状态数组
     *
     * @param state 状态
     * @return 状态数组
     */
    private static VersatileGearboxShaftState[] getFaceStatesFromBlock(BlockState state) {
        if (!(state.getBlock() instanceof VersatileGearboxBlock)) {
            return null;
        }
        return new VersatileGearboxShaftState[]{
            VersatileGearboxBlock.getShaftState(Direction.DOWN, state),
            VersatileGearboxBlock.getShaftState(Direction.UP, state),
            VersatileGearboxBlock.getShaftState(Direction.NORTH, state),
            VersatileGearboxBlock.getShaftState(Direction.SOUTH, state),
            VersatileGearboxBlock.getShaftState(Direction.WEST, state),
            VersatileGearboxBlock.getShaftState(Direction.EAST, state)
        };
    }

    /**
     * 获取方向索引
     *
     * @param direction 方向
     * @return 方向索引
     */
    private int getDirectionIndex(Direction direction) {
        return switch (direction) {
            case DOWN -> 0;
            case UP -> 1;
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
        };
    }
}
