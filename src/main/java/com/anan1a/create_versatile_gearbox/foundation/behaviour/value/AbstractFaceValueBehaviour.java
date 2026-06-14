package com.anan1a.create_versatile_gearbox.foundation.behaviour.value;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 面绑定滑条的抽象基类。
 * <p>
 * 提供双排 (±) 值映射、停转 (col 0=0)、{@link BehaviourType} 存储、netId、NBT no-op 等公共逻辑。
 * 子类只需实现：
 * <ul>
 *   <li>{@link #createBoard(Player, BlockHitResult)} — 创建 UI 面板</li>
 *   <li>{@link #formatValue(Integer)} — 内部值 → 显示文本</li>
 * </ul>
 */
public abstract class AbstractFaceValueBehaviour extends ScrollValueBehaviour {

    /** 该滑条的 netId（由调用方决定，用于区分不同面/不同类型）。 */
    private final int netId;
    /** 该滑条的 BehaviourType（由子类传前缀，父类在此统一构造）。 */
    private final BehaviourType<?> type;

    public AbstractFaceValueBehaviour(Component label, SmartBlockEntity be,
                                      FaceValueBoxTransform slot, int netId,
                                      String typeName) {
        super(label, be, slot);
        this.netId = netId;
        this.type = new BehaviourType<>(typeName);
        // 子类通过 formatValue() 实现具体文本格式，父类在此统一注册 formatter
        withFormatter(this::formatValue);
    }

    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    @Override
    public int netId() {
        return netId;
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

    /** 内部值 → 显示文本。由子类实现具体格式。 */
    public abstract String formatValue(Integer index);

    // UI 上显示格式：当前行的倍率/转速文本
    protected MutableComponent formatSettings(ValueSettings settings) {
        int col = settings.value();
        int v = col == 0 ? 0 : (settings.row() == 1 ? col : -col);
        return Component.literal(formatValue(v));
    }

    // NBT 由 AdvancedGearboxFaceContainer 统一管理
    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // no-op
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // no-op
    }
}
