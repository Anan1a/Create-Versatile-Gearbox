package com.anan1a.create_versatile_gearbox.foundation.container;

import java.util.Arrays;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;

/**
 * 面状态枚举容器的默认实现：每个方向存储一个 {@link StringRepresentable} 枚举值。
 * <p>
 * 继承 {@link FaceStateContainer}，以 {@code "FaceState"} 键在每个方向的
 * 复合标签中存储枚举值的序列化名称：
 * <pre>
 * {
 *   "FaceStateData": {
 *     "DOWN":  { "FaceState": "fwd" },
 *     "UP":    { "FaceState": "rev" },
 *     ...
 *   }
 * }
 * </pre>
 * <p>
 * 提供 O(1) 的面状态读写、批量填充、数组导出、以及副本创建功能。
 *
 * @param <T> 面状态枚举类型，必须同时继承 {@link Enum} 并实现 {@link StringRepresentable}
 */
public class EnumFaceContainer<T extends Enum<T> & StringRepresentable> extends FaceStateContainer {

    /** 每个方向复合标签内，面状态值的键名。 */
    private static final String FACE_STATE_KEY = "FaceState";

    /** 六个面状态值的运行时缓存数组。 */
    private final T[] states;

    /** 默认状态值。 */
    private final T defaultValue;

    /** 枚举常量缓存。 */
    private final T[] enumConstants;

    /**
     * 构造全默认值的容器。
     */
    @SuppressWarnings("unchecked")
    protected EnumFaceContainer(T defaultValue) {
        this.defaultValue = java.util.Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        Class<T> enumClass = defaultValue.getDeclaringClass();
        this.states = (T[]) java.lang.reflect.Array.newInstance(enumClass, 6);
        this.enumConstants = enumClass.getEnumConstants();
        fill(defaultValue);
    }

    /**
     * 从已有数组创建容器。
     */
    @SuppressWarnings("unchecked")
    protected EnumFaceContainer(T defaultValue, T[] array) {
        this.defaultValue = java.util.Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        Class<T> enumClass = defaultValue.getDeclaringClass();
        this.states = (T[]) java.lang.reflect.Array.newInstance(enumClass, 6);
        this.enumConstants = enumClass.getEnumConstants();
        System.arraycopy(array, 0, this.states, 0, Math.min(array.length, 6));
        for (int i = 0; i < 6; i++) {
            if (this.states[i] == null) {
                this.states[i] = defaultValue;
            }
        }
    }

    // ===== 工厂方法 =====

    /**
     * 创建新容器，所有面初始化为指定默认值。
     *
     * @param defaultValue 默认状态值
     * @param <T>          面状态枚举类型
     * @return 新的容器
     */
    public static <T extends Enum<T> & StringRepresentable> EnumFaceContainer<T> of(T defaultValue) {
        return new EnumFaceContainer<>(defaultValue);
    }

    /**
     * 从现有数组创建容器。
     *
     * @param defaultValue 默认值（用于填充数组不足 6 个或含 null 的位置）
     * @param states       源数据数组（最多取前 6 个元素）
     * @param <T>          面状态枚举类型
     * @return 新的容器
     */
    public static <T extends Enum<T> & StringRepresentable> EnumFaceContainer<T> fromArray(T defaultValue, T[] states) {
        return new EnumFaceContainer<>(defaultValue, states);
    }

    /**
     * 从 NBT 反序列化创建容器。
     *
     * @param nbt          NBT 数据
     * @param defaultValue 默认值
     * @param <T>          面状态枚举类型
     * @return 反序列化后的容器
     */
    public static <T extends Enum<T> & StringRepresentable> EnumFaceContainer<T> fromNbt(CompoundTag nbt, T defaultValue) {
        EnumFaceContainer<T> container = new EnumFaceContainer<>(defaultValue);
        container.fromNbt(nbt);
        return container;
    }

    /**
     * 创建当前容器的独立副本。
     *
     * @return 状态相同但独立的新容器
     */
    public EnumFaceContainer<T> copy() {
        return new EnumFaceContainer<>(defaultValue, states);
    }

    // ===== 框架方法实现 =====

    @Override
    protected void writeFace(CompoundTag faceTag, Direction face) {
        T state = states[face.get3DDataValue()];
        faceTag.putString(FACE_STATE_KEY,
            state != null ? state.getSerializedName() : defaultValue.getSerializedName());
    }

    @Override
    protected void readFace(CompoundTag faceTag, Direction face) {
        if (faceTag.contains(FACE_STATE_KEY)) {
            states[face.get3DDataValue()] = enumValueOf(
                faceTag.getString(FACE_STATE_KEY), defaultValue, enumConstants);
        } else {
            states[face.get3DDataValue()] = defaultValue;
        }
    }

    // ===== 面访问 =====

    /**
     * 获取指定面的当前状态。
     *
     * @param face 要查询的面方向
     * @return 该面的状态值
     */
    public T get(Direction face) {
        return states[java.util.Objects.requireNonNull(face, "face must not be null").get3DDataValue()];
    }

    /**
     * 设置指定面的状态。
     *
     * @param face  要修改的面方向
     * @param value 新的状态值
     * @return 自身（链式调用支持）
     */
    public EnumFaceContainer<T> set(Direction face, T value) {
        states[face.get3DDataValue()] = java.util.Objects.requireNonNull(value, "value must not be null");
        return this;
    }

    /**
     * 将所有面的状态设为同一个值。
     *
     * @param value 要填充的状态值
     * @return 自身
     */
    public EnumFaceContainer<T> fill(T value) {
        Arrays.fill(states, java.util.Objects.requireNonNull(value, "value must not be null"));
        return this;
    }

    // ===== 便捷查询 =====

    /**
     * 检查指定面的状态是否等于默认值。
     *
     * @param face 要检查的面
     * @return true 如果该面状态 == defaultValue
     */
    public boolean isDefault(Direction face) {
        return states[face.get3DDataValue()] == defaultValue;
    }

    /**
     * 检查所有 6 个面的状态是否都是默认值。
     *
     * @return true 如果所有面都是默认值
     */
    public boolean allDefault() {
        for (T s : states) {
            if (s != defaultValue) return false;
        }
        return true;
    }

    // ===== 蓝图变换 =====

    /**
     * 按重映射索引重新排列 6 个面的枚举状态。
     * <p>
     * 子类若新增其他数据数组，应覆写此方法：先 {@code super.transform(remap)}，
     * 再重排自己的数组。
     */
    @Override
    public void transform(int[] remap) {
        if (remap == null || remap.length != 6) {
            throw new IllegalArgumentException("remap must be an int[6]");
        }
        T[] oldStates = toArray();
        for (int newIdx = 0; newIdx < 6; newIdx++) {
            states[newIdx] = oldStates[remap[newIdx]];
        }
    }

    // ===== 数据导出 =====

    /**
     * 返回内部状态数组的副本。
     *
     * @return 6 个面状态的数组副本（D/U/N/S/W/E 顺序）
     */
    public T[] toArray() {
        return states.clone();
    }

    // ===== 对象方法 =====

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EnumFaceContainer<?> other)) return false;
        return Arrays.equals(this.states, other.states);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(states);
    }

    @Override
    public String toString() {
        return "EnumFaceContainer{" +
            "DOWN=" + get(Direction.DOWN) +
            ", UP=" + get(Direction.UP) +
            ", NORTH=" + get(Direction.NORTH) +
            ", SOUTH=" + get(Direction.SOUTH) +
            ", WEST=" + get(Direction.WEST) +
            ", EAST=" + get(Direction.EAST) +
            '}';
    }
}
