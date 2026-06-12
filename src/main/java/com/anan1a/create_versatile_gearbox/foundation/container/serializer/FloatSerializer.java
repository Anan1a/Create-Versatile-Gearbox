package com.anan1a.create_versatile_gearbox.foundation.container.serializer;

import net.minecraft.nbt.CompoundTag;

/**
 * {@code float} 类型的面序列化器。
 * <p>
 * 在每个方向的 NBT 子标签中以 {@code "Value"} 键存储浮点数：
 * <pre>
 * { "DOWN": { "Value": 1.5 }, "UP": { "Value": -0.25 }, ... }
 * </pre>
 */
public class FloatSerializer extends AbstractSerializer<Float> {

    public FloatSerializer(float fallback) {
        super(fallback);
    }

    @Override
    public Float read(CompoundTag tag, String key) {
        return tag.getFloat(key);
    }

    @Override
    public void write(CompoundTag tag, String key, Float value) {
        tag.putFloat(key, value);
    }
}
