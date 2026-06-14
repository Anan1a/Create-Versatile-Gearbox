package com.anan1a.create_versatile_gearbox.foundation.behaviour.value;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 数值滑条的抽象基类。
 * <p>
 * 提供 {@link BehaviourType} 存储、netId、NBT no-op 等公共逻辑。
 * 子类需实现 {@link #formatValue(Integer)}、{@link #formatSettings(ValueSettings)}，
 * 以及可选的 {@link #getValueSettings()} / {@link #setValueSettings(Player, ValueSettings, boolean)} 覆写。
 * 构造时传入拼接好的完整类型名（{@code TYPE_PREFIX + typeSuffix}）。
 */
public abstract class AbstractValueBehaviour extends ScrollValueBehaviour {

    /** 该滑条的 netId（由调用方决定，用于区分不同面/不同类型）。 */
    private final int netId;
    /** 该滑条的 BehaviourType（由子类传前缀，父类在此统一构造）。 */
    private final BehaviourType<?> type;

    public AbstractValueBehaviour(Component label, SmartBlockEntity be,
                                  FaceValueBoxTransform slot, int netId,
                                  String typeName) {
        super(label, be, slot);
        this.netId = netId;
        this.type = new BehaviourType<>(typeName);
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

    /** 内部值 → 显示文本。由子类实现具体格式。 */
    public abstract String formatValue(Integer index);

    /** 由子类提供设置面板上（ValueSettings → 显示文本）的格式转换。 */
    protected abstract MutableComponent formatSettings(ValueSettings settings);

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
