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
 * 双排指数幂滑条，内部值 v 直接为指数（2 的幂次）。
 * <p>
 * 值域 {@code [-maxExponent, maxExponent]}，col 0 为恒等。
 * Row 0 = 除幂 (÷2^{|v|})，Row 1 = 乘幂 (×2^{|v|})。
 * 不涉及负数，只有乘除幂。
 */
public class ExpBehaviour extends AbstractSignedBehaviour {

    private static final String TYPE_PREFIX = "exp_";
    private final int maxExponent;
    private final int milestoneInterval;
    private final List<Component> rowLabels;

    /**
     * @param label             滑条标签
     * @param be                所属 BlockEntity
     * @param slot              交互框变换
     * @param netId             网络 ID
     * @param typeSuffix        类型后缀（拼入 typeName）
     * @param maxExponent       最大指数（值域为 {@code [-maxExponent, maxExponent]}）
     * @param milestoneInterval 刻度间隔（0 = 无刻度）
     * @param rowLabels         双排行标题列表，长度 2（row 0=负行, row 1=正行）
     */
    public ExpBehaviour(Component label, SmartBlockEntity be,
                        FaceValueBoxTransform slot, int netId,
                        String typeSuffix, int maxExponent,
                        int milestoneInterval, List<Component> rowLabels) {
        super(label, be, slot, netId, TYPE_PREFIX + typeSuffix);
        this.maxExponent = maxExponent;
        this.milestoneInterval = milestoneInterval;
        this.rowLabels = rowLabels;
        between(-maxExponent, maxExponent);
    }

    /**
     * 简化构造器，行标题默认为 {@code "÷"} 和 {@code "×"}。
     */
    public ExpBehaviour(Component label, SmartBlockEntity be,
                        FaceValueBoxTransform slot, int netId,
                        String typeSuffix, int maxExponent,
                        int milestoneInterval) {
        this(label, be, slot, netId, typeSuffix, maxExponent, milestoneInterval,
                List.of(
                        Component.literal("\u00F7").withStyle(ChatFormatting.BOLD),
                        Component.literal("\u00D7").withStyle(ChatFormatting.BOLD)
                ));
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(
                label, maxExponent, milestoneInterval, rowLabels,
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
