package com.anan1a.create_versatile_gearbox.foundation.behaviour.value.signed;

import java.util.List;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 双排整数域滑条，值域 {@code [-maxValue, maxValue]}，col 0 为停转。
 * <p>
 * Row 0 = 负值，Row 1 = 正值，值对称分布。用于需要符号选择且数值连续的场景。
 */
public class IntBehaviour extends AbstractSignedBehaviour {

    private static final String TYPE_PREFIX = "int_";
    private final int maxValue;
    private final int milestoneInterval;
    private final List<Component> rowLabels;

    /**
     * @param label             滑条标签
     * @param be                所属 BlockEntity
     * @param slot              交互框变换
     * @param netId             网络 ID
     * @param typeSuffix        类型后缀（拼入 typeName）
     * @param maxValue          值域最大值（值域为 {@code [-maxValue, maxValue]}）
     * @param milestoneInterval 刻度间隔（0 = 无刻度）
     * @param rowLabels         双排行标题列表，长度 2（row 0=负行, row 1=正行）
     */
    public IntBehaviour(Component label, SmartBlockEntity be,
                        FaceValueBoxTransform slot, int netId,
                        String typeSuffix, int maxValue,
                        int milestoneInterval, List<Component> rowLabels) {
        super(label, be, slot, netId, TYPE_PREFIX + typeSuffix);
        this.maxValue = maxValue;
        this.milestoneInterval = milestoneInterval;
        this.rowLabels = rowLabels;
        between(-maxValue, maxValue);
    }

    /**
     * 简化构造器，行标题默认为 {@code "-"} 和 {@code "+"}。
     */
    public IntBehaviour(Component label, SmartBlockEntity be,
                        FaceValueBoxTransform slot, int netId,
                        String typeSuffix, int maxValue,
                        int milestoneInterval) {
        this(label, be, slot, netId, typeSuffix, maxValue, milestoneInterval,
                List.of(
                        Component.literal("-").withStyle(ChatFormatting.BOLD),
                        Component.literal("+").withStyle(ChatFormatting.BOLD)
                ));
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
}
