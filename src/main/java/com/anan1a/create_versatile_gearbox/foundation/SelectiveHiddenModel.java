package com.anan1a.create_versatile_gearbox.foundation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 选择性隐藏模型部件
 * 
 * <p>功能说明：
 * 根据模型元素的name属性过滤四边形，实现动态隐藏特定部件。
 * 参考Create的CopycatModelCore实现方式。
 * 
 * <p>使用方式：
 * <pre>{@code
 * Set<String> hiddenNames = Set.of("Casing", "Side");
 * BakedModel model = new SelectiveHiddenModel(templateModel, hiddenNames);
 * }</pre>
 * 
 * <p>工作原理：
 * 1. 在模型加载时，每个BakedQuad会携带其来源元素的名称信息
 * 2. 通过BakedQuadHelper获取quad的名称属性
 * 3. 根据配置的隐藏规则过滤掉指定名称的quad
 * 
 * @see com.simibubi.create.foundation.model.BakedQuadHelper
 */
@OnlyIn(Dist.CLIENT)
public class SelectiveHiddenModel extends BakedModelWrapper<BakedModel> {

    /** 需要隐藏的部件名称集合 */
    private final Set<String> hiddenNames;

    /** 过滤条件：返回true表示保留，false表示隐藏 */
    private final Predicate<String> visibilityPredicate;

    /**
     * 构造函数：根据名称集合隐藏部件
     * 
     * @param template 原始烘焙模型
     * @param hiddenNames 需要隐藏的部件名称集合
     */
    public SelectiveHiddenModel(BakedModel template, Set<String> hiddenNames) {
        super(template);
        this.hiddenNames = new HashSet<>(hiddenNames);
        this.visibilityPredicate = name -> !this.hiddenNames.contains(name);
    }

    /**
     * 构造函数：使用自定义过滤条件
     * 
     * @param template 原始烘焙模型
     * @param visibilityPredicate 可见性判断条件，返回true保留，false隐藏
     */
    public SelectiveHiddenModel(BakedModel template, Predicate<String> visibilityPredicate) {
        super(template);
        this.hiddenNames = Set.of();
        this.visibilityPredicate = visibilityPredicate;
    }

    /**
     * 获取四边形列表，过滤掉隐藏的部件
     */
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData,
                                    RenderType renderType) {
        List<BakedQuad> originalQuads = super.getQuads(state, side, rand, extraData, renderType);
        
        // 快速路径：无隐藏规则时直接返回原模型
        if (hiddenNames.isEmpty() && visibilityPredicate == null) {
            return originalQuads;
        }

        List<BakedQuad> visibleQuads = new ArrayList<>(originalQuads.size());

        for (BakedQuad quad : originalQuads) {
            if (quad == null) {
                continue;
            }

            // 获取quad的来源元素名称
            // 注意：BakedQuadHelper.getName()在某些Create版本中可能不存在
            String elementName = getQuadName(quad);
            
            // 根据规则判断是否保留
            if (shouldRender(elementName)) {
                visibleQuads.add(quad);
            }
        }

        return visibleQuads;
    }

    /**
     * 获取四边形的来源元素名称
     * 
     * <p>实现方式：
     * 通过纹理名称推断元素名称，因为模型中的每个元素通常使用特定的纹理。
     * 例如："block/versatile_gearbox/fwd_core" → "fwd_core"
     * 
     * <p>注意：此实现依赖于纹理命名规范。如果需要精确匹配模型元素名称，
     * 需要修改模型加载器来在烘焙时将名称信息附加到quad上。
     * 
     * @param quad 四边形
     * @return 元素名称，如果无法获取返回null
     */
    private String getQuadName(BakedQuad quad) {
        if (quad == null) {
            return null;
        }

        // 从纹理名称推断元素名称
        TextureAtlasSprite sprite = quad.getSprite();
        if (sprite != null) {
            ResourceLocation loc = sprite.contents().name();
            if (loc != null) {
                String path = loc.getPath();
                // 从路径提取元素名（如 "block/gearbox/core" → "core"）
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < path.length() - 1) {
                    return path.substring(lastSlash + 1);
                }
                return path;
            }
        }

        return null;
    }

    /**
     * 判断指定名称的元素是否应该渲染
     * 
     * @param elementName 元素名称（可能为null）
     * @return true=渲染，false=隐藏
     */
    protected boolean shouldRender(String elementName) {
        if (elementName == null || elementName.isEmpty()) {
            return true; // 无名元素默认显示
        }

        if (visibilityPredicate != null) {
            return visibilityPredicate.test(elementName);
        }

        return !hiddenNames.contains(elementName);
    }

    /**
     * 创建一个仅显示指定名称部件的模型
     * 
     * @param template 原始模型
     * @param visibleNames 需要显示的部件名称集合
     * @return 过滤后的模型
     */
    public static SelectiveHiddenModel showOnly(BakedModel template, Set<String> visibleNames) {
        return new SelectiveHiddenModel(template, visibleNames::contains);
    }

    /**
     * 创建一个隐藏指定名称部件的模型（便捷方法）
     * 
     * @param template 原始模型
     * @param names 需要隐藏的部件名称
     * @return 过滤后的模型
     */
    public static SelectiveHiddenModel hide(BakedModel template, String... names) {
        Set<String> hidden = new HashSet<>();
        for (String name : names) {
            hidden.add(name);
        }
        return new SelectiveHiddenModel(template, hidden);
    }
}
