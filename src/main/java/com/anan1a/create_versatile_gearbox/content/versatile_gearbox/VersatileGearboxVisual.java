package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

/**
 * 万能变速箱可视化类（六面轴版本）
 * <p>
 * Flywheel高性能渲染引擎支持，所有六个面都有传动轴。
 */
public class VersatileGearboxVisual extends KineticBlockEntityVisual<VersatileGearboxBlockEntity> {

    /**
     * 存储每个方向的旋转实例（数组形式，优化访问速度）
     * <p>
     * 索引 0-5 对应 DOWN, UP, NORTH, SOUTH, WEST, EAST（与 Direction.values() 顺序一致）
     */
    protected final RotatingInstance[] keys = new RotatingInstance[6];

    /**
     * 当前动力源面的朝向
     * <p>
     * 用于计算各方向的旋转方向（正转/反转）
     * 如果没有动力源则为 null
     */
    protected Direction sourceFacing;

    /**
     * 脏标记，用于优化状态检查频率
     * <p>
     * true 表示需要检查并更新半轴实例
     */
    protected boolean dirty = true;

    /**
     * 构造函数
     *
     * @param context     可视化上下文
     * @param blockEntity 方块实体
     * @param partialTick 部分tick值（用于插值）
     */
    public VersatileGearboxVisual(VisualizationContext context, VersatileGearboxBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        updateSourceFacing();
        initShaftInstances();
    }

    /**
     * 初始化半轴实例
     * <p>
     * 根据方块状态创建非OFF状态的半轴旋转实例
     */
    private void initShaftInstances() {
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));
        for (Direction direction : Iterate.directions) {
            if (VersatileGearboxBlock.getShaftState(direction, blockState) == VersatileGearboxShaftState.OFF)
                continue;

            RotatingInstance instance = instancer.createInstance();
            instance.setup(blockEntity, direction.getAxis(), getSpeed(direction))
                    .setPosition(getVisualPosition())
                    .rotateToFace(Direction.SOUTH, direction)
                    .setChanged();
            keys[direction.ordinal()] = instance;
        }
    }

    /**
     * 计算指定方向的旋转速度
     * <p>
     * 【计算逻辑】
     * 1. 获取方块实体的基本速度（来自动力网络）
     * 2. 如果有动力源，根据轴方向关系和翻转属性调整速度符号
     * 3. 速度为负数表示反向旋转
     *
     * @param direction 要计算的方向（传动轴方向）
     * @return 该方向的旋转速度（可为负数表示反转）
     */
    private float getSpeed(Direction direction) {
        float speed = blockEntity.getSpeed();
        if (speed != 0 && sourceFacing != null) {
            speed *= VersatileGearboxBlockEntity.getRotationSpeedModifier(direction, sourceFacing, blockState);
        }
        return speed;
    }

    /**
     * 更新动力源朝向
     * <p>
     * 从方块实体的source字段计算动力源的实际朝向
     * source字段存储的是相对于方块位置的偏移量
     */
    protected void updateSourceFacing() {
        if (blockEntity.hasSource()) {
            // 计算从方块位置到动力源位置的偏移
            BlockPos source = blockEntity.source.subtract(pos);
            // 转换为最近的方向
            sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ());
        } else {
            sourceFacing = null;
        }
    }

    /**
     * 每帧更新可视化状态
     * <p>
     * 更新内容：
     * - 重新计算动力源朝向（因为连接状态可能改变）
     * - 动态添加/删除半轴实例（根据状态变化，仅在dirty=true时执行）
     * - 为每个旋转实例更新速度和状态
     * <p>
     * 【性能优化】使用dirty标记减少不必要的状态检查
     *
     * @param partialTick 部分tick值（用于插值）
     */
    @Override
    public void update(float partialTick) {
        updateSourceFacing();

        if (dirty) {
            updateShaftInstances();
            dirty = false;
        }

        for (int i = 0; i < 6; i++) {
            RotatingInstance instance = keys[i];
            if (instance != null) {
                Direction direction = Direction.values()[i];
                instance.setup(blockEntity, direction.getAxis(), getSpeed(direction)).setChanged();
            }
        }
    }

    /**
     * 动态更新半轴实例
     * <p>
     * 检查每个方向的状态，动态添加或删除旋转实例：
     * - OFF状态 → 删除实例（如果存在）
     * - 非OFF状态 → 添加实例（如果不存在）
     */
    protected void updateShaftInstances() {
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));
        for (Direction direction : Iterate.directions) {
            int idx = direction.ordinal();
            VersatileGearboxShaftState state = VersatileGearboxBlock.getShaftState(direction, blockState);
            RotatingInstance existing = keys[idx];

            if (state == VersatileGearboxShaftState.OFF && existing != null) {
                keys[idx] = null;
                existing.delete();
            } else if (state != VersatileGearboxShaftState.OFF && existing == null) {
                RotatingInstance instance = instancer.createInstance();
                instance.setup(blockEntity, direction.getAxis(), getSpeed(direction))
                        .setPosition(getVisualPosition())
                        .rotateToFace(Direction.SOUTH, direction)
                        .setChanged();
                keys[idx] = instance;
            }
        }
    }

    /**
     * 更新光照
     * <p>
     * 将光照信息传递给所有旋转实例
     */
    @Override
    public void updateLight(float partialTick) {
        for (RotatingInstance instance : keys) {
            if (instance != null) {
                relight(instance);
            }
        }
    }

    /**
     * 标记状态缓存失效
     * <p>
     * 当方块状态改变时调用此方法，触发下一帧的实例状态检查
     */
    public void invalidateCache() {
        this.dirty = true;
    }

    /**
     * 清理资源
     * <p>
     * 删除所有旋转实例
     * 当可视化被移除时调用
     */
    @Override
    protected void _delete() {
        for (RotatingInstance instance : keys) {
            if (instance != null) {
                instance.delete();
            }
        }
    }

    /**
     * 收集用于 crumbling 动画的实例
     * <p>
     * Crumbling是Create模组中物品被破坏时的动画效果
     * 此方法返回所有旋转实例以用于该动画
     *
     * @param consumer 实例消费者
     */
    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (RotatingInstance instance : keys) {
            if (instance != null) {
                consumer.accept(instance);
            }
        }
    }
}