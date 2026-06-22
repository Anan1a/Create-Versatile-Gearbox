package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.List;
import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.DynamicTextureModel;
import static com.anan1a.create_versatile_gearbox.CreateVersatileGearbox.MODID;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 万向齿轮箱方块的定制烘焙模型，根据每面轴状态动态切换纹理。
 * <p>
 * 状态来源：直接从 {@link BlockState} 属性读取（无需 ModelData），
 * 因此 {@link #gatherModelData} 使用父类的空实现即可。
 */
@OnlyIn(Dist.CLIENT)
public class VersatileGearboxModel extends DynamicTextureModel<VersatileGearboxShaftState> {

    /** 万向齿轮箱方块纹理的基础路径。 */
    private static final String TEXTURE_BASE = "block/versatile_gearbox/";
    /** 关闭外壳替换使用的纹理（来自 Create 的安山岩机壳）。 */
    private static final ResourceLocation TEXTURE_ANDESITE_CASING =
            ResourceLocation.fromNamespaceAndPath("create", "block/andesite_casing");
    /** 正向核心占位纹理。 */
    private static final ResourceLocation TEXTURE_FWD_CORE =
            ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "fwd_core");
    /** 反向核心目标纹理。 */
    private static final ResourceLocation TEXTURE_REV_CORE =
            ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "rev_core");
    /** 有轴外壳占位纹理（OFF 时替换为安山岩机壳）。 */
    private static final ResourceLocation TEXTURE_OFF_SHELL =
            ResourceLocation.fromNamespaceAndPath(MODID, TEXTURE_BASE + "off_shell");

    /** 注册两个动态纹理条目：核心（FWD/REV 切换）和外壳（OFF 时隐藏轴）。 */
    private static final List<TextureEntry<VersatileGearboxShaftState>> TEXTURE_ENTRIES = List.of(
            new TextureEntry<>(
                    TEXTURE_FWD_CORE,
                    Map.of(
                            VersatileGearboxShaftState.FWD, TEXTURE_FWD_CORE,
                            VersatileGearboxShaftState.REV, TEXTURE_REV_CORE
                    )
            ),
            new TextureEntry<>(
                    TEXTURE_OFF_SHELL,
                    Map.of(VersatileGearboxShaftState.OFF, TEXTURE_ANDESITE_CASING)
            )
    );

    public VersatileGearboxModel(BakedModel template) {
        super(template, TEXTURE_ENTRIES);
    }

    /**
     * 直接从 BlockState 属性读取指定面的轴状态，无需读取 ModelData。
     */
    @Override
    protected VersatileGearboxShaftState resolveState(BlockState state, Direction face, ModelData data) {
        if (!(state.getBlock() instanceof VersatileGearboxBlock)) {
            return VersatileGearboxShaftState.OFF;
        }
        return VersatileGearboxBlock.getShaftState(face, state);
    }
}
