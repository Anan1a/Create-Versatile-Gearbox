package com.anan1a.create_versatile_gearbox.foundation.behaviour.option;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 选项滑条的抽象基类。
 * <p>
 * 提供 {@link BehaviourType} 构造、netId、NBT no-op、值钳位等公共逻辑。
 * 子类需在构造器中传入拼接好的完整类型名（{@code TYPE_PREFIX + typeSuffix}）。
 *
 * @param <E> 实现了 {@link INamedIconOptions} 的枚举类型
 */
public abstract class AbstractOptionBehaviour<E extends Enum<E> & INamedIconOptions>
        extends ScrollOptionBehaviour<E> {

    /** 该滑条的 netId（由调用方决定，用于区分不同面/不同类型）。 */
    private final int netId;
    /** 该滑条的 BehaviourType（由于类传前缀，父类在此统一构造）。 */
    private final BehaviourType<?> type;
    private final int maxIndex;

    public AbstractOptionBehaviour(Class<E> enum_, Component label, SmartBlockEntity be,
                                   FaceValueBoxTransform slot, int netId,
                                   String typeName) {
        super(enum_, label, be, slot);
        this.netId = netId;
        this.type = new BehaviourType<>(typeName);
        this.maxIndex = enum_.getEnumConstants().length - 1;
    }

    /**
     * 从 faceData 同步滑条值时使用，直接赋值不触发回调/NBT写入/网络包。
     * 超出范围的旧数据会被钳位到合法区间。
     */
    public void clampValue(int raw) {
        this.value = Mth.clamp(raw, 0, maxIndex);
    }

    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    @Override
    public int netId() {
        return netId;
    }

    // NBT 由外部统一管理
    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // no-op
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // no-op
    }
}
