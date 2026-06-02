package com.anan1a.create_versatile_gearbox.mixin.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anan1a.create_versatile_gearbox.content.advanced_gearbox.AdvancedGearboxBlockEntity;
import com.anan1a.create_versatile_gearbox.content.advanced_gearbox.AdvancedGearboxModel;
import com.anan1a.create_versatile_gearbox.content.advanced_gearbox.AdvancedGearboxShaftState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 类：修复高级齿轮箱（AdvancedGearbox）在 SectionCompiler 阶段的渲染数据传递问题。
 * <p>
 * <b>问题根源</b>：<br>
 * Minecraft 1.21 的区块编译（SectionCompiler）在 {@code rend_chk_rebuild} 线程上重建区块时，
 * 对每个方块调用 {@code BlockRenderDispatcher.renderBatched()} → {@code tesselateBlock()} → 
 * {@link BakedModel#getQuads} 获取烘焙 quad。理论上，{@link AdvancedGearboxBlockEntity#getModelData()}
 * 应该在 {@code renderBatched()} 之前被调用以提供 ModelData，但在 Create 的渲染管线中，
 * 该方法在此阶段不被正确分派（可能与 Create 的 ModelSwapper 或其他 Mixin 冲突），
 * 导致 {@link AdvancedGearboxModel#resolveState} 无法通过 ModelData 获取面状态数据。
 * <p>
 * <b>解决方案</b>：<br>
 * 在 {@code renderBatched()} 的 HEAD 处注入，直接从 BlockEntity 获取面状态数组
 * 并写入 {@link AdvancedGearboxModel#CURRENT_FACE_STATES}（ThreadLocal），
 * 供模型在 {@code getQuads()} 中读取。
 * <p>
 * <b>关键设计</b>：<br>
 * 此 Mixin <em>不修改</em>传递给 {@code tesselateBlock()} 的原始 ModelData，
 * 避免丢失连接纹理（casingConnectivity）等其他系统写入 ModelData 的数据。
 * 面状态数据通过 ThreadLocal 直接桥接，由模型自身的 {@code resolveState()} 读取。
 * <p>
 * 数据流：
 * <pre>
 *   BlockEntity (NBT) 
 *       → Mixin @Inject → be.getFaceStatesArray()
 *       → CURRENT_FACE_STATES (ThreadLocal)
 *       → resolveState() 
 *       → DynamicTextureModel → getQuads() → BakedQuad
 * </pre>
 *
 * @see AdvancedGearboxModel#CURRENT_FACE_STATES
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("CVG_MIXIN");

    /**
     * 在 {@link BlockRenderDispatcher#renderBatched} 方法执行之初捕获渲染上下文。
     * <p>
     * 从 BlockEntity 直接获取面状态数组，写入
     * {@link AdvancedGearboxModel#CURRENT_FACE_STATES}（ThreadLocal），
     * 供 {@code AdvancedGearboxModel.resolveState()} 在 {@code getQuads()} 中读取。
     * 无需经过 ModelData，避免替换原始数据导致连接纹理丢失。
     * <p>
     * 由于 {@code renderBatched → tesselateBlock → getQuads} 是同步串行的，
     * ThreadLocal 值在同一个方块的多次 {@code resolveState} 调用（每个面一次）之间不会被覆盖。
     * 非 AdvancedGearbox 方块则清除 ThreadLocal，防止 resolveState 读到残留数据。
     *
     * @param state          当前渲染方块的 BlockState
     * @param pos            当前渲染方块的世界坐标
     * @param level          世界访问器（用于查询 BlockEntity）
     * @param poseStack      渲染姿势栈
     * @param vertexConsumer 顶点缓冲区消费者
     * @param checkSides     Minecraft 内部参数（是否检查侧面可见性）
     * @param random         随机数源（用于破坏粒子等效果）
     * @param modelData      原始 ModelData（此 Mixin 不修改它，保持透传）
     * @param renderType     当前渲染类型（如 solid、cutout）
     * @param ci             Mixin 回调信息（未使用）
     */
    @Inject(
        method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("HEAD")
    )
    private void cvg$captureContext(BlockState state, BlockPos pos, BlockAndTintGetter level,
                                    PoseStack poseStack, VertexConsumer vertexConsumer, boolean checkSides,
                                    RandomSource random, ModelData modelData, RenderType renderType,
                                    CallbackInfo ci) {
        // 查询当前位置的 BlockEntity 并检查是否为高级齿轮箱
        // level.getBlockEntity(pos) 在 rend_chk_rebuild 线程调用是安全的，
        // 因为 SectionCompiler 在重建期间持有该区块的读锁。
        if (level.getBlockEntity(pos) instanceof AdvancedGearboxBlockEntity agbe) {
            // 从 BE 获取六个面的轴状态数组
            // getFaceStatesArray() 返回 clone，修改不影响 BE 内部状态
            AdvancedGearboxShaftState[] arr = agbe.getFaceStatesArray();
            // 注入到 ThreadLocal，供 resolveState 在后续的 tesselateBlock 调用中读取
            // 数据流：BE (NBT) → 此 ThreadLocal → resolveState → DynamicTextureModel
            AdvancedGearboxModel.CURRENT_FACE_STATES.set(arr);
        } else {
            // 非高级齿轮箱方块：清除 ThreadLocal
            // 防止前一个 AdvancedGearbox 的渲染数据被当前非 AG 方块的 resolveState 读取，
            // 导致纹理映射错误地使用旧的面状态（此情况虽概率低，但考虑防御性编程）
            AdvancedGearboxModel.CURRENT_FACE_STATES.remove();
        }
    }
}
