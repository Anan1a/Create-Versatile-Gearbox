package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;


/**
 * 高级齿轮箱渲染器（六面轴版本）
 * <p>
 * 所有六个面都有传动轴，支持全方位动力传输。
 * 每个半轴围绕自己的轴独立旋转，方向由动力源和翻转属性决定。
 */
public class AdvancedGearboxRenderer extends KineticBlockEntityRenderer<AdvancedGearboxBlockEntity> {

    /**
     * 构造函数
     *
     * @param context 渲染器提供者上下文
     */
    public AdvancedGearboxRenderer(BlockEntityRendererProvider.Context context) {
        // 调用父类构造函数，初始化 KineticBlockEntityRenderer 基础渲染功能
        // 父类提供了 kineticRotationTransform 等旋转动画工具方法
        super(context);
    }

    /**
     * 安全渲染高级齿轮箱的所有半轴
     * <p>
     * 【渲染策略】
     * - Flywheel 优化：如果启用了 Flywheel 可视化，则跳过此渲染器，由 Flywheel 接管
     * - 六面轴渲染：遍历所有 6 个方向，根据 NBT 数据判断是否渲染该方向的半轴
     * - 独立旋转：每个半轴根据其方向的动力传输状态独立计算旋转角度
     * <p>
     * 无轴状态组的半轴不渲染，有轴状态组的半轴正常渲染。
     *
     * @param be          方块实体，包含动力网络和状态信息
     * @param partialTicks 部分 tick 值，用于平滑动画插值
     * @param ms           姿态栈，包含位置、旋转等变换矩阵
     * @param buffer       多重缓冲区源，用于获取不同类型的渲染缓冲区
     * @param light        光照值，包含天空光照和方块光照
     * @param overlay      覆盖层纹理坐标（通常不使用）
     */
    @Override
    protected void renderSafe(AdvancedGearboxBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // Flywheel 优化：如果启用了 Flywheel 可视化，则跳过此渲染器
        // Flywheel 使用 GPU 实例化渲染，性能优于传统的 TileEntityRenderer
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        // 获取方块位置和基础速度
        BlockPos pos = be.getBlockPos();
        float baseSpeed = be.getSpeed();

        // 计算动力源方向（用于确定各面旋转方向）
        // 从 BE 的 source 字段（动力源位置）推算动力输入方向
        Direction sourceFacing = null;
        if (baseSpeed != 0 && be.hasSource() && be.source != null) {
            // 计算动力源与自身位置的偏移向量
            BlockPos sourceOffset = be.source.subtract(pos);
            // 从偏移向量获取最接近的方向（即动力源所在的面）
            sourceFacing = Direction.getNearest(sourceOffset.getX(), sourceOffset.getY(), sourceOffset.getZ());
        }

        // 遍历六个方向，渲染所有无轴状态组的半轴
        for (Direction direction : Iterate.directions) {
            // 跳过无轴状态组的半轴（不渲染）
            // 使用枚举的统一方法判断，便于扩展新状态（如 PARTIAL、LOCKED 等）
            if (!be.getShaftState(direction).hasShaft())
                continue;
            // 渲染该方向的半轴
            renderShaftHalf(be, pos, sourceFacing, baseSpeed, direction, ms, buffer, light);
        }
    }

    /**
     * 渲染单个半轴
     * <p>
     * 【渲染流程】
     * 1. 获取半轴模型 → 2. 计算旋转角度 → 3. 应用变换并渲染
     * <p>
     * 【性能优化】参数预计算传递，避免每帧重复计算
     *
     * @param be          方块实体
     * @param pos         方块位置
     * @param sourceFacing 动力源方向
     * @param baseSpeed   基础速度
     * @param direction   半轴方向
     * @param ms          姿态栈
     * @param buffer      渲染缓冲区
     * @param light       光照值
     */
    protected void renderShaftHalf(AdvancedGearboxBlockEntity be, BlockPos pos,
                                   Direction sourceFacing, float baseSpeed, Direction direction,
                                   PoseStack ms, MultiBufferSource buffer, int light) {
        final Axis shaftAxis = direction.getAxis();

        // 获取缓存的半轴模型（Create API）
        // 传入 be.getBlockState() 仅用于模型注册表查找，不涉及面状态属性
        // partialFacing 会根据 direction 自动旋转模型到正确朝向
        SuperByteBuffer shaftBuffer = CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);

        // 计算该方向的实际速度（考虑传动比）
        // 使用 BE 的统一方法，与 Visual 保持一致
        float speedForDirection = be.getSpeedForDirection(baseSpeed, direction, sourceFacing);

        // 使用修正后的速度计算旋转角度
        // 负速度会产生负角度，实现反向旋转
        float angle = getAngleForDirection(be, pos, shaftAxis, speedForDirection);

        // 应用旋转并渲染（Create API）
        // kineticRotationTransform 会应用旋转矩阵、光照和位置变换
        kineticRotationTransform(shaftBuffer, be, shaftAxis, angle, light);

        // 将半轴模型渲染到屏幕
        // renderInto: 将已变换的模型缓冲区内容绘制到渲染目标
        // - ms (PoseStack): 包含位置、旋转、缩放等变换信息的矩阵栈
        // - buffer.getBuffer(RenderType.solid()): 获取实体渲染类型的顶点缓冲区
        //   RenderType.solid() 用于不透明的固体方块渲染，支持光照和纹理
        shaftBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    /**
     * 根据指定速度计算旋转角度
     * <p>
     * 【核心修复】直接使用速度值计算角度，确保半轴与传动轴同步。
     *
     * @param be    方块实体
     * @param pos   方块位置
     * @param axis  轴向
     * @param speed 该方向的实际速度
     * @return 旋转角度（弧度制）
     */
    protected float getAngleForDirection(AdvancedGearboxBlockEntity be, BlockPos pos, Axis axis, float speed) {
        // 获取当前渲染时间（用于动画同步）
        // AnimationTickHolder 提供与游戏 tick 同步的时间戳
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        
        // 获取该位置的旋转偏移量（用于多方块同步旋转）
        // 确保相邻方块的轴旋转相位一致
        float offset = getRotationOffsetForPosition(be, pos, axis);
        
        // 计算旋转角度（度数制）
        // 公式：angle = (时间 * 速度 * 传动系数 + 偏移量) % 360
        // - time * speed * 3f / 10: 时间积分得到角度，3/10 是 Create 的传动系数
        // - 负速度会产生负角度，实现反向旋转
        // - % 360 确保角度在 0-359 度范围内
        float angle = (time * speed * 3f / 10 + offset) % 360;
        
        // 转换为弧度制（OpenGL 使用弧度）
        // 公式：弧度 = 度数 * π / 180
        return angle / 180 * (float) Math.PI;
    }

}