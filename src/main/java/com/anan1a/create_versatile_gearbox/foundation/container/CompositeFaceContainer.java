package com.anan1a.create_versatile_gearbox.foundation.container;

import java.util.LinkedHashMap;
import java.util.Map;

import com.anan1a.create_versatile_gearbox.foundation.container.serializer.AbstractSerializer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * 复合面数据容器：多个命名字段共享同一个 NBT 根键。
 * <p>
 * 管理 NBT 结构中 {@code "FaceStateData"} 根键下的 6 个方向子标签，
 * 每个标签内可平铺存储多个不同类型的字段：
 * <pre>
 * {
 *   "FaceStateData": {
 *     "DOWN":  { "FaceState": "fwd", "SpeedValue": 5, "Multiplier": 1.5 },
 *     "UP":    { "FaceState": "rev", "SpeedValue": 0, "Multiplier": 1.0 },
 *     ...
 *   }
 * }
 * </pre>
 * <p>
 * 使用示例：
 * <pre>{@code
 * CompositeFaceContainer container = new CompositeFaceContainer();
 *
 * FieldSlot<MyEnum> stateField = container.add("FaceState",
 *     new EnumSerializer<>(MyEnum.OFF, MyEnum.values()));
 *
 * stateField.set(dir, MyEnum.FWD);
 *
 * // 序列化
 * CompoundTag nbt = new CompoundTag();
 * container.writeToRoot(nbt);
 * container.readFromRoot(nbt);
 * }</pre>
 */
public class CompositeFaceContainer {

    /** NBT 中面状态数据的根键名。 */
    public static final String FACE_ROOT_KEY = "FaceStateData";

    /**
     * 方向键名常量，与 {@link Direction#get3DDataValue()} 的序数严格对齐。
     */
    protected static final String[] DIRECTION_KEYS = {"DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST"};

    /** 按注册顺序维护的所有字段。 */
    private final Map<String, FieldSlot<?>> fields = new LinkedHashMap<>();

    // ===== 字段注册 =====

    /**
     * 注册一个命名字段。
     * <p>
     * 字段的 NBT 键名即 {@code key}，写入/读取时该值直接以键值对形式存在于
     * 每个方向的复合标签中。初始值和 NBT 缺键回退值均来自 {@link AbstractSerializer#fallback()}。
     *
     * @param key          NBT 中的键名（同一容器内不能重复）
     * @param serializer   序列化策略
     * @param <T>          字段值的类型
     * @return 注册的字段槽，用于后续读写
     * @throws IllegalArgumentException 如果键名已存在
     */
    public <T> FieldSlot<T> add(String key, AbstractSerializer<T> serializer) {
        if (fields.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate field key: " + key);
        }
        FieldSlot<T> slot = new FieldSlot<>(key, serializer);
        fields.put(key, slot);
        return slot;
    }

    /**
     * 按键名获取已注册的字段槽。
     *
     * @param key 字段的 NBT 键名
     * @param <T> 字段值的类型
     * @return 字段槽，不存在时返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> FieldSlot<T> getField(String key) {
        return (FieldSlot<T>) fields.get(key);
    }

    // ===== NBT 序列化 =====

    /**
     * 将 6 个面的数据序列化到父 NBT 的根键下，由 BE 在 {@code write()} 中调用。
     * <p>
     * 遍历 6 个方向，为每个方向创建一个独立复合标签，
     * 遍历该方向的所有已注册字段写入，最后统一放入 {@link #FACE_ROOT_KEY} 根键。
     */
    public final void writeToRoot(CompoundTag tag) {
        CompoundTag nbt = new CompoundTag();
        for (int i = 0; i < 6; i++) {
            CompoundTag faceTag = new CompoundTag();
            for (FieldSlot<?> slot : fields.values()) {
                slot.write(faceTag, Direction.from3DDataValue(i));
            }
            nbt.put(DIRECTION_KEYS[i], faceTag);
        }
        tag.put(FACE_ROOT_KEY, nbt);
    }

    /**
     * 从父 NBT 的根键下反序列化恢复 6 个面的数据，由 BE 在 {@code read()} 中调用。
     * <p>
     * 如果 NBT 中缺失根键，保持当前默认值不变。
     * 遍历 6 个方向，读取各方向复合标签，遍历该方向所有已注册字段读取。
     */
    public final void readFromRoot(CompoundTag tag) {
        if (!tag.contains(FACE_ROOT_KEY)) return;
        CompoundTag nbt = tag.getCompound(FACE_ROOT_KEY);
        for (int i = 0; i < 6; i++) {
            String key = DIRECTION_KEYS[i];
            CompoundTag faceTag = nbt.contains(key) ? nbt.getCompound(key) : new CompoundTag();
            for (FieldSlot<?> slot : fields.values()) {
                slot.read(faceTag, Direction.from3DDataValue(i));
            }
        }
    }

    /**
     * 蓝图变换：按重映射索引重新排列 6 个面的数据。
     * <p>
     * 所有已注册字段按 {@code remap} 重排。remap 数组长度必须为 6，
     * 索引 i 表示新位置 Direction.from3DDataValue(i)，
     * 值 remap[i] 表示该位置应从旧方向 Direction.from3DDataValue(remap[i]) 拷贝数据。
     *
     * @param remap 重映射索引数组
     * @throws IllegalArgumentException 如果 remap 为 null 或长度 != 6
     */
    public void transform(int[] remap) {
        for (FieldSlot<?> slot : fields.values()) {
            slot.transform(remap);
        }
    }
}
