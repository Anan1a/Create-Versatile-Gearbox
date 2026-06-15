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
 * 双排指数幂滑条，内部值 v 直接为指数（2 的幂次）。
 * <p>
 * 值域 {@code [-maxExponent, maxExponent]}，col 0 为恒等。
 * Row 0 = 除幂 (÷2^{|v|})，Row 1 = 乘幂 (×2^{|v|})。
 * 不涉及负数，只有乘除幂。
 */
public class ExpBehaviour extends AbstractSignedBehaviour {

    private static final String TYPE_PREFIX = "exp_";
    private final int maxExponent;

    public ExpBehaviour(Component label, SmartBlockEntity be,
                        FaceValueBoxTransform slot, int netId,
                        String typeSuffix, int maxExponent) {
        super(label, be, slot, netId, TYPE_PREFIX + typeSuffix);
        this.maxExponent = maxExponent;
        between(-maxExponent, maxExponent);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(
                label, maxExponent, 1,
                List.of(
                        Component.literal("\u00F7").withStyle(ChatFormatting.BOLD),
                        Component.literal("\u00D7").withStyle(ChatFormatting.BOLD)
                ),
                new ValueSettingsFormatter(this::formatSettings)
        );
    }

    /** 指数 → 浮点倍率（供外部使用）。 */
    public float expToMultiplier(int exp) {
        if (exp == 0) return 1.0f;
        if (exp > 0) return (float) (1 << exp);
        return 1.0f / (1 << -exp);
    }

    /** 浮点倍率 → 指数（数据同步时使用）。 */
    public int multiplierToExp(float multiplier) {
        if (multiplier == 1.0f) return 0;
        if (multiplier > 1.0f) {
            return Math.round((float) (Math.log(multiplier) / Math.log(2)));
        } else {
            return -Math.round((float) (Math.log(1.0f / multiplier) / Math.log(2)));
        }
    }

    /** 内部值 v（指数）→ 显示文本。 */
    @Override
    public String formatValue(Integer v) {
        if (v == 0) return "×1";
        if (v > 0) return "×" + (1 << v);
        return "÷" + (1 << -v);
    }
}
