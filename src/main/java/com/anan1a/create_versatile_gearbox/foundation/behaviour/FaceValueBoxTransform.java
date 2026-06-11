package com.anan1a.create_versatile_gearbox.foundation.behaviour;

import java.util.function.Supplier;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 通用滑条交互框变换，每个滑条绑定一个面。
 * <p>
 * 负责三件事：
 * <ol>
 *   <li><b>定位</b> — widget 在面上的 voxel 坐标 (x, y)</li>
 *   <li><b>缩放</b> — widget 的大小比例</li>
 *   <li><b>激活判断</b> — 当面与绑定面一致且活跃条件满足时才显示/可交互</li>
 * </ol>
 */
public class FaceValueBoxTransform extends ValueBoxTransform.Sided {

    private static final float WIDGET_WIDTH = 6 / 12f;

    private final Direction face;
    private final float xPos;
    private final float yPos;
    private final Supplier<Boolean> activeCondition;

    /**
     * @param face           绑定的面方向
     * @param x              voxel x 坐标（0~16）
     * @param y              voxel y 坐标（0~16）
     * @param activeCondition 每次激活判断时调用，返回 true 才允许交互
     */
    public FaceValueBoxTransform(Direction face, float x, float y,
                                 Supplier<Boolean> activeCondition) {
        this.face = face;
        this.xPos = x;
        this.yPos = y;
        this.activeCondition = activeCondition;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(xPos, yPos, 16 - WIDGET_WIDTH);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return direction == face && activeCondition.get();
    }

    @Override
    public float getScale() {
        return WIDGET_WIDTH;
    }
}
