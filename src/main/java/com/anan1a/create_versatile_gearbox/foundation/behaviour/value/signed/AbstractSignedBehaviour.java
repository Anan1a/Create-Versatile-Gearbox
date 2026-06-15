package com.anan1a.create_versatile_gearbox.foundation.behaviour.value.signed;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.value.AbstractValueBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

/**
 * 双排 (±) 滑条的抽象基类。
 * <p>
 * 提供双排值映射（row 0=负值，row 1=正值，col=绝对值）及停转 (col 0=0) 的默认实现。
 */
public abstract class AbstractSignedBehaviour extends AbstractValueBehaviour {

    public AbstractSignedBehaviour(Component label, SmartBlockEntity be,
                                   FaceValueBoxTransform slot, int netId,
                                   String typeName) {
        super(label, be, slot, netId, typeName);
    }

    // 将内部值按符号映射到两行 UI，col 0=0（停转）：
    // v ≥ 0 → row 1 (col=v)；v < 0 → row 0 (col=-v)
    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(value < 0 ? 0 : 1, Math.abs(value));
    }

    // 将 UI 的行和 col 映射回内部值
    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
        if (valueSetting.equals(getValueSettings()))
            return;
        int col = valueSetting.value();
        int v = col == 0 ? 0 : (valueSetting.row() == 1 ? col : -col);
        setValue(v);
        playFeedbackSound(this);
    }

    // UI 上显示格式：当前行的倍率/转速文本
    @Override
    protected MutableComponent formatSettings(ValueSettings settings) {
        int col = settings.value();
        int v = col == 0 ? 0 : (settings.row() == 1 ? col : -col);
        return Component.literal(formatValue(v));
    }
}
