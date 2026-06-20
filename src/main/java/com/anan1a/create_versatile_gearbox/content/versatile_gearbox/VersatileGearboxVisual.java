package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

/**
 * 万能变速箱可视化类（六面轴版本）
 * <p>
 * 使用 Flywheel 渲染引擎为每个非 OFF 面创建半轴旋转实例（{@link RotatingInstance}），
 * 六个面均可独立旋转。
 * <p>
 * <b>实例生命周期</b>（一次性分配）：<br>
 * 构造函数中根据当前面状态创建对应实例，之后 <em>数量固定</em>。
 * OFF 面不创建实例，非 OFF 面创建一个 {@link RotatingInstance}。
 * 后续状态切换不增删实例，因为纹理通过 BlockState 独立控制。
 */
public class VersatileGearboxVisual extends KineticBlockEntityVisual<VersatileGearboxBlockEntity> {

    /**
     * 存储每个方向的旋转实例（数组形式，优化访问速度）。
     * <p>
     * 索引 0-5 对应 DOWN, UP, NORTH, SOUTH, WEST, EAST
     * （与 {@link Direction#values()} 顺序一致）。
     * 未使用的槽位为 null（该面 OFF）。
     */
    protected final RotatingInstance[] keys = new RotatingInstance[6];

    /**
     * 构造可视化实例。
     * <p>
     * 执行一次性的半轴实例分配：遍历六个面，仅为非 OFF 面创建旋转实例。
     * 不创建后续增删机制，因为纹理通过 BlockState 独立控制。
     *
     * @param context     可视化上下文（Flywheel 框架提供）
     * @param blockEntity 方块实体，包含面状态和动力数据
     * @param partialTick 部分 tick 值（用于插值）
     */
    public VersatileGearboxVisual(VisualizationContext context, VersatileGearboxBlockEntity blockEntity, float partialTick) {
        // 调用父类构造函数，初始化基础 KineticBlockEntityVisual 功能
        super(context, blockEntity, partialTick);

        // 一次性分配半轴实例：遍历六面，仅为非 OFF 面创建 RotatingInstance
        // 实例数量在此时固定，后续不增删
        initShaftInstances();
    }

    /**
     * 初始化半轴实例。
     * <p>
     * 遍历六个面，为非 OFF 面创建 {@link RotatingInstance}。
     * OFF 面跳过（keys[i] 保持 null），后续无论状态如何变化都不增删实例。
     * 旋转方向和速度通过 {@link #getSpeed(Direction)} 计算。
     */
    private void initShaftInstances() {
        // 获取旋转实例创建器：指定 ROTATING 类型和半轴模型（SHAFT_HALF）
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));
        
        // 遍历六个方向（DOWN, UP, NORTH, SOUTH, WEST, EAST）
        for (Direction direction : Iterate.directions) {
            // 跳过 OFF 面：不创建实例，此时显示机壳纹理
            // 使用枚举的统一方法判断，便于扩展新状态
            if (!VersatileGearboxBlock.getShaftState(direction, blockEntity.getBlockState()).hasShaft())
                continue;

            // 创建旋转实例并配置
            RotatingInstance instance = instancer.createInstance();
            instance.setup(blockEntity, direction.getAxis(), getSpeed(direction))   // 设置旋转轴和速度
                    .setPosition(getVisualPosition())                               // 设置世界位置
                    .rotateToFace(Direction.SOUTH, direction)                       // 旋转到目标方向（以 SOUTH 为基准）
                    .setChanged();                                                  // 标记实例状态已变更
            
            // 存入数组，索引对应 Direction.ordinal()
            keys[direction.ordinal()] = instance;
        }
    }

    /**
     * 计算指定方向的旋转速度。
     * <p>
     * 使用 BE 的统一方法，与 Renderer 保持一致。
     *
     * @param direction 要计算的方向
     * @return 该方向的旋转速度（负数表示反向旋转）
     */
    private float getSpeed(Direction direction) {
        return blockEntity.getVisualSpeed(direction);
    }

    /**
     * 每帧更新可视化状态。
     * <p>
     * 更新内容：
     * <ol>
     *   <li>重新计算动力源朝向（因为连接状态可能改变）</li>
     *   <li>为每个非 null 的旋转实例更新速度和状态</li>
     * </ol>
     * 不增删实例（实例数量在构造函数中固定）。
     *
     * @param partialTick 部分 tick 值（用于插值）
     */
    @Override
    public void update(float partialTick) {
        // 遍历六个方向，更新所有已存在的旋转实例
        for (int i = 0; i < 6; i++) {
            RotatingInstance instance = keys[i];
            // 跳过 OFF 面（未创建实例，keys[i] 为 null）
            if (instance != null) {
                Direction direction = Direction.values()[i];
                // 更新实例的旋转轴和速度，标记状态变更
                instance.setup(blockEntity, direction.getAxis(), getSpeed(direction)).setChanged();
            }
        }
        // 注意：此方法不增删实例，实例数量在构造函数中固定
    }

    /**
     * 更新光照。
     * <p>
     * 将光照信息传递给所有旋转实例。
     *
     * @param partialTick 部分 tick 值（用于插值）
     */
    @Override
    public void updateLight(float partialTick) {
        // 遍历所有旋转实例，更新光照信息
        for (RotatingInstance instance : keys) {
            // 跳过未创建的实例（OFF 面）
            if (instance != null) {
                // 将方块位置的光照数据传递给实例
                relight(instance);
            }
        }
    }

    /**
     * 清理资源。
     * <p>
     * 删除所有旋转实例，当可视化被移除时调用。
     */
    @Override
    protected void _delete() {
        // 遍历所有旋转实例，释放资源
        for (RotatingInstance instance : keys) {
            // 跳过未创建的实例（OFF 面）
            if (instance != null) {
                // 删除实例，释放 GPU 资源
                instance.delete();
            }
        }
    }

    /**
     * 收集用于 crumbling 动画的实例。
     * <p>
     * Crumbling 是方块被破坏时的动画效果，
     * 返回所有存在的旋转实例参与该动画。
     *
     * @param consumer 实例消费者
     */
    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        // 遍历所有旋转实例，收集参与 crumbling 动画的实例
        // Crumbling 是方块被破坏时的破碎动画效果
        for (RotatingInstance instance : keys) {
            // 跳过未创建的实例（OFF 面）
            if (instance != null) {
                // 将实例传递给消费者，用于渲染破碎动画
                consumer.accept(instance);
            }
        }
    }
}