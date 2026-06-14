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
 * 每个面的滑条 behaviour，继承 AbstractFaceValueBehaviour。
 * <p>
 * 双排 +/- 离散选择，值域 {@code [-maxValue, maxValue]}，col 0 为停转。
 */
public class FaceSpeedBehaviour extends AbstractFaceValueBehaviour {

    private static final String TYPE_PREFIX = "face_speed_";
    private final int maxValue;

    public FaceSpeedBehaviour(Component label, SmartBlockEntity be,
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
                label, maxValue, 32,
                List.of(
                        Component.literal("\u27f3").withStyle(ChatFormatting.BOLD),
                        Component.literal("\u27f2").withStyle(ChatFormatting.BOLD)
                ),
                new ValueSettingsFormatter(this::formatSettings)
        );
    }

    @Override
    public String formatValue(Integer index) {
        return index.toString();
    }
}
