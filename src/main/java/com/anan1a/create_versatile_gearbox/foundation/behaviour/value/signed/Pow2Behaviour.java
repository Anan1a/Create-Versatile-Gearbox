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
 * 双排指数幂滑条，内部值 v = 0 为停转，v ≠ 0 对应 2 的幂次。
 * <p>
 * 值域 {@code [-(2M+1), 2M+1]}，其中 M = {@code maxExponent}。
 * 两行各 {@code 2M+2} 个槽位，col 0=停转，col 1~2M+1=倍率：
 * <ul>
 *   <li>Row 0（负幂 -）：v ∈ [-(2M+1), -1]，col = -v，左到右 -1/256 → -1 → -256</li>
 *   <li>Row 1（正幂 +）：v ∈ [0, 2M+1]，col = v，左到右 0 → +1/256 → +1 → +256</li>
 * </ul>
 * "0" 位于两行 col 0（内部值 v=0），"-1" 位于 - 行 col M+1，"1" 位于 + 行 col M+1。
 */
public class Pow2Behaviour extends AbstractSignedBehaviour {

    private static final String TYPE_PREFIX = "pow2_";
    private final int maxExponent;
    private final int milestoneInterval;
    private final List<Component> rowLabels;

    /**
     * @param label             滑条标签
     * @param be                所属 BlockEntity
     * @param slot              交互框变换
     * @param netId             网络 ID
     * @param typeSuffix        类型后缀（拼入 typeName）
     * @param maxExponent       最大指数（值域为 {@code [-(2M+1), 2M+1]}）
     * @param milestoneInterval 刻度间隔（0 = 无刻度）
     * @param rowLabels         双排行标题列表，长度 2（row 0=负行, row 1=正行）
     */
    public Pow2Behaviour(Component label, SmartBlockEntity be,
                         FaceValueBoxTransform slot, int netId,
                         String typeSuffix, int maxExponent,
                         int milestoneInterval, List<Component> rowLabels) {
        super(label, be, slot, netId, TYPE_PREFIX + typeSuffix);
        this.maxExponent = maxExponent;
        this.milestoneInterval = milestoneInterval;
        this.rowLabels = rowLabels;
        // 值域 [-(2M+1), 2M+1]，v=0 为停转，正负各 2M+1 个倍率值
        between(-maxExponent * 2 - 1, maxExponent * 2 + 1);
    }

    /**
     * 简化构造器，行标题默认为 {@code "\u27f3"} 和 {@code "\u27f2"}。
     */
    public Pow2Behaviour(Component label, SmartBlockEntity be,
                         FaceValueBoxTransform slot, int netId,
                         String typeSuffix, int maxExponent,
                         int milestoneInterval) {
        this(label, be, slot, netId, typeSuffix, maxExponent, milestoneInterval,
                List.of(
                        Component.literal("\u27f3").withStyle(ChatFormatting.BOLD),
                        Component.literal("\u27f2").withStyle(ChatFormatting.BOLD)
                ));
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(
                label, maxExponent * 2 + 1, milestoneInterval, rowLabels,
                new ValueSettingsFormatter(this::formatSettings)
        );
    }

    /**
     * 内部值到倍率的转换，供外部（callback、sync）调用。
     * <ul>
     *   <li>v = 0 → 0（停转）</li>
     *   <li>v ≥ 1 → 2^{v-1-M}（范围 +1/256 ~ +256）</li>
     *   <li>v ≤ -1 → -2^{-v-1-M}（范围 -1/256 ~ -256）</li>
     * </ul>
     */
    public float indexToMultiplier(int index) {
        if (index == 0) return 0f;
        if (index > 0) {
            int exp = index - 1 - maxExponent;
            if (exp >= 0) return (float) (1 << exp);
            return 1.0f / (1 << -exp);
        } else {
            int exp = -index - 1 - maxExponent;
            if (exp >= 0) return -(float) (1 << exp);
            return -1.0f / (1 << -exp);
        }
    }

    /** 倍率反转为内部值（{@link #indexToMultiplier} 的逆运算）。 */
    public int multiplierToIndex(float multiplier) {
        if (multiplier == 0) return 0;
        if (multiplier > 0) {
            return Math.round((float) (Math.log(multiplier) / Math.log(2))) + maxExponent + 1;
        } else {
            return -Math.round((float) (Math.log(-multiplier) / Math.log(2))) - 1 - maxExponent;
        }
    }

    /** 内部值 → 显示文本（"0"、"1/256"、"2"、"-1/2"、"-256" 等）。 */
    @Override
    public String formatValue(Integer index) {
        if (index == 0) return "0";
        if (index > 0) {
            int exp = index - 1 - maxExponent;
            if (exp == 0) return "1";
            if (exp > 0) return String.valueOf(1 << exp);
            return "1/" + (1 << -exp);
        } else {
            int exp = -index - 1 - maxExponent;
            if (exp == 0) return "-1";
            if (exp > 0) return "-" + (1 << exp);
            return "-1/" + (1 << -exp);
        }
    }
}
