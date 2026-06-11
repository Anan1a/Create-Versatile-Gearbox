package com.anan1a.create_versatile_gearbox.foundation.container;

import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;

/**
 * 面状态数据容器框架：提供 6 方向 NBT 骨架，由子类实现每个方向的序列化。
 * <p>
 * 此类管理 NBT 结构中 {@code "FaceStateData"} 根键下的 6 个方向子标签：
 * <pre>
 * {
 *   "FaceStateData": {
 *     "DOWN":  { ... },    // Direction.DOWN
 *     "UP":    { ... },    // Direction.UP
 *     "NORTH": { ... },    // Direction.NORTH
 *     "SOUTH": { ... },    // Direction.SOUTH
 *     "WEST":  { ... },    // Direction.WEST
 *     "EAST":  { ... }     // Direction.EAST
 *   }
 * }
 * </pre>
 * <p>
 * 子类通过实现 {@link #writeFace(CompoundTag, Direction)} 和
 * {@link #readFace(CompoundTag, Direction)} 决定每个方向子标签的具体数据结构。
 * 框架提供方向遍历和键名管理，子类负责字段内容的读写。
 * <p>
 * <b>使用方式</b>
 * <pre>{@code
 * // 自定义实现：每个方向存一个 int 值
 * public class HeatLevelContainer extends FaceStateContainer {
 *     public static final String HEAT_KEY = "Heat";
 *     private final int[] levels = new int[6];
 *
 *     &#064;Override
 *     protected void writeFace(CompoundTag faceTag, Direction face) {
 *         faceTag.putInt(HEAT_KEY, levels[face.get3DDataValue()]);
 *     }
 *
 *     &#064;Override
 *     protected void readFace(CompoundTag faceTag, Direction face) {
 *         levels[face.get3DDataValue()] = faceTag.getInt(HEAT_KEY);
 *     }
 * }
 *
 * // 使用默认的枚举实现
 * EnumFaceContainer<ShaftState> states = EnumFaceContainer.of(ShaftState.OFF);
 * }</pre>
 * <p>
 * <b>线程安全</b><br>
 * 此类不是线程安全的。每个 BlockEntity 应拥有自己的实例。
 */
public abstract class FaceStateContainer {

    /** NBT 中面状态数据的根键名。 */
    public static final String FACE_ROOT_KEY = "FaceStateData";

    /**
     * 方向键名常量，与 {@link Direction#get3DDataValue()} 的序数严格对齐。
     * <ul>
     *   <li>0 → {@code "DOWN"}  → {@link Direction#DOWN}</li>
     *   <li>1 → {@code "UP"}    → {@link Direction#UP}</li>
     *   <li>2 → {@code "NORTH"} → {@link Direction#NORTH}</li>
     *   <li>3 → {@code "SOUTH"} → {@link Direction#SOUTH}</li>
     *   <li>4 → {@code "WEST"}  → {@link Direction#WEST}</li>
     *   <li>5 → {@code "EAST"}  → {@link Direction#EAST}</li>
     * </ul>
     */
    protected static final String[] DIRECTION_KEYS = {"DOWN", "UP", "NORTH", "SOUTH", "WEST", "EAST"};

    /**
     * 将 6 个面的数据序列化为 NBT CompoundTag。
     * <p>
     * 遍历 6 个方向，为每个方向创建一个独立复合标签并委托给 {@link #writeFace}，
     * 由子类决定写入哪些字段。
     *
     * @return 包含 6 个面数据的 CompoundTag
     */
    public final CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        for (int i = 0; i < 6; i++) {
            CompoundTag faceTag = new CompoundTag();
            writeFace(faceTag, Direction.from3DDataValue(i));
            nbt.put(DIRECTION_KEYS[i], faceTag);
        }
        return nbt;
    }

    /**
     * 从 NBT 反序列化恢复 6 个面的数据。
     * <p>
     * 遍历 6 个方向，读取对应的复合标签并委托给 {@link #readFace}。
     * 如果 NBT 中缺失某个方向键，传入空的 CompoundTag 由子类自行处理默认值。
     *
     * @param nbt 包含面数据的 NBT
     */
    public final void fromNbt(CompoundTag nbt) {
        for (int i = 0; i < 6; i++) {
            String key = DIRECTION_KEYS[i];
            CompoundTag faceTag = nbt.contains(key) ? nbt.getCompound(key) : new CompoundTag();
            readFace(faceTag, Direction.from3DDataValue(i));
        }
    }

    /**
     * 序列化指定面的数据到复合标签。
     * <p>
     * 子类在此方法中将该面的状态数据写入 {@code faceTag}。
     * 写入的字段在 {@link #readFace} 中对应读取。
     *
     * @param faceTag 该方向的 NBT 复合标签（由框架创建，写入后自动放入根 NBT）
     * @param face    当前处理的面方向
     */
    protected abstract void writeFace(CompoundTag faceTag, Direction face);

    /**
     * 反序列化指定面的数据从复合标签。
     * <p>
     * 子类在此方法中从 {@code faceTag} 读取字段并恢复内部状态。
     * 如果 {@code faceTag} 为空（NBT 中缺失该方向键），
     * 子类应自行填入默认值。
     *
     * @param faceTag 该方向的 NBT 复合标签（可能为空，由子类处理）
     * @param face    当前处理的面方向
     */
    protected abstract void readFace(CompoundTag faceTag, Direction face);

    /**
     * 蓝图变换：按重映射索引重新排列 6 个面的数据。
     * <p>
     * 子类在此方法中将所有数据数组按 {@code remap} 重排。
     * 例如旋转 90° 时，NORTH 的数据迁到 EAST，EAST 到 SOUTH 等。
     * <p>
     * remap 数组长度必须为 6，索引 i 表示新位置 Direction.from3DDataValue(i)，
     * 值 remap[i] 表示该位置应从旧方向 Direction.from3DDataValue(remap[i]) 拷贝数据。
     *
     * @param remap 重映射索引数组
     * @throws IllegalArgumentException 如果 remap 为 null 或长度 != 6
     */
    public abstract void transform(int[] remap);

    // ===== 通用枚举工具 =====

    /**
     * 将字符串名称解析为枚举常量。
     * <p>
     * 遍历枚举类的所有常量，对比 {@link StringRepresentable#getSerializedName()}。
     * 如果传入的名称不匹配任何枚举常量（如存档损坏或 Mod 升级后枚举值被移除），
     * 返回 {@code fallback} 而非抛异常，确保容错。
     *
     * @param <T>      枚举类型
     * @param name     枚举的序列化名称（与 getSerializedName() 比较）
     * @param fallback 无法匹配时返回的默认值
     * @param constants 枚举常量数组（通常来自 {@code EnumClass.getEnumConstants()}）
     * @return 匹配的枚举常量，无匹配时返回 fallback
     */
    protected static <T extends Enum<T> & StringRepresentable> T enumValueOf(String name, T fallback, T[] constants) {
        Objects.requireNonNull(constants, "constants must not be null");
        for (T constant : constants) {
            if (constant.getSerializedName().equals(name)) {
                return constant;
            }
        }
        return fallback;
    }
}
