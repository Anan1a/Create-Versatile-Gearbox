package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 万能变速箱方块实体
 * <p>
 * 继承 SplitShaftBlockEntity，实现多方向动力传输。
 * 核心逻辑：根据输出面与动力源面的轴方向关系决定旋转方向，
 * 支持每个输出面独立翻转（通过扳手切换）。
 */
public class VersatileGearboxBlockEntity extends SplitShaftBlockEntity {

    public VersatileGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 获取指定输出面的旋转速度Modifier
     *
     * @param face 输出面方向
     * @return 旋转速度倍率（0=关闭, 1=同向, -1=反向）
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 获取方块状态
        BlockState state = getBlockState();
        
        // 1. 检查输出面状态
        ShaftState faceState = VersatileGearboxBlock.getShaftState(face, state);
        if (faceState == ShaftState.OFF) {
            return 0;
        }
        
        // 2. 检查是否有动力源
        if (!hasSource()) {
            return 0;
        }
        
        // 3. 检查输入面状态
        Direction source = getSourceFacing();
        ShaftState sourceState = VersatileGearboxBlock.getShaftState(source, state);
        if (sourceState == ShaftState.OFF) {
            return 0;
        }
        
        // 4. 计算旋转方向
        return getRotationSpeedModifier(face, source, state);
    }

    /**
     * 根据动力源面计算指定输出面的旋转速度Modifier（静态方法）
     * <p>
     * 三状态版本：OFF(关闭), SAME(同向), OPPOSITE(反向)
     *
     * @param face   输出面
     * @param source 动力源面
     * @param state  方块状态
     * @return 旋转速度倍率（0=关闭, 1=同向, -1=反向）
     */
    public static float getRotationSpeedModifier(Direction face, Direction source, BlockState state) {
        // 获取输入面（动力源面）状态
        ShaftState sourceState = VersatileGearboxBlock.getShaftState(source, state);
        
        // 如果输入面关闭，所有输出都停止
        if (sourceState == ShaftState.OFF) {
            return 0;
        }

        // 获取输出面状态
        ShaftState faceState = VersatileGearboxBlock.getShaftState(face, state);
        
        // 如果输出面关闭，返回0（不输出动力）
        if (faceState == ShaftState.OFF) {
            return 0;
        }

        // 轴方向修正：同向为1，反向为-1
        int axisAdjust = face.getAxisDirection() == source.getAxisDirection() ? 1 : -1;
        // int axisAdjust = 1;

        // 输出面状态修正
        int faceModifier = faceState == ShaftState.FWD ? 1 : -1;
        
        // 动力源面状态修正（影响输出方向）
        int sourceModifier = sourceState == ShaftState.FWD ? 1 : -1;
        
        return axisAdjust * faceModifier * sourceModifier;
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }

    /**
     * 提供模型数据给渲染器
     * <p>
     * 返回六个面的状态，用于动态纹理替换
     */
    @Override
    public ModelData getModelData() {
        BlockState state = getBlockState();
        
        // 按顺序获取六个面的状态：DOWN, UP, NORTH, SOUTH, WEST, EAST
        ShaftState[] faceStates = new ShaftState[]{
            VersatileGearboxBlock.getShaftState(Direction.DOWN, state),
            VersatileGearboxBlock.getShaftState(Direction.UP, state),
            VersatileGearboxBlock.getShaftState(Direction.NORTH, state),
            VersatileGearboxBlock.getShaftState(Direction.SOUTH, state),
            VersatileGearboxBlock.getShaftState(Direction.WEST, state),
            VersatileGearboxBlock.getShaftState(Direction.EAST, state)
        };
        
        // 将面状态数组打包到 ModelData 中，传递给渲染器
        return ModelData.builder()
                .with(VersatileGearboxModel.FACE_STATES, faceStates)
                .build();
    }
}
