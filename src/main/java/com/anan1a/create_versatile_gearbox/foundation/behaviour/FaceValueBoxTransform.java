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

    /** 缩放基准值：widget 宽度除以该值得到缩放比例。 */
    private static final float SCALE_BASE = 12f;

    /** 该滑条绑定的所有面方向。 */
    private final Set<Direction> faces;
    /** widget 在面上的 voxel x 坐标（0~16）。 */
    private final float xPos;
    /** widget 在面上的 voxel y 坐标（0~16）。 */
    private final float yPos;
    /** widget 的实际宽度（voxel 单位，用于定位和缩放）。 */
    private final float width;
    /** widget 沿 Z 轴的内缩偏移量（voxel 单位），越大越远离表面。 */
    private final float offset;
    /** 激活条件回调，返回 true 时才显示该滑条。 */
    private final Supplier<Boolean> activeCondition;

    /**
     * 构造函数，用于自定义宽度和偏移。
     * @param faces           绑定的面方向集合
     * @param x               voxel x 坐标（0~16）
     * @param y               voxel y 坐标（0~16）
     * @param width           widget 实际宽度（voxel 单位，用于定位和缩放）
     * @param offset          widget 沿 Z 轴内缩偏移量（voxel 单位），越大越远离表面
     * @param activeCondition 每次激活判断时调用，返回 true 才允许交互
     */
    public FaceValueBoxTransform(Set<Direction> faces, float x, float y, float width, float offset,
                                 Supplier<Boolean> activeCondition) {
        this.faces = faces;
        this.xPos = x;
        this.yPos = y;
        this.width = width;
        this.offset = offset;
        // 子类字段已赋值，可安全覆写父类构造器中 scale = getScale() 得到的错误值
        this.scale = width / SCALE_BASE;
        this.activeCondition = activeCondition;
    }

     /**
     * 构造函数，用于单面，自定义宽度和偏移。
     * @param face            绑定的面方向
     * @param x               voxel x 坐标（0~16）
     * @param y               voxel y 坐标（0~16）
     * @param width           widget 实际宽度（voxel 单位，用于定位和缩放）
     * @param offset          widget 沿 Z 轴内缩偏移量（voxel 单位），越大越远离表面
     * @param activeCondition 每次激活判断时调用，返回 true 才允许交互
     */
    public FaceValueBoxTransform(Direction face, float x, float y, float width, float offset,
                                 Supplier<Boolean> activeCondition) {
        this(Set.of(face), x, y, width, offset, activeCondition);
    }

    /**
     * 默认构造器，宽度默认 6，偏移默认 0。
     * <p>
     * 宽度默认为 6，与 {@link ValueBoxTransform} 相同。
     * @param faces           绑定的面方向集合
     * @param x               voxel x 坐标（0~16）
     * @param y               voxel y 坐标（0~16）
     * @param activeCondition 每次激活判断时调用，返回 true 才允许交互
     */
    public FaceValueBoxTransform(Set<Direction> faces, float x, float y,
                                 Supplier<Boolean> activeCondition) {
        this(faces, x, y, 6, 0, activeCondition);
    }

     /**
     * 默认构造器，用于单面，宽度默认 6，偏移默认 0。
     * @param face            绑定的面方向
     * @param x               voxel x 坐标（0~16）
     * @param y               voxel y 坐标（0~16）
     * @param activeCondition 每次激活判断时调用，返回 true 才允许交互
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
        return VecHelper.voxelSpace(xPos, yPos, 16 - width / SCALE_BASE + offset);
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
     * 返回 0.5（相对于6格像素如果材质是16x），使 widget 在面上约为半面大小。
     */
    @Override
    public float getScale() {
        // 父类构造器调用此方法时 width 未初始化（默认值 0），导致 scale = 0。
        // 已在构造器中用 this.scale = width / SCALE_BASE 覆写。
        // 保留此方法，用于后续可能的扩展。
        return width / SCALE_BASE;
    }
}
