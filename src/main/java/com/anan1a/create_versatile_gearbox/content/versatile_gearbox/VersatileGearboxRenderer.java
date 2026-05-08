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
 * 万能变速箱渲染器
 * <p>
 * 【核心概念】
 * - 方向轴（箱体轴）：方块的放置朝向，由 BlockState 的 AXIS 属性决定（Y/X/Z）
 *   - 决定箱体的外观朝向
 *   - 与方向轴平行的面没有传动轴接口
 * - 传动轴：连接动力的半轴，垂直于方向轴
 *   - 每个传动轴有独立的旋转方向和速度
 *   - 渲染时每个传动轴围绕自己的轴旋转
 * <p>
 * 【渲染逻辑】
 * 1. 获取方向轴（箱体轴）
 * 2. 遍历6个方向，跳过与方向轴平行的面
 * 3. 对剩余方向渲染半轴，每个半轴围绕自己的传动轴旋转
 * 4. 根据动力源方向调整旋转角度，确保视觉与实际动力传输一致
 * <p>
 * 【设计特点】
 * - 使用 Create API 简化渲染流程
 * - 保留对半轴方向的灵活控制，支持动态改变半轴配置
 * - 适配 Flywheel 可视化优化
 */
public class VersatileGearboxRenderer extends KineticBlockEntityRenderer<VersatileGearboxBlockEntity> {

    public VersatileGearboxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(VersatileGearboxBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        // Flywheel 优化检测 - 使用可视化管理器时跳过原版渲染
        // Flywheel 会使用 VersatileGearboxVisual 进行更高效的渲染
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        // 获取方向轴（箱体轴）- 由方块放置时的朝向决定
        // 例如：Y轴表示垂直放置，X/Z轴表示水平放置
        final Axis boxAxis = be.getBlockState().getValue(BlockStateProperties.AXIS);
        final BlockPos pos = be.getBlockPos();

        // 遍历所有6个方向（上下前后左右）
        for (Direction direction : Iterate.directions) {
            // 【关键判断】跳过与方向轴平行的方向
            // 原理：方向轴是箱体的朝向，与方向轴平行的面是箱体本身，没有传动轴接口
            // 例如：方向轴为Y时，顶面(UP)和底面(DOWN)与Y轴平行，不渲染半轴
            if (boxAxis == direction.getAxis())
                continue;

            // 检查是否需要渲染该方向的半轴（支持动态配置）
            // 可通过重写 shouldRenderShaftHalf() 实现半轴的动态启用/禁用
            if (!shouldRenderShaftHalf(be, direction))
                continue;

            // 渲染单个半轴
            renderShaftHalf(be, direction, ms, buffer, light);
        }
    }

    /**
     * 检查是否应该渲染指定方向的半轴
     * <p>
     * 【用途】支持动态配置半轴的显示/隐藏
     * - 默认返回 true，所有非方向轴的面都渲染半轴
     * - 可重写此方法实现：
     *   - 根据游戏逻辑动态启用/禁用半轴
     *   - 实现玩家自定义半轴配置
     *   - 根据红石信号控制半轴显示
     * 
     * @param be        方块实体
     * @param direction 要检查的方向（传动轴方向）
     * @return true 表示渲染该半轴，false 表示跳过
     */
    protected boolean shouldRenderShaftHalf(VersatileGearboxBlockEntity be, Direction direction) {
        // 默认：所有非方向轴方向都渲染半轴
        // 可扩展：根据 be 的动态配置决定是否渲染
        return true;
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
        
        // 3. 根据动力源方向调整旋转方向
        // 【核心逻辑】万能变速箱的动力传输特性：
        // - 与动力源相对的面反向旋转（速度倍率 -1）
        // - 其他面同向旋转（速度倍率 1）
        // 这样确保视觉上的旋转方向与实际动力传输一致
        if (be.getSpeed() != 0 && be.hasSource()) {
            // 计算动力源方向
            BlockPos source = be.source.subtract(pos);
            Direction sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ());
            
            // 获取旋转速度修正系数
            float modifier = VersatileGearboxBlockEntity.getRotationSpeedModifier(direction, sourceFacing);
            
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