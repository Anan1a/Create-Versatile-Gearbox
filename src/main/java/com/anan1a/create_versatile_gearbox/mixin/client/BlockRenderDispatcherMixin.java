/**
 * Mixin 类：修复高级齿轮箱的 ModelData 传递问题。
 * <p>
 * 问题背景：Minecraft 的渲染管线中，{@link AdvancedGearboxModel#getModelData()} 方法在
 * SectionCompiler 阶段不被正确分派（原因不明，可能与 Create 的 ModelSwapper 或其他 Mixin 有关）。
 * 导致模型无法从 BlockEntity 获取面状态数据。
 * <p>
 * 解决方案：在 {@link BlockRenderDispatcher#renderBatched()} 方法中，
 * 通过 {@code @Inject} 捕获上下文（BlockPos 和 Level），然后通过 {@code @ModifyArg}
 * 在调用 {@link net.minecraft.client.renderer.block.ModelBlockRenderer#tesselateBlock()} 之前，
 * 从缓存或 BlockEntity 读取面状态并注入到 ModelData 中。
 */
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {

    /** Mixin 专用日志记录器，用于调试注入行为 */
    private static final Logger LOGGER = LoggerFactory.getLogger("CVG_MIXIN");

    /** 当前渲染方块的位置（ThreadLocal，避免并发问题） */
    private static final ThreadLocal<BlockPos> CTX_POS = new ThreadLocal<>();
    /** 当前渲染上下文的 Level 访问器（ThreadLocal） */
    private static final ThreadLocal<BlockAndTintGetter> CTX_LEVEL = new ThreadLocal<>();

    /**
     * 在 {@code renderBatched()} 方法开始时捕获渲染上下文。
     * <p>
     * 将 BlockPos 和 Level 存储到 ThreadLocal 中，供后续的 {@code cvg$injectFaceStates()} 使用。
     *
     * @param state          当前渲染的方块状态
     * @param pos            当前渲染的方块位置
     * @param level          Level 访问器
     * @param poseStack      渲染姿势栈
     * @param vertexConsumer 顶点消费者
     * @param checkSides     是否检查侧面
     * @param random         随机数源
     * @param modelData      原始 ModelData
     * @param renderType     渲染类型
     * @param ci             Mixin 回调信息
     */
    @Inject(
        method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At("HEAD")
    )
    private void cvg$captureContext(BlockState state, BlockPos pos, BlockAndTintGetter level,
                                    PoseStack poseStack, VertexConsumer vertexConsumer, boolean checkSides,
                                    RandomSource random, ModelData modelData, RenderType renderType,
                                    CallbackInfo ci) {
        CTX_POS.set(pos);
        CTX_LEVEL.set(level);
    }

    /**
     * 在调用 {@code tesselateBlock()} 之前注入面状态数据到 ModelData。
     * <p>
     * 注入优先级：
     * <ol>
     *   <li>如果原始 ModelData 已有 FACE_STATES，直接返回（避免重复注入）</li>
     *   <li>从静态缓存 {@link AdvancedGearboxModel#FACE_STATES_CACHE} 读取（优先）</li>
     *   <li>从 BlockEntity 读取并更新缓存（备用）</li>
     *   <li>返回原始数据（非高级齿轮箱方块）</li>
     * </ol>
     *
     * @param originalData 原始的 ModelData
     * @return 注入了 FACE_STATES 的新 ModelData，或原始数据（如果不是高级齿轮箱）
     */
    @ModifyArg(
        method = "renderBatched(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"
        ),
        index = 10  // tesselateBlock 的第 11 个参数（index 从 0 开始）是 ModelData
    )
    private ModelData cvg$injectFaceStates(ModelData originalData) {
        // 1. 检查原始数据是否已有 FACE_STATES（避免重复注入）
        if (originalData != null && originalData.has(AdvancedGearboxModel.FACE_STATES)) {
            return originalData;
        }

        // 获取之前捕获的上下文
        BlockPos pos = CTX_POS.get();
        BlockAndTintGetter level = CTX_LEVEL.get();
        if (pos == null || level == null) return originalData;

        // 2. 优先从缓存读取（性能更好，避免频繁访问 BE）
        AdvancedGearboxShaftState[] cached = AdvancedGearboxModel.FACE_STATES_CACHE.get(pos);
        if (cached != null && cached.length == 6) {
            LOGGER.warn("[CVG_MIXIN] Injected FACE_STATES from cache at {}, states={}",
                    pos, java.util.Arrays.toString(cached));
            return ModelData.builder()
                    .with(AdvancedGearboxModel.FACE_STATES, cached)
                    .build();
        }

        // 3. 从 BlockEntity 读取并更新缓存
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AdvancedGearboxBlockEntity agbe) {
            AdvancedGearboxShaftState[] arr = agbe.getFaceStatesSnapshot();
            AdvancedGearboxModel.updateCache(pos, arr);
            LOGGER.warn("[CVG_MIXIN] Injected FACE_STATES from BE at {}, states={}",
                    pos, java.util.Arrays.toString(arr));
            return ModelData.builder()
                    .with(AdvancedGearboxModel.FACE_STATES, arr)
                    .build();
        }

        // 4. 不是高级齿轮箱，返回原始数据
        return originalData;
    }
}
