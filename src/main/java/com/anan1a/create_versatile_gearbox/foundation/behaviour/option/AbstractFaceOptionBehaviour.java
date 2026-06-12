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
 * 面绑定选项滑条的抽象基类。
 * <p>
 * 提供面索引 / netId 计算、NBT no-op 等公共逻辑。
 * 子类只需实现：
 * <ul>
 *   <li>{@link #getType()} — 返回对应 BehaviourType</li>
 * </ul>
 *
 * @param <E> 实现了 {@link INamedIconOptions} 的枚举类型
 */
public abstract class AbstractFaceOptionBehaviour<E extends Enum<E> & INamedIconOptions>
        extends ScrollOptionBehaviour<E> {

    /** 绑定的面索引（0-5）。 */
    protected final int faceIndex;
    /** 滑条类型序数，用于计算 netId 偏移：netId = faceIndex + ordinal * 6。 */
    protected final int ordinal;
    private final int maxIndex;

    public AbstractFaceOptionBehaviour(Class<E> enum_, Component label, SmartBlockEntity be,
                                       FaceValueBoxTransform slot, int faceIndex, int ordinal) {
        super(enum_, label, be, slot);
        this.faceIndex = faceIndex;
        this.ordinal = ordinal;
        this.maxIndex = enum_.getEnumConstants().length - 1;
    }

    /**
     * 从 faceData 同步滑条值时使用，直接赋值不触发回调/NBT写入/网络包。
     * 超出范围的旧数据会被钳位到合法区间。
     */
    public void clampValue(int raw) {
        this.value = Mth.clamp(raw, 0, maxIndex);
    }

    // 子类必须返回对应 BehaviourType，防止 Map 去重时覆盖
    @Override
    public abstract BehaviourType<?> getType();

    // 每个面用唯一 netId，保证服务端能正确区分各面滑条
    @Override
    public int netId() {
        return faceIndex + ordinal * 6;
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
