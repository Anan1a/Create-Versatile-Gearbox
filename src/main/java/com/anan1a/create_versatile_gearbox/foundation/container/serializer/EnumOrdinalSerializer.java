package com.anan1a.create_versatile_gearbox.foundation.container.serializer;

import net.minecraft.nbt.CompoundTag;

/**
 * 枚举类型的面序列化器 — 按序数存储。
 * <p>
 * 与 {@link EnumSerializer}（按名称存字符串）不同，此序列化器存枚举的
 * {@link Enum#ordinal()} 序数，更紧凑但要求枚举常量顺序在版本间不变：
 * <pre>
 * { "DOWN": { "Value": 0 }, "UP": { "Value": 2 }, ... }
 * </pre>
 * <p>
 * 序数超出范围时返回 {@code fallback} 而非抛异常，确保容错。
 *
 * @param <T> 枚举类型（不需要实现 {@link StringRepresentable}，只需要是 {@link Enum}）
 */
public class EnumOrdinalSerializer<T extends Enum<T>> extends AbstractSerializer<T> {

    private final T[] constants;

    /**
     * @param fallback  默认值（也用作序数越界时的回退值）
     * @param constants 枚举常量数组（{@code EnumClass.getEnumConstants()}）
     */
    public EnumOrdinalSerializer(T fallback, T[] constants) {
        super(fallback);
        this.constants = java.util.Objects.requireNonNull(constants, "constants must not be null");
    }

    @Override
    public T read(CompoundTag tag, String key) {
        int ordinal = tag.getInt(key);
        if (ordinal >= 0 && ordinal < constants.length) {
            return constants[ordinal];
        }
        return fallback();
    }

    @Override
    public void write(CompoundTag tag, String key, T value) {
        tag.putInt(key, value.ordinal());
    }
}
