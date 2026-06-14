package com.anan1a.create_versatile_gearbox.foundation.behaviour.value;

import java.util.List;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 单排自然数域滑条，值域 {@code [0, maxValue]}。
 * <p>
 * 只有一排，col 值直接对应内部值，无符号无停转特殊处理。
 */
public class FaceNaturalSpeedBehaviour extends AbstractFaceValueBehaviour {

    private static final String TYPE_PREFIX = "face_natural_speed_";
    private final int maxValue;

    public FaceNaturalSpeedBehaviour(Component label, SmartBlockEntity be,
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
                List.of(Component.literal("\u27f3").withStyle(ChatFormatting.BOLD)),
                new ValueSettingsFormatter(this::formatSettings)
        );
    }

    // 单排：row 固定 0，col 直接等于值
    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value);
    }

    // 单排：col 直接作为值
    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
        if (valueSetting.equals(getValueSettings()))
            return;
        setValue(valueSetting.value());
        playFeedbackSound(this);
    }

    @Override
    public String formatValue(Integer index) {
        return index.toString();
    }
}
