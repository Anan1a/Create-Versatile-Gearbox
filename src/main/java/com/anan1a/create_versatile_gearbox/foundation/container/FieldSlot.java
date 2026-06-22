package com.anan1a.create_versatile_gearbox.foundation.container;

import java.util.Arrays;

import com.anan1a.create_versatile_gearbox.foundation.container.serializer.AbstractSerializer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * 命名字段槽：一个面方向上的一种数据字段。
 * <p>
 * 维护 6 个面的同名数值，配合 {@link AbstractSerializer} 序列化到/从 NBT 复合标签。
 * 与 {@link CompositeFaceContainer} 配合使用，多个字段共享同一个面复合标签。
 * <p>
 * 使用示例：
 * <pre>{@code
 * CompositeFaceContainer container = new CompositeFaceContainer();
 * FieldSlot<MyEnum> stateField = container.add(
 *     "FaceState", new EnumSerializer<>(MyEnum.OFF, MyEnum.values()));
 * FieldSlot<Integer> speedField = container.add(
 *     "SpeedValue", new IntSerializer(0));
 *
 * stateField.set(dir, MyEnum.FWD);
 * int speed = speedField.get(dir);
 * }</pre>
 * <p>
 * NBT 中各字段平铺在同一个面复合标签内，互不冲突：
 * <pre>
 * { "DOWN": { "FaceState": "fwd", "SpeedValue": 5, "Multiplier": 1.5 }, ... }
 * </pre>
 *
 * @param <T> 字段值的类型
 */
public class FieldSlot<T> {

    /** NBT 中的键名。 */
    private final String key;
    /** 序列化策略。 */
    private final AbstractSerializer<T> serializer;
    /** 6 个面的数据（D/U/N/S/W/E 顺序）。 */
    private final T[] data;

    /**
     * @param key          NBT 中的键名
     * @param serializer   序列化策略
     */
    @SuppressWarnings("unchecked")
    public FieldSlot(String key, AbstractSerializer<T> serializer) {
        this.key = java.util.Objects.requireNonNull(key, "key must not be null");
        this.serializer = java.util.Objects.requireNonNull(serializer, "serializer must not be null");
        this.data = (T[]) new Object[6];
        fill(serializer.fallback());
    }

    // ===== 面访问 =====

    /** 获取指定面的值。 */
    public T get(Direction face) {
        return data[face.get3DDataValue()];
    }

    /** 设置指定面的值。 */
    public void set(Direction face, T value) {
        data[face.get3DDataValue()] = value;
    }

    /** 将所有面的值设为同一个值。 */
    public void fill(T value) {
        Arrays.fill(data, value);
    }

    /** 返回内部数组的副本。 */
    public T[] toArray() {
        return data.clone();
    }

    // ===== 序列化（由 CompositeFaceContainer 调用） =====

    /** 将此字段直接写入面的复合标签（键名为 {@link #key}）。 */
    public void write(CompoundTag faceTag, Direction face) {
        serializer.write(faceTag, key, data[face.get3DDataValue()]);
    }

    /** 从面的复合标签按 {@link #key} 读取此字段。 */
    public void read(CompoundTag faceTag, Direction face) {
        if (faceTag.contains(key)) {
            data[face.get3DDataValue()] = serializer.read(faceTag, key);
        } else {
            data[face.get3DDataValue()] = serializer.fallback();
        }
    }

    /** 按重映射索引重新排列 6 个面的数据。 */
    public void transform(int[] remap) {
        T[] oldData = toArray();
        for (int i = 0; i < 6; i++) {
            data[i] = oldData[remap[i]];
        }
    }

    // ===== 通用 =====

    /** NBT 键名。 */
    public String key() {
        return key;
    }
}
