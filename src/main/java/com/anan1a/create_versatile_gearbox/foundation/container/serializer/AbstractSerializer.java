package com.anan1a.create_versatile_gearbox.foundation.container.serializer;

import net.minecraft.nbt.CompoundTag;

/**
 * 单面数据的序列化策略基类。
 * <p>
 * 定义如何将单个面的数据从 NBT 复合标签中按指定键名读写。
 * 键名由调用方（通常是 {@code FieldSlot}）传入，因此多个字段可共享同一个
 * 面复合标签而互不冲突。
 * <p>
 * 统一管理 {@link #fallback()} 的存储，子类只需实现 {@link #read} 和 {@link #write}。
 *
 * @param <T> 存储值的类型
 */
public abstract class AbstractSerializer<T> {

    private final T fallback;

    protected AbstractSerializer(T fallback) {
        this.fallback = java.util.Objects.requireNonNull(fallback, "fallback must not be null");
    }

    /**
     * 从 NBT 复合标签中读取指定键名的值。
     */
    public abstract T read(CompoundTag tag, String key);

    /**
     * 将值写入 NBT 复合标签的指定键名。
     */
    public abstract void write(CompoundTag tag, String key, T value);

    /**
     * 读取失败或初始化时的默认值。
     */
    public final T fallback() {
        return fallback;
    }
}
