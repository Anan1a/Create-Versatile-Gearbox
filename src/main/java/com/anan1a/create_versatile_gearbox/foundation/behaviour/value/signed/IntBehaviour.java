package com.anan1a.create_versatile_gearbox.foundation.behaviour.value;

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

    public IntBehaviour(Component label, SmartBlockEntity be,
                        FaceValueBoxTransform slot, int netId,
                        String typeSuffix, int maxValue) {
        super(label, be, slot, netId, TYPE_PREFIX + typeSuffix);
        this.maxValue = maxValue;
        // 值域 [-maxValue, maxValue]
        between(-maxValue, maxValue);
    }

    // 滑条 UI：双排设计，第一排=反转(负值，显示"-")，第二排=正转(正值，显示"+")，值域 0~maxValue
    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(
                label, maxValue, 8,
                List.of(
                        Component.literal("-").withStyle(ChatFormatting.BOLD),
                        Component.literal("+").withStyle(ChatFormatting.BOLD)
                ),
                new ValueSettingsFormatter(this::formatSettings)
        );
    }

    @Override
    public String formatValue(Integer index) {
        return index.toString();
    }
}
