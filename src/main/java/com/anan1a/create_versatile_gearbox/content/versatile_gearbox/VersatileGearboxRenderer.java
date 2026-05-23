package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 万能变速箱渲染器（六面轴版本）
 * <p>
 * 所有六个面都有传动轴，支持全方位动力传输。
 * 每个半轴围绕自己的轴独立旋转，方向由动力源和翻转属性决定。
 */
public class VersatileGearboxRenderer extends KineticBlockEntityRenderer<VersatileGearboxBlockEntity> {

    /**
     * 构造函数
     *
     * @param context 渲染器提供者上下文
     */
    public VersatileGearboxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * 安全渲染万能变速箱的所有半轴
     * <p>
     * 【渲染策略】
     * - Flywheel优化：如果启用了Flywheel可视化，则跳过此渲染器，由Flywheel接管
     * - 六面轴渲染：遍历所有6个方向，根据BlockState判断是否渲染该方向的半轴
     * - 独立旋转：每个半轴根据其方向的动力传输状态独立计算旋转角度
     * <p>
     * 【关键修复】
     * - 优先从BlockState读取状态，确保与Ponder场景中的modifyBlock同步
     * - OFF状态的半轴不渲染，FWD/REV状态的半轴正常渲染
     *
     * @param be          方块实体，包含动力网络和状态信息
     * @param partialTicks 部分tick值，用于平滑动画插值
     * @param ms           姿态栈，包含位置、旋转等变换矩阵
     * @param buffer       多重缓冲区源，用于获取不同类型的渲染缓冲区
     * @param light        光照值，包含天空光照和方块光照
     * @param overlay      覆盖层纹理坐标（通常不使用）
     */
    @Override
    protected void renderSafe(VersatileGearboxBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // Flywheel 优化检测 - 使用可视化管理器时跳过原版渲染
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        // 遍历所有6个方向（六面轴版本：所有面都有传动轴）
        for (Direction direction : Iterate.directions) {
            // 检查是否应该渲染该方向的半轴（OFF状态不渲染）
            if (!shouldRenderShaftHalf(be, direction))
                continue;

            // 渲染单个半轴
            renderShaftHalf(be, direction, ms, buffer, light);
        }
    }

    /**
     * 检查是否应该渲染指定方向的半轴
     * <p>
     * 【三状态版本】OFF 状态不渲染半轴，FWD/REV 状态渲染半轴。
     * <p>
     * 【关键修复】优先从 BlockState 读取状态，确保与 Ponder 场景中的 modifyBlock 同步。
     *
     * @param be        方块实体
     * @param direction 要检查的方向
     * @return true 表示渲染该半轴
     */
    protected boolean shouldRenderShaftHalf(VersatileGearboxBlockEntity be, Direction direction) {
        // 直接从 BlockState 读取最新状态（而非缓存的 BlockEntity 数据）
        ShaftState state = VersatileGearboxBlock.getShaftState(direction, be.getBlockState());
        return state != ShaftState.OFF;
    }

    /**
     * 渲染单个半轴
     * <p>
     * 【渲染流程】
     * 1. 获取半轴模型 → 2. 计算旋转角度 → 3. 应用变换并渲染
     * 
     * @param be        方块实体
     * @param direction 半轴方向（同时也是传动轴的方向）
     * @param ms        变换栈
     * @param buffer    渲染缓冲区
     * @param light     光照值
     */
    protected void renderShaftHalf(VersatileGearboxBlockEntity be, Direction direction, 
                                   PoseStack ms, MultiBufferSource buffer, int light) {
        // 获取传动轴的轴方向
        final Axis shaftAxis = direction.getAxis();
        final BlockPos pos = be.getBlockPos();

        // 1. 获取缓存的半轴模型（Create API）
        SuperByteBuffer shaftBuffer = CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);

        // 2. 计算该方向的实际速度（考虑翻转属性）
        float speedForDirection = be.getSpeed();
        if (speedForDirection != 0 && be.hasSource()) {
            try {
                // 计算动力源方向
                BlockPos source = be.source.subtract(pos);
                Direction sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ());
                
                // 获取旋转速度修正系数（包含翻转属性）
                float modifier = VersatileGearboxBlockEntity.getRotationSpeedModifier(direction, sourceFacing, be.getBlockState());
                
                // 应用修正到速度上
                speedForDirection *= modifier;
            } catch (Exception e) {
                // 防止计算异常导致渲染崩溃
                com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.LOGGER.error(
                    "Error calculating renderer speed for direction {}: {}", direction, e.getMessage());
                speedForDirection = 0;
            }
        }
        
        // 3. 使用修正后的速度计算旋转角度
        float angle = getAngleForDirection(be, pos, shaftAxis, speedForDirection);

        // 4. 应用旋转并渲染（Create API）
        kineticRotationTransform(shaftBuffer, be, shaftAxis, angle, light);

        // 5. 将半轴模型渲染到屏幕
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
    protected float getAngleForDirection(VersatileGearboxBlockEntity be, BlockPos pos, Axis axis, float speed) {
        float time = net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(be.getLevel());
        float offset = getRotationOffsetForPosition(be, pos, axis);
        
        // 直接使用速度计算角度（允许负数角度表示反向旋转）
        float angle = (time * speed * 3f / 10 + offset) % 360;
        
        // 转换为弧度制
        return angle / 180 * (float) Math.PI;
    }

}