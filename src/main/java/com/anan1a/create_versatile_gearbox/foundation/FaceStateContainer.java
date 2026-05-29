package com.anan1a.create_versatile_gearbox.foundation;

import java.util.Arrays;
import java.util.Objects;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;

/**
 * 面状态数据容器：以 {@link Direction} 为索引，存储 6 个面的可枚举状态，
 * 并提供统一的 NBT 序列化/反序列化、运行时面访问、以及 ModelData 导出支持。
 * <p>
 * <b>设计动机</b><br>
 * Minecraft 原版 BlockState 使用 {@link net.minecraft.world.level.block.state.properties.EnumProperty}，
 * 每个属性增加 2× 变体数，6 个面 × 3 状态 = 729 种组合，枚举类每增加一个值就指数膨胀。
 * 此容器改用 NBT 存储状态，将 6 个面的数据压缩到一个 CompoundTag 中：
 * <ul>
 *   <li>突破 BlockState 枚举数量限制，支持每个面独立扩展更多变种</li>
 *   <li>运行时以内存中的 T[] 缓存，面访问是 O(1) 数组索引，与 BlockState 一样高效</li>
 *   <li>序列化时自动将枚举转换为 {@link StringRepresentable#getSerializedName()} 字符串</li>
 * </ul>
 * <p>
 * <b>数据流三阶段</b>
 * <ol>
 *   <li><b>持久化 → 运行时</b>：{@link #read(CompoundTag, HolderLookup.Provider, boolean)} 从 NBT 加载 → {@link #fromNbt(CompoundTag)}</li>
 *   <li><b>运行时操作</b>：{@link #get(Direction)} / {@link #set(Direction, Object)} 直接操作内存数组</li>
 *   <li><b>运行时 → 渲染</b>：{@link #toArray()} → {@link net.neoforged.neoforge.client.model.data.ModelData} → {@link AdvancedGearboxModel#resolveState}</li>
 * </ol>
 * <p>
 * <b>使用方式</b>（类似 BlockState 的面访问体验）：
 * <pre>{@code
 * // 新建（所有面初始为 OFF）
 * FaceStateContainer<ShaftState> states = FaceStateContainer.of(ShaftState.OFF);
 *
 * // 面访问
 * ShaftState s = states.get(face);
 * states.set(face, ShaftState.FWD);
 *
 * // NBT 序列化
 * tag.put("face_states", states.toNbt());
 * states.fromNbt(tag.getCompound("face_states"));
 *
 * // 导出到 ModelData（渲染管线）
 * return ModelData.builder()
 *         .with(FACE_STATES_MODEL_PROPERTY, states.toArray())
 *         .build();
 * }</pre>
 * <p>
 * <b>NBT 存储格式</b><br>
 * 使用方向名作为键名（可读性优先于紧凑性）：
 * <pre>
 * {
 *   "down":  "fwd",    // Direction.DOWN
 *   "up":    "rev",    // Direction.UP
 *   "north": "fwd",    // Direction.NORTH
 *   "south": "off",    // Direction.SOUTH
 *   "west":  "off",    // Direction.WEST
 *   "east":  "rev"     // Direction.EAST
 * }
 * </pre>
 * <p>
 * 键名 {@code "down"/"up"/"north"/"south"/"west"/"east"} 与 {@link Direction#getName()} 一致，
 * 值使用枚举常量的 {@link StringRepresentable#getSerializedName()}。
 * 缺失的键会被 {@link #defaultValue} 填充，确保 NBT 部分写入时不会读到 null。
 * <p>
 * <b>线程安全</b><br>
 * 此类不是线程安全的。每个 {@link net.minecraft.world.level.block.entity.BlockEntity}
 * 应拥有自己的实例，BlockEntity 的读写在主线程或工作线程按逻辑串行访问。
 *
 * @param <T> 面状态枚举类型，必须同时继承 {@link Enum} 并实现 {@link StringRepresentable}
 */
public final class FaceStateContainer<T extends Enum<T> & StringRepresentable> {

    /** 六个面状态值的运行时缓存数组，索引与 {@link Direction#get3DDataValue()} 一致。 */
    private final T[] states;

    /**
     * 方向键名常量，与 {@link Direction#get3DDataValue()} 的序数严格对齐。
     * <ul>
     *   <li>0 → {@code "down"}  → {@link Direction#DOWN}</li>
     *   <li>1 → {@code "up"}    → {@link Direction#UP}</li>
     *   <li>2 → {@code "north"} → {@link Direction#NORTH}</li>
     *   <li>3 → {@code "south"} → {@link Direction#SOUTH}</li>
     *   <li>4 → {@code "west"}  → {@link Direction#WEST}</li>
     *   <li>5 → {@code "east"}  → {@link Direction#EAST}</li>
     * </ul>
     * 使用字符串而非数字的原因：NBT 作为人类可读的层级格式，方向名比 {@code "0"}~{@code "5"}
     * 更直观，调试时可直接看出哪个面的数据异常。
     */
    private static final String[] DIRECTION_KEYS = {"down", "up", "north", "south", "west", "east"};

    /**
     * 默认状态值。
     * <ul>
     *   <li>新创建的容器，6 个面全部填充此值</li>
     *   <li>反序列化时 NBT 中缺失的键用此值填充</li>
     *   <li>{@link #isDefault(Direction)} 和 {@link #allDefault()} 以此值为准比较</li>
     * </ul>
     */
    private final T defaultValue;

    /**
     * 构造函数：创建全默认值的容器。
     * <p>
     * 内部通过 {@link java.lang.reflect.Array#newInstance(Class, int)} 创建泛型 T[]，
     * 因为 Java 不允许直接 {@code new T[6]}（泛型数组创建在运行时类型擦除后不合法）。
     * 反射方式能保留运行时枚举类型，确保数组元素类型安全。
     *
     * @param defaultValue 所有面的初始状态
     */
    @SuppressWarnings("unchecked")
    private FaceStateContainer(T defaultValue) {
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        // 通过反射创建泛型数组：T[] 在运行时被擦除为 Enum，反射能拿到具体枚举类
        // Array.newInstance(enumClass, 6) 等价于 new ShaftState[6]
        Class<T> enumClass = defaultValue.getDeclaringClass();
        this.states = (T[]) java.lang.reflect.Array.newInstance(enumClass, 6);
        fill(defaultValue);
    }

    /**
     * 构造函数：从已有数组创建容器。
     * <p>
     * 将传入数组的前 6 个元素复制到内部 states[]，不足 6 个或中间有 null 时用 defaultValue 填充。
     * 此构造函数主要用于 {@link #fromArray(Object, Enum[])} 工厂方法，
     * 或在需要从外部数据源（如其他 Mod 的 API 返回值）初始化时使用。
     *
     * @param defaultValue 默认值，用于填充 null 或越界位置
     * @param array        源数据数组（最多取前 6 个元素）
     */
    @SuppressWarnings("unchecked")
    private FaceStateContainer(T defaultValue, T[] array) {
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        Class<T> enumClass = defaultValue.getDeclaringClass();
        this.states = (T[]) java.lang.reflect.Array.newInstance(enumClass, 6);
        // 复制前 6 个元素，避免源数组越界
        System.arraycopy(array, 0, this.states, 0, Math.min(array.length, 6));
        // null 安全填充：复制后仍有 null 的位置填入默认值
        for (int i = 0; i < 6; i++) {
            if (this.states[i] == null) {
                this.states[i] = defaultValue;
            }
        }
    }

    /**
     * 创建新容器，所有面初始化为指定默认值。
     * <p>
     * 最常用的工厂方法，适用于在 BlockEntity 字段初始化时使用：
     * <pre>{@code
     * private final FaceStateContainer<ShaftState> faceStates =
     *     FaceStateContainer.of(ShaftState.OFF);
     * }</pre>
     *
     * @param defaultValue 默认状态值
     * @param <T>          面状态枚举类型
     * @return 新的容器，states = [defaultValue, defaultValue, ..., defaultValue]
     */
    public static <T extends Enum<T> & StringRepresentable> FaceStateContainer<T> of(T defaultValue) {
        return new FaceStateContainer<>(defaultValue);
    }

    /**
     * 从现有数组创建容器。
     * <p>
     * 与 {@link #of(Object)} 的区别：此方法接受一个已有数组，将其内容复制到内部 states[]。
     * 适用于从外部数据源（如另一个容器的 {@link #toArray()} 返回值、其他 Mod 的 API 返回的数组等）初始化。
     *
     * @param defaultValue 默认值（用于填充数组不足 6 个或含 null 的位置）
     * @param states       源数据数组（最多取前 6 个元素）
     * @param <T>          面状态枚举类型
     * @return 新的容器
     */
    public static <T extends Enum<T> & StringRepresentable> FaceStateContainer<T> fromArray(T defaultValue, T[] states) {
        return new FaceStateContainer<>(defaultValue, states);
    }

    /**
     * 从 NBT 反序列化创建容器。
     * <p>
     * 静态工厂版，适合直接从 NBT 一步创建：
     * <pre>{@code
     * FaceStateContainer<ShaftState> states =
     *     FaceStateContainer.fromNbt(tag.getCompound("face_states"), ShaftState.OFF);
     * }</pre>
     * <p>
     * 内部先创建全默认容器，再调用 {@link #fromNbt(CompoundTag)} 覆写已有的值，
     * NBT 中缺失的键保持 defaultValue。
     *
     * @param tag          NBT 数据（六个方向名键）
     * @param defaultValue 默认值（缺失键时使用）
     * @param <T>          面状态枚举类型
     * @return 反序列化后的容器
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & StringRepresentable> FaceStateContainer<T> fromNbt(CompoundTag tag, T defaultValue) {
        FaceStateContainer<T> container = new FaceStateContainer<>(defaultValue);
        container.fromNbt(tag);
        return container;
    }

    // ===== 面访问（类似 BlockState 的 getValue/setValue 体验） =====

    /**
     * 获取指定面的当前状态。
     * <p>
     * 时间复杂度 O(1)，直接数组索引：{@code states[face.get3DDataValue()]}。
     * 与 BlockState 的 {@code state.getValue(PROPERTY)} 性能等价。
     *
     * @param face 要查询的面方向
     * @return 该面的状态值
     */
    public T get(Direction face) {
        return states[face.get3DDataValue()];
    }

    /**
     * 设置指定面的状态。
     * <p>
     * 修改的是运行时内存缓存，不影响 NBT。
     * 调用者需自行触发 {@code setChanged()} 和 {@code requestModelDataUpdate()} 以持久化和同步渲染。
     *
     * @param face  要修改的面方向
     * @param value 新的状态值（不可为 null）
     * @return 自身（链式调用支持）
     */
    public FaceStateContainer<T> set(Direction face, T value) {
        states[face.get3DDataValue()] = Objects.requireNonNull(value, "value must not be null");
        return this;
    }

    /**
     * 将所有面的状态设为同一个值。
     * <p>
     * 相当于对 6 个面依次调用 {@link #set(Direction, Object)}。
     * 适用于重置场景：比如扳手 Shift+右键快速拆除时将 6 个面全部设为 OFF。
     *
     * @param value 要填充的状态值
     * @return 自身
     */
    public FaceStateContainer<T> fill(T value) {
        Arrays.fill(states, Objects.requireNonNull(value, "value must not be null"));
        return this;
    }

    // ===== 便捷查询 =====

    /**
     * 检查指定面的状态是否等于默认值。
     * <p>
     * 适用于快速判断某个面是否需要特殊处理。
     * 例如渲染时可以跳过默认状态面的纹理替换。
     *
     * @param face 要检查的面
     * @return true 如果该面状态 == defaultValue
     */
    public boolean isDefault(Direction face) {
        return states[face.get3DDataValue()] == defaultValue;
    }

    /**
     * 检查所有 6 个面的状态是否都是默认值。
     * <p>
     * 适用于判断整个方块是否需要特殊处理。
     * 如果是全默认状态，可以跳过一些不必要的计算。
     *
     * @return true 如果所有面都是默认值
     */
    public boolean allDefault() {
        for (T s : states) {
            if (s != defaultValue) return false;
        }
        return true;
    }

    // ===== 数据导出 =====

    /**
     * 返回内部状态数组的副本。
     * <p>
     * 主要用于导出到 {@link net.neoforged.neoforge.client.model.data.ModelData}：
     * <pre>{@code
     * return ModelData.builder()
     *         .with(SomeModelProperty.INSTANCE, faceStates.toArray())
     *         .build();
     * }</pre>
     * <p>
     * 返回 {@code clone()} 而非直接返回内部引用，防止外部代码意外修改内部状态。
     * 渲染管线只需要读取数据，不应允许外部通过此数组修改状态。
     *
     * @return 6 个面状态的数组副本（D/U/N/S/W/E 顺序）
     */
    public T[] toArray() {
        return states.clone();
    }

    // ===== NBT 序列化 =====

    /**
     * 将 6 个面的状态序列化为 NBT CompoundTag。
     * <p>
     * <b>输出格式</b>
     * <pre>
     * {
     *   "down":  "fwd",   // Direction.DOWN  -> getSerializedName()
     *   "up":    "rev",   // Direction.UP    -> getSerializedName()
     *   "north": "fwd",   // Direction.NORTH -> getSerializedName()
     *   "south": "off",   // Direction.SOUTH -> getSerializedName()
     *   "west":  "off",   // Direction.WEST  -> getSerializedName()
     *   "east":  "rev"    // Direction.EAST  -> getSerializedName()
     * }
     * </pre>
     * <p>
     * 值使用 {@link StringRepresentable#getSerializedName()}，这是 Minecraft 枚举
     * 序列化的标准方式（如 {@link net.minecraft.world.level.block.state.properties.BlockStateProperties} 的做法）。
     * <p>
     * <b>null 安全</b>：虽然内部 states[] 理论上不应有空元素，但若因 bug 出现 null，
     * 会使用 defaultValue 的序列化名作为保底，避免 NBT 写入 null 导致存档损坏。
     *
     * @return 包含 6 个面状态的 CompoundTag
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < 6; i++) {
            tag.putString(DIRECTION_KEYS[i],
                states[i] != null ? states[i].getSerializedName() : defaultValue.getSerializedName());
        }
        return tag;
    }

    /**
     * 从 NBT CompoundTag 反序列化恢复 6 个面的状态。
     * <p>
     * <b>反序列化策略</b>
     * <ol>
     *   <li>按 D/U/N/S/W/E 顺序遍历 6 个方向键</li>
     *   <li>如果 NBT 中包含该键，通过 {@link #enumValueOf(String)} 将字符串转换回枚举</li>
     *   <li>如果 NBT 中缺失该键（部分写入或旧格式），用 {@link #defaultValue} 填充</li>
     * </ol>
     * <p>
     * 此方法覆写的是容器已有的值。如果只想读取而不修改已有容器，请使用
     * 静态工厂 {@link #fromNbt(CompoundTag, Object)} 创建新容器。
     *
     * @param tag 包含面状态的 NBT
     */
    public void fromNbt(CompoundTag tag) {
        for (int i = 0; i < 6; i++) {
            String key = DIRECTION_KEYS[i];
            if (tag.contains(key)) {
                // tag.getString(key) 返回字符串 → 通过 enumValueOf 转换回枚举
                states[i] = enumValueOf(tag.getString(key));
            } else {
                // NBT 中没有此方向的数据（可能是新初始化的 BlockEntity）
                states[i] = defaultValue;
            }
        }
    }

    // ===== 内部工具 =====

    /**
     * 将字符串名称解析为枚举常量。
     * <p>
     * 遍历枚举类的所有常量，对比 {@link StringRepresentable#getSerializedName()}。
     * 使用 getSerializedName() 而非 {@link Enum#name()}，是因为 Minecraft 的序列化
     * 规范要求使用小写名称（如 {@code "fwd"} 而非 {@code "FWD"}）。
     * <p>
     * 如果传入的名称不匹配任何枚举常量（如存档损坏或 Mod 升级后枚举值被移除），
     * 返回 defaultValue 而非抛异常，确保容错。
     *
     * @param name 枚举的序列化名称（与 getSerializedName() 比较）
     * @return 匹配的枚举常量，无匹配时返回 defaultValue
     */
    @SuppressWarnings("unchecked")
    private T enumValueOf(String name) {
        Class<T> enumClass = (Class<T>) defaultValue.getClass();
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.getSerializedName().equals(name)) {
                return constant;
            }
        }
        // 容错：不可识别的名称用默认值代替，避免存档损坏导致崩溃
        return defaultValue;
    }

    // ===== 对象方法 =====

    /**
     * 比较两个 FaceStateContainer 是否持有完全相同的 6 个面状态。
     * <p>
     * 两个容器只要内部 states[] 数组逐元素相等即视为相等，不要求是同一个对象。
     * 这使 FaceStateContainer 可以用作 {@link net.neoforged.neoforge.client.model.data.ModelData}
     * 的更新判定依据：如果新旧容器的 states[] 内容一致，则不触发模型重载。
     *
     * @param obj 要比较的对象
     * @return true 如果 obj 也是 FaceStateContainer 且 6 个面状态全部相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FaceStateContainer<?> other)) return false;
        return Arrays.equals(this.states, other.states);
    }

    /**
     * 哈希码基于内部 states[] 数组计算。
     * <p>
     * 与 {@link #equals(Object)} 一致，使用 {@link Arrays#hashCode(Object[])}。
     * 用于 {@link net.neoforged.neoforge.client.model.data.ModelData} 的 HashMap 查找。
     *
     * @return states[] 的哈希码
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(states);
    }

    /**
     * 调试用字符串表示。
     * <p>
     * 输出示例：{@code FaceStateContainer{DOWN=fwd, UP=rev, NORTH=fwd, SOUTH=off, WEST=off, EAST=rev}}
     * <br>
     * 使用方向名前缀，便于在日志中快速定位哪个面的数据异常。
     *
     * @return 人类可读的 6 个面状态内容
     */
    @Override
    public String toString() {
        return "FaceStateContainer{" +
            "DOWN=" + get(Direction.DOWN) +
            ", UP=" + get(Direction.UP) +
            ", NORTH=" + get(Direction.NORTH) +
            ", SOUTH=" + get(Direction.SOUTH) +
            ", WEST=" + get(Direction.WEST) +
            ", EAST=" + get(Direction.EAST) +
            '}';
    }
}
