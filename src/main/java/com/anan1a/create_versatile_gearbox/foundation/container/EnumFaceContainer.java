package com.anan1a.create_versatile_gearbox.foundation.container;

import com.anan1a.create_versatile_gearbox.foundation.container.serializer.EnumSerializer;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * 面状态枚举容器的默认实现：每个方向存储一个 {@link StringRepresentable} 枚举值。
 * <p>
 * 继承 {@link CompositeFaceContainer}，构造时自动添加名为 {@code "FaceState"} 的枚举字段。
 * 子类可通过 {@link #addField} 添加更多命名字段，共享同一个 NBT 根键。
 * <pre>
 * {
 *   "FaceStateData": {
 *     "DOWN":  { "FaceState": "fwd", ...其他子类字段... },
 *     "UP":    { "FaceState": "rev", ... },
 *     ...
 *   }
 * }
 * </pre>
 * <p>
 * 提供 O(1) 的面状态读写、批量填充、数组导出、以及副本创建功能。
 *
 * @param <T> 面状态枚举类型，必须同时继承 {@link Enum} 并实现 {@link StringRepresentable}
 */
public class EnumFaceContainer<T extends Enum<T> & StringRepresentable> extends CompositeFaceContainer {

    /** 每个方向复合标签内，面状态值的键名。 */
    public static final String FACE_STATE_KEY = "FaceState";

    /** 枚举状态字段槽。 */
    private final FieldSlot<T> stateSlot;

    /** 默认状态值（子类访问需要）。 */
    protected final T defaultValue;

    /**
     * 构造全默认值的容器。
     */
    protected EnumFaceContainer(T defaultValue) {
        this.defaultValue = java.util.Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        this.stateSlot = add(
                FACE_STATE_KEY,
                new EnumSerializer<>(defaultValue, defaultValue.getDeclaringClass().getEnumConstants()));
    }

    // ===== 面访问 =====

    /**
     * 获取指定面的当前状态。
     */
    public T getState(Direction face) {
        return stateSlot.get(face);
    }

    /**
     * 设置指定面的状态。
     *
     * @return 自身（链式调用支持）
     */
    public EnumFaceContainer<T> setState(Direction face, T value) {
        stateSlot.set(face, java.util.Objects.requireNonNull(value, "value must not be null"));
        return this;
    }
}
