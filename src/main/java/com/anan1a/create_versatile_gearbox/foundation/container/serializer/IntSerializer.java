package com.anan1a.create_versatile_gearbox.foundation.container.serializer;

import net.minecraft.nbt.CompoundTag;

/**
 * {@code int} 类型的面序列化器。
 * <p>
 * 在每个方向的 NBT 子标签中以 {@code "Value"} 键存储整数：
 * <pre>
 * { "DOWN": { "Value": 128 }, "UP": { "Value": -64 }, ... }
 * </pre>
 */
public class IntSerializer extends AbstractSerializer<Integer> {

    public IntSerializer(int fallback) {
        super(fallback);
    }

    @Override
    public Integer read(CompoundTag tag, String key) {
        return tag.getInt(key);
    }

    @Override
    public void write(CompoundTag tag, String key, Integer value) {
        tag.putInt(key, value);
    }
}
