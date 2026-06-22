package com.anan1a.create_versatile_gearbox.foundation.container.serializer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;

/**
 * 枚举类型的面序列化器。
 * <p>
 * 在每个方向的 NBT 子标签中以 {@code "Value"} 键存储枚举的序列化名称：
 * <pre>
 * { "DOWN": { "Value": "fwd" }, "UP": { "Value": "rev" }, ... }
 * </pre>
 * <p>
 * 反序列化时按 {@link StringRepresentable#getSerializedName()} 匹配。
 * 不匹配时返回 {@code fallback}，确保版本兼容。
 *
 * @param <T> 实现了 {@link StringRepresentable} 的枚举类型
 */
public class EnumSerializer<T extends Enum<T> & StringRepresentable> extends AbstractSerializer<T> {

    private final T[] constants;

    /**
     * @param fallback  默认值（也用作不匹配时的回退值）
     * @param constants 枚举常量数组（{@code EnumClass.getEnumConstants()}）
     */
    public EnumSerializer(T fallback, T[] constants) {
        super(fallback);
        this.constants = java.util.Objects.requireNonNull(constants, "constants must not be null");
    }

    @Override
    public T read(CompoundTag tag, String key) {
        String name = tag.getString(key);
        for (T constant : constants) {
            if (constant.getSerializedName().equals(name)) {
                return constant;
            }
        }
        return fallback();
    }

    @Override
    public void write(CompoundTag tag, String key, T value) {
        tag.putString(key, value.getSerializedName());
    }
}
