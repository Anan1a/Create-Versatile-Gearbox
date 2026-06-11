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
 * 提供双排 (±) 值映射、停转 (col 0=0)、{@link BehaviourType} 存储、netId 计算、NBT no-op 等公共逻辑。
 * 子类只需实现：
 * <ul>
 *   <li>{@link #createBoard(Player, BlockHitResult)} — 创建 UI 面板</li>
 *   <li>{@link #formatValue(Integer)} — 内部值 → 显示文本</li>
 * </ul>
 */
public abstract class AbstractFaceValueBehaviour extends ScrollValueBehaviour {

    /** 绑定的面索引（0-5）。 */
    protected final int faceIndex;
    /** 滑条类型序数，用于计算 netId 偏移：netId = faceIndex + ordinal * 6。 */
    protected final int ordinal;
    /** 该滑条的 BehaviourType（由外部传入，每面独立，防止 Map 去重时覆盖）。 */
    private final BehaviourType<?> type;

    public AbstractFaceValueBehaviour(Component label, SmartBlockEntity be,
                                      FaceValueBoxTransform slot, int faceIndex, int ordinal,
                                      BehaviourType<?> type) {
        super(label, be, slot);
        this.faceIndex = faceIndex;
        this.ordinal = ordinal;
        this.type = type;
        // 子类通过 formatValue() 实现具体文本格式，父类在此统一注册 formatter
        withFormatter(this::formatValue);
    }

    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    // 每个面用唯一 netId，保证服务端能正确区分各面滑条
    @Override
    public int netId() {
        return faceIndex + ordinal * 6;
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
