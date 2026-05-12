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

    public VersatileGearboxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(VersatileGearboxBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // Flywheel 优化检测 - 使用可视化管理器时跳过原版渲染
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        final BlockPos pos = be.getBlockPos();

        // 遍历所有6个方向（六面轴版本：所有面都有传动轴）
        for (Direction direction : Iterate.directions) {
            if (!shouldRenderShaftHalf(be, direction))
                continue;

            // 渲染单个半轴
            renderShaftHalf(be, direction, ms, buffer, light);
        }
    }

    /**
     * 检查是否应该渲染指定方向的半轴
     * <p>
     * 三状态版本：OFF状态不渲染半轴。
     *
     * @param be        方块实体
     * @param direction 要检查的方向
     * @return true 表示渲染该半轴
     */
    protected boolean shouldRenderShaftHalf(VersatileGearboxBlockEntity be, Direction direction) {
        ShaftState state = VersatileGearboxBlock.getShaftState(direction, be.getBlockState());
        return state != ShaftState.OFF;
    }

    /**
     * 渲染单个半轴
     * <p>
     * 【渲染流程】
     * 1. 获取半轴模型 → 2. 计算旋转角度 → 3. 调整旋转方向 → 4. 应用变换并渲染
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
        // 注意：direction 是半轴伸出的方向，而 axis 是半轴旋转的轴
        // 例如：direction=EAST 时，axis=X（半轴围绕X轴旋转）
        final Axis shaftAxis = direction.getAxis();
        final BlockPos pos = be.getBlockPos();

        // 1. 获取缓存的半轴模型（Create API）
        // 使用 partialFacing 获取指定朝向的部分模型
        SuperByteBuffer shaftBuffer = CachedBuffers.partialFacing(
                AllPartialModels.SHAFT_HALF, be.getBlockState(), direction);

        // 2. 计算旋转角度（使用 Create API）
        // getAngleForBe() 自动处理：渲染时间、方块速度、位置偏移
        float baseAngle = getAngleForBe(be, pos, shaftAxis);
        
        // 3. 根据动力源方向和翻转属性调整旋转方向
            // 【核心逻辑】万能变速箱的动力传输特性：
            // - 与动力源相对的面反向旋转（速度倍率 -1）
            // - 其他面同向旋转（速度倍率 1）
            // 这样确保视觉上的旋转方向与实际动力传输一致
            if (be.getSpeed() != 0 && be.hasSource()) {
                // 计算动力源方向
                BlockPos source = be.source.subtract(pos);
                Direction sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ());
                
                // 获取旋转速度修正系数（包含翻转属性）
                float modifier = VersatileGearboxBlockEntity.getRotationSpeedModifier(direction, sourceFacing, be.getBlockState());
                
                // 应用修正
                baseAngle = modifyAngleForSource(baseAngle, modifier);
            }

        // 4. 应用旋转并渲染（Create API）
        // kineticRotationTransform() 处理：光照、旋转、颜色效果
        kineticRotationTransform(shaftBuffer, be, shaftAxis, baseAngle, light);
        shaftBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    /**
     * 根据旋转修正系数调整角度
     * <p>
     * 【原理】
     * - Minecraft 渲染使用弧度制，而速度修正使用角度制的逻辑
     * - 需要先将弧度转换为角度，应用修正，再转换回弧度
     * 
     * @param angle    当前旋转角度（弧度制）
     * @param modifier 修正系数（1 表示同向，-1 表示反向）
     * @return 修正后的角度（弧度制）
     */
    protected float modifyAngleForSource(float angle, float modifier) {
        // 将弧度转换为角度制
        float degrees = angle * 180f / (float) Math.PI;
        // 应用修正系数（反转或保持方向）
        degrees *= modifier;
        // 转换回弧度制
        return degrees / 180f * (float) Math.PI;
    }
}