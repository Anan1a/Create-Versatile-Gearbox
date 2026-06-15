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

    public CountBehaviour(Component label, SmartBlockEntity be,
                          FaceValueBoxTransform slot, int netId,
                          String typeSuffix, int maxValue) {
        super(label, be, slot, netId, TYPE_PREFIX + typeSuffix);
        this.maxValue = maxValue;
        between(0, maxValue);
    }

    // 单排 UI
    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(
                label, maxValue, 1,
                List.of(Component.literal("N").withStyle(ChatFormatting.BOLD)),
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
