package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 万能变速箱可视化类（六面轴版本）
 * <p>
 * Flywheel高性能渲染引擎支持，所有六个面都有传动轴。
 */
public class VersatileGearboxVisual extends KineticBlockEntityVisual<VersatileGearboxBlockEntity> {

    /**
     * 存储每个方向的旋转实例
     * <p>
     * Key: 方向（NORTH, SOUTH, EAST, WEST, UP, DOWN）
     * Value: 该方向的旋转实例
     */
    protected final EnumMap<Direction, RotatingInstance> keys = new EnumMap<>(Direction.class);

    /**
     * 当前动力源面的朝向
     * <p>
     * 用于计算各方向的旋转方向（正转/反转）
     * 如果没有动力源则为 null
     */
    protected Direction sourceFacing;

    public VersatileGearboxVisual(VisualizationContext context, VersatileGearboxBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        // 初始化动力源朝向
        updateSourceFacing();

        // 创建旋转实例的instancer
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));

        // 遍历所有6个方向（六面轴版本：所有面都有传动轴）
        for (Direction direction : Iterate.directions) {
            // OFF状态的半轴不渲染
            if (VersatileGearboxBlock.getShaftState(direction, blockState) == ShaftState.OFF)
                continue;

            final Direction.Axis axis = direction.getAxis();

            // 为该方向创建旋转实例
            RotatingInstance instance = instancer.createInstance();

            // 设置实例属性
            instance.setup(blockEntity, axis, getSpeed(direction))
                    .setPosition(getVisualPosition())
                    .rotateToFace(Direction.SOUTH, direction)
                    .setChanged();

            // 存储到Map中
            keys.put(direction, instance);
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
        // 获取方块实体的基本速度
        float speed = blockEntity.getSpeed();

        // 如果有动力源，应用旋转方向Modifier（三状态版本）
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
     * - 动态添加/删除半轴实例（根据状态变化）
     * - 为每个旋转实例更新速度和状态
     *
     * @param partialTick 部分tick值（用于插值）
     */
    @Override
    public void update(float partialTick) {
        // 先更新动力源朝向
        updateSourceFacing();

        // 检查是否有半轴需要添加或删除
        updateShaftInstances();

        // 更新每个方向的旋转实例
        for (Map.Entry<Direction, RotatingInstance> key : keys.entrySet()) {
            Direction direction = key.getKey();
            Direction.Axis axis = direction.getAxis();

            // 重新设置实例：更新关联的方块实体、轴方向、速度
            key.getValue()
                    .setup(blockEntity, axis, getSpeed(direction))
                    .setChanged();
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
            ShaftState state = VersatileGearboxBlock.getShaftState(direction, blockState);
            boolean hasInstance = keys.containsKey(direction);

            // OFF状态且有实例 → 删除实例
            if (state == ShaftState.OFF && hasInstance) {
                keys.remove(direction).delete();
            }
            // 非OFF状态且无实例 → 创建实例
            else if (state != ShaftState.OFF && !hasInstance) {
                RotatingInstance instance = instancer.createInstance();
                instance.setup(blockEntity, direction.getAxis(), getSpeed(direction))
                        .setPosition(getVisualPosition())
                        .rotateToFace(Direction.SOUTH, direction)
                        .setChanged();
                keys.put(direction, instance);
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
        // 使用FlatLit将光照更新应用到所有旋转实例
        relight(keys.values().toArray(FlatLit[]::new));
    }

    /**
     * 清理资源
     * <p>
     * 删除所有旋转实例并清空Map
     * 当可视化被移除时调用
     */
    @Override
    protected void _delete() {
        keys.values().forEach(AbstractInstance::delete);
        keys.clear();
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
        keys.values()
                .forEach(consumer);
    }
}