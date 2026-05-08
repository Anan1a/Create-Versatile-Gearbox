package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 万能变速箱渲染器
 * <p>
 * 负责渲染变速箱内部的旋转轴：
 * - 根据方块朝向（AXIS）确定箱体轴方向
 * - 为非箱体轴方向的每个面渲染半轴（SHAFT_HALF）
 * - 根据动力源方向调整渲染角度，实现正确的旋转方向视觉
 * <p>
 * 渲染逻辑：
 * - 遍历所有6个方向（上下前后左右）
 * - 跳过与箱体轴相同的方向（那部分是箱体本身）
 * - 对每个有效方向，渲染一个半轴并应用旋转动画
 */
public class VersatileGearboxRenderer extends KineticBlockEntityRenderer<VersatileGearboxBlockEntity> {

    public VersatileGearboxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(VersatileGearboxBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // 如果正在使用可视化管理器（Flywheel），则跳过原版渲染
        // Flywheel会使用VersatileGearboxVisual进行渲染
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        // 获取箱体的轴方向（由方块状态决定：X=水平X轴, Y=垂直轴, Z=水平Z轴）
        final Axis boxAxis = be.getBlockState().getValue(BlockStateProperties.AXIS);
        final BlockPos pos = be.getBlockPos();

        // 获取渲染时间，用于计算旋转角度
        float time = AnimationTickHolder.getRenderTime(be.getLevel());

        // 遍历所有6个方向
        for (Direction direction : Iterate.directions) {
            final Axis axis = direction.getAxis();

            // 跳过与箱体轴相同方向 - 那是箱体本身，不需要渲染半轴
            if (boxAxis == axis)
                continue;

            // 创建半轴缓冲区
            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);

            // 计算旋转偏移量，基于位置和轴
            float offset = getRotationOffsetForPosition(be, pos, axis);

            // 计算当前旋转角度：时间 * 速度 * 系数 / 10，然后取模360
            float angle = (time * be.getSpeed() * 3f / 10) % 360;

            // 如果有动力源，根据动力源方向调整旋转方向
            // 这是关键：使渲染的旋转方向与实际的旋转Modifier一致
            if (be.getSpeed() != 0 && be.hasSource()) {
                // 计算动力源面的方向
                BlockPos source = be.source.subtract(be.getBlockPos());
                Direction sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ());

                // 根据动力源方向和当前面的关系，添加符号（+1 或 -1）
                // 这样可以渲染正确的旋转方向（正转或反转）
                angle *= VersatileGearboxBlockEntity.getRotationSpeedModifier(direction, sourceFacing);
            }

            // 添加偏移量并转换为弧度
            angle += offset;
            angle = angle / 180f * (float) Math.PI;

            // 应用旋转变换并渲染
            kineticRotationTransform(shaft, be, axis, angle, light);
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }
}