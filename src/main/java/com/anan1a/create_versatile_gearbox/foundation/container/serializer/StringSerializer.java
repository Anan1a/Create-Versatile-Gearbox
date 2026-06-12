package com.anan1a.create_versatile_gearbox.foundation.container.serializer;

import net.minecraft.nbt.CompoundTag;

/**
 * {@code String} 类型的面序列化器。
 * <p>
 * 在每个方向的 NBT 子标签中以 {@code "Value"} 键存储字符串：
 * <pre>
 * { "DOWN": { "Value": "hello" }, "UP": { "Value": "world" }, ... }
 * </pre>
 */
public class StringSerializer extends AbstractSerializer<String> {

    public StringSerializer(String fallback) {
        super(fallback);
    }

    @Override
    public String read(CompoundTag tag, String key) {
        return tag.getString(key);
    }

    @Override
    public void write(CompoundTag tag, String key, String value) {
        tag.putString(key, value);
    }
}
