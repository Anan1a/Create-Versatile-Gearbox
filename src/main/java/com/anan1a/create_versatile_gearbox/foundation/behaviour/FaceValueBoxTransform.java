package com.anan1a.create_versatile_gearbox.foundation.behaviour;

import java.util.Set;
import java.util.function.Supplier;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 通用滑条交互框变换，每个滑条绑定一个或多个面。
 * <p>
 * 负责三件事：
 * <ol>
 *   <li><b>定位</b> — widget 在面上的 voxel 坐标 (x, y)</li>
 *   <li><b>缩放</b> — widget 的大小比例</li>
 *   <li><b>激活判断</b> — 当面与绑定面一致且活跃条件满足时才显示/可交互</li>
 * </ol>
 */
public class FaceValueBoxTransform extends ValueBoxTransform.Sided {

    /** widget 相对于整面的宽度比例（6/12 = 0.5）。 */
    private static final float WIDGET_WIDTH = 6 / 12f;

    /** 该滑条绑定的所有面方向。 */
    private final Set<Direction> faces;
    /** widget 在面上的 voxel x 坐标（0~16）。 */
    private final float xPos;
    /** widget 在面上的 voxel y 坐标（0~16）。 */
    private final float yPos;
    /** 激活条件回调，返回 true 时才显示该滑条。 */
    private final Supplier<Boolean> activeCondition;

    /**
     * @param faces           绑定的面方向集合
     * @param x               voxel x 坐标（0~16）
     * @param y               voxel y 坐标（0~16）
     * @param activeCondition 每次激活判断时调用，返回 true 才允许交互
     */
    public FaceValueBoxTransform(Set<Direction> faces, float x, float y,
                                 Supplier<Boolean> activeCondition) {
        this.faces = faces;
        this.xPos = x;
        this.yPos = y;
        this.activeCondition = activeCondition;
    }

    /**
     * 单面快捷构造器。
     */
    public FaceValueBoxTransform(Direction face, float x, float y,
                                 Supplier<Boolean> activeCondition) {
        this(Set.of(face), x, y, activeCondition);
    }

    // ————— ValueBoxTransform.Sided 抽象方法实现 —————

    /**
     * 返回 widget 在面上的 3D 位置。
     * <p>
     * {@link VecHelper#voxelSpace} 将 0~16 的格内坐标映射为世界坐标。
     * z = 16 - WIDGET_WIDTH 将 widget 置于面表面稍内一点，避免 Z-fighting。
     */
    @Override
    protected Vec3 getSouthLocation() {
        return VecHelper.voxelSpace(xPos, yPos, 16 - WIDGET_WIDTH);
    }

    /**
     * 判断该方向是否应显示此滑条。
     * <p>
     * 只有点击的面在 {@link #faces} 集合中且 {@link #activeCondition} 为 true 时才显示。
     */
    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return faces.contains(direction) && activeCondition.get();
    }

    /**
     * widget 渲染缩放比例。
     * <p>
     * 返回 0.5（相对于整面 1.0），使 widget 在面上约为半面大小。
     */
    @Override
    public float getScale() {
        return WIDGET_WIDTH;
    }
}
