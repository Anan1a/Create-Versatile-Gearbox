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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 万能变速箱可视化类（用于Flywheel渲染引擎）
 * <p>
 * 【Flywheel简介】
 * Flywheel是Create模组的高性能渲染引擎，用于优化大量旋转方块的渲染性能
 * 当玩家开启"可视化"选项时，方块实体的渲染会从原版渲染器切换到Flywheel
 * <p>
 * 【核心功能】
 * - 为每个传动轴方向创建独立的旋转实例（RotatingInstance）
 * - 根据动力源方向计算每个轴的实际旋转速度（考虑正转/反转）
 * - 支持动态更新旋转状态（当连接关系变化时）
 * <p>
 * 【轴概念区分】
 * - 方向轴（箱体轴）：由 BlockState.AXIS 定义，决定箱体朝向
 * - 传动轴：垂直于方向轴的半轴，每个传动轴围绕自己的轴独立旋转
 * <p>
 * 【可视化流程】
 * 1. 初始化：遍历6个方向，跳过与方向轴平行的方向
 * 2. 创建实例：为每个传动轴方向创建旋转实例
 * 3. 更新：每帧更新旋转速度和状态
 * 4. 清理：删除时销毁所有实例
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

        // 获取箱体轴方向（由方块状态决定）
        final Direction.Axis boxAxis = blockState.getValue(BlockStateProperties.AXIS);

        // 初始化动力源朝向
        updateSourceFacing();

        // 创建旋转实例的instancer
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));

        // 遍历所有6个方向
        for (Direction direction : Iterate.directions) {
            final Direction.Axis axis = direction.getAxis();

            // 跳过与箱体轴相同的方向 - 那是箱体本身
            if (boxAxis == axis) {
                continue;
            }

            // 为该方向创建旋转实例
            RotatingInstance instance = instancer.createInstance();

            // 设置实例属性：
            // - blockEntity: 关联的方块实体
            // - axis: 旋转轴方向
            // - speed: 根据动力源计算的旋转速度（考虑正/反转）
            instance.setup(blockEntity, axis, getSpeed(direction))
                    .setPosition(getVisualPosition())
                    // 旋转到目标方向
                    .rotateToFace(Direction.SOUTH, direction)
                    .setChanged();

            // 存储到Map中以便后续更新和删除
            keys.put(direction, instance);
        }
    }

    /**
     * 计算指定方向的旋转速度
     * <p>
     * 【计算逻辑】
     * 1. 获取方块实体的基本速度（来自动力网络）
     * 2. 如果有动力源，根据轴方向关系调整速度符号
     * 3. 速度为负数表示反向旋转
     * <p>
     * 【示例】
     * - 基本速度=64，动力源=NORTH，计算EAST方向：
     *   - getRotationSpeedModifier(EAST, NORTH) = 1（同向）
     *   - 最终速度=64 * 1 = 64
     * - 基本速度=64，动力源=NORTH，计算SOUTH方向：
     *   - getRotationSpeedModifier(SOUTH, NORTH) = -1（反向）
     *   - 最终速度=64 * -1 = -64
     *
     * @param direction 要计算的方向（传动轴方向）
     * @return 该方向的旋转速度（可为负数表示反转）
     */
    private float getSpeed(Direction direction) {
        // 获取方块实体的基本速度
        float speed = blockEntity.getSpeed();

        // 如果有动力源，应用旋转方向Modifier
        if (speed != 0 && sourceFacing != null) {
            // Modifier: 1=同向旋转, -1=反向旋转
            speed *= VersatileGearboxBlockEntity.getRotationSpeedModifier(direction, sourceFacing);
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
     * - 为每个旋转实例更新速度和状态
     *
     * @param partialTick 部分tick值（用于插值）
     */
    @Override
    public void update(float partialTick) {
        // 先更新动力源朝向
        updateSourceFacing();

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