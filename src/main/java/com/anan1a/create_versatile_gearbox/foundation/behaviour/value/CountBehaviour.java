package com.anan1a.create_versatile_gearbox.foundation.behaviour.value;

import java.util.List;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 单排自然数域滑条，值域 {@code [0, maxValue]}。
 * <p>
 * 只有一排，col 值直接对应内部值，无符号无停转特殊处理。
 */
public class CountBehaviour extends AbstractValueBehaviour {

    private static final String TYPE_PREFIX = "count_";
    private final int maxValue;
    private final int milestoneInterval;
    private final List<Component> rowLabels;

    /**
     * @param label             滑条标签
     * @param be                所属 BlockEntity
     * @param slot              交互框变换
     * @param netId             网络 ID
     * @param typeSuffix        类型后缀（拼入 typeName）
     * @param maxValue          值域最大值（值域为 {@code [0, maxValue]}）
     * @param milestoneInterval 刻度间隔（0 = 无刻度）
     * @param rowLabels         单排行标题列表，长度 1
     */
    public CountBehaviour(Component label, SmartBlockEntity be,
                          FaceValueBoxTransform slot, int netId,
                          String typeSuffix, int maxValue,
                          int milestoneInterval, List<Component> rowLabels) {
        super(label, be, slot, netId, TYPE_PREFIX + typeSuffix);
        this.maxValue = maxValue;
        this.milestoneInterval = milestoneInterval;
        this.rowLabels = rowLabels;
        between(0, maxValue);
    }

    /**
     * 简化构造器，行标题默认为 {@code "N"}。
     */
    public CountBehaviour(Component label, SmartBlockEntity be,
                          FaceValueBoxTransform slot, int netId,
                          String typeSuffix, int maxValue,
                          int milestoneInterval) {
        this(label, be, slot, netId, typeSuffix, maxValue, milestoneInterval,
                List.of(Component.literal("N").withStyle(ChatFormatting.BOLD)));
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(
                label, maxValue, milestoneInterval, rowLabels,
                new ValueSettingsFormatter(this::formatSettings)
        );
    }

    @Override
    public String formatValue(Integer index) {
        return index.toString();
    }

    @Override
    protected MutableComponent formatSettings(ValueSettings settings) {
        return Component.literal(formatValue(settings.value()));
    }
}
