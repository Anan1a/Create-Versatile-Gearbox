package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.List;
import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;
import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * 多功能齿轮箱模型
 *
 * <p>功能说明：
 * 根据六个面的轴状态（FWD/REV/OFF）动态渲染不同的纹理。
 * - FWD: 轴正向旋转 → 显示 fwd_core 纹理
 * - REV: 轴反向旋转 → 显示 rev_core 纹理
 * - OFF: 无轴或关闭 → 显示 off_shell 或 Andesite 机壳纹理
 *
 * <p>面状态顺序：[DOWN, UP, NORTH, SOUTH, WEST, EAST]
 */
@OnlyIn(Dist.CLIENT)
public class VersatileGearboxModel extends BakedModelWrapper<BakedModel> {

    /** 存储六个面轴状态的属性 */
    public static final ModelProperty<VersatileGearboxShaftState[]> FACE_STATES = new ModelProperty<>();

    private static final String TEXTURE_BASE = "block/versatile_gearbox/";

    // 纹理常量
    private static final ResourceLocation TEXTURE_ANDESITE_CASING = ResourceLocation.fromNamespaceAndPath("create", "block/andesite_casing");
    private static final ResourceLocation TEXTURE_FWD_CORE = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "fwd_core");
    private static final ResourceLocation TEXTURE_REV_CORE = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "rev_core");
    private static final ResourceLocation TEXTURE_OFF_SHELL = ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "off_shell");

    // 纹理条目定义
    // CORE_ENTRY: 旋转状态(FWD/REV)使用独立纹理
    private static final DynamicTextureModel.TextureEntry<VersatileGearboxShaftState> CORE_ENTRY = new DynamicTextureModel.TextureEntry<>(
            TEXTURE_FWD_CORE,
            Map.of(
                VersatileGearboxShaftState.FWD, TEXTURE_FWD_CORE,
                VersatileGearboxShaftState.REV, TEXTURE_REV_CORE
            )
    );

    // SHELL_ENTRY: 关闭状态使用外壳纹理
    private static final DynamicTextureModel.TextureEntry<VersatileGearboxShaftState> SHELL_ENTRY = new DynamicTextureModel.TextureEntry<>(
            TEXTURE_OFF_SHELL,
            Map.of(VersatileGearboxShaftState.OFF, TEXTURE_ANDESITE_CASING)
    );

    private static final List<DynamicTextureModel.TextureEntry<VersatileGearboxShaftState>> TEXTURE_ENTRIES = List.of(CORE_ENTRY, SHELL_ENTRY);

    /** 动态纹理模型实例 */
    private final DynamicTextureModel<VersatileGearboxShaftState> dynamicTextureModel;

    public VersatileGearboxModel(BakedModel template) {
        super(template);
        this.dynamicTextureModel = new DynamicTextureModel<>(
                template,
                TEXTURE_ENTRIES,
                VersatileGearboxModel::getStatesFromBlock,
                this::getStateForFace,
                this::getStatesFromModelData
        );
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        return dynamicTextureModel.getQuads(state, side, rand, extraData, renderType);
    }

    private VersatileGearboxShaftState[] getStatesFromModelData(ModelData data) {
        if (data != null && data.has(FACE_STATES)) {
            VersatileGearboxShaftState[] states = data.get(FACE_STATES);
            // 验证状态数组有效性：必须长度为6且无null
            if (states != null && states.length == 6) {
                for (VersatileGearboxShaftState s : states) {
                    if (s == null) return null;
                }
                return states;
            }
        }
        return null;
    }

    private VersatileGearboxShaftState getStateForFace(Direction face, VersatileGearboxShaftState[] states) {
        if (states == null || states.length != 6) {
            return VersatileGearboxShaftState.OFF;
        }
        int index = directionToIndex(face);
        return (index >= 0 && index < 6) ? states[index] : VersatileGearboxShaftState.OFF;
    }

    /**
     * 从BlockState获取六个面的轴状态
     */
    private static VersatileGearboxShaftState[] getStatesFromBlock(BlockState state) {
        if (!(state.getBlock() instanceof VersatileGearboxBlock block)) {
            return null;
        }
        return new VersatileGearboxShaftState[]{
                block.getShaftState(Direction.DOWN, state),
                block.getShaftState(Direction.UP, state),
                block.getShaftState(Direction.NORTH, state),
                block.getShaftState(Direction.SOUTH, state),
                block.getShaftState(Direction.WEST, state),
                block.getShaftState(Direction.EAST, state)
        };
    }

    /**
     * 方向到索引的映射
     * 索引顺序：DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5
     */
    private static int directionToIndex(Direction direction) {
        return switch (direction) {
            case DOWN -> 0;
            case UP -> 1;
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
        };
    }
}
