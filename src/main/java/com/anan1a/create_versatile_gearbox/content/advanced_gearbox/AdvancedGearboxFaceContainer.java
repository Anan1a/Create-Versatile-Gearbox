package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import com.anan1a.create_versatile_gearbox.foundation.container.EnumFaceContainer;
import com.anan1a.create_versatile_gearbox.foundation.container.FieldSlot;
import com.anan1a.create_versatile_gearbox.foundation.container.serializer.FloatSerializer;
import com.anan1a.create_versatile_gearbox.foundation.container.serializer.IntSerializer;

import net.minecraft.core.Direction;

/**
 * 高级齿轮箱面数据容器：在 {@link EnumFaceContainer} 的枚举状态基础上，
 * 通过继承的 {@link #add} 方法扩展存储每面的转速值（int）和倍率值（float）。
 * <p>
 * NBT 结构：
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
 * 所有字段由父类的内部 {@code CompositeFaceContainer} 统一管理序列化和变换。
 */
public class AdvancedGearboxFaceContainer extends EnumFaceContainer<AdvancedGearboxShaftState> {

    /** NBT 键名：每面转速值（int）。 */
    public static final String SPEED_VALUE_KEY = "SpeedValue";
    /** 转速默认值。 */
    private static final int DEFAULT_SPEED_VALUE = 16;
    /** 转速字段槽。 */
    private final FieldSlot<Integer> speedValueSlot;

    /** NBT 键名：每面倍率值（float）。 */
    public static final String MULTIPLIER_KEY = "Multiplier";
    /** 倍率默认值。 */
    private static final float DEFAULT_MULTIPLIER = 1.0f;
    /** 倍率字段槽。 */
    private final FieldSlot<Float> multiplierSlot;

    /**
     * 构造全默认值的容器。
     * <p>
     * 所有面的状态初始化为 {@link AdvancedGearboxBlock#DEFAULT_SHAFT_STATE}，
     * SpeedValue = 16，Multiplier = 1.0。
     */
    public AdvancedGearboxFaceContainer() {
        this(AdvancedGearboxBlock.DEFAULT_SHAFT_STATE);
    }

    /**
     * 构造全默认值的容器（指定枚举默认值）。
     *
     * @param defaultState 默认面状态
     */
    public AdvancedGearboxFaceContainer(AdvancedGearboxShaftState defaultState) {
        super(defaultState);
        this.speedValueSlot = add(SPEED_VALUE_KEY, new IntSerializer(DEFAULT_SPEED_VALUE));
        this.multiplierSlot = add(MULTIPLIER_KEY, new FloatSerializer(DEFAULT_MULTIPLIER));
    }

    // ===== SpeedValue 访问 =====

    /**
     * 获取指定面的转速值。
     */
    public int getSpeedValue(Direction face) {
        return speedValueSlot.get(face);
    }

    /**
     * 设置指定面的转速值。
     *
     * @return 自身
     */
    public AdvancedGearboxFaceContainer setSpeedValue(Direction face, int value) {
        speedValueSlot.set(face, value);
        return this;
    }

    // ===== Multiplier 访问 =====

    /**
     * 获取指定面的倍率值。
     */
    public float getMultiplier(Direction face) {
        return multiplierSlot.get(face);
    }

    /**
     * 设置指定面的倍率值。
     *
     * @return 自身
     */
    public AdvancedGearboxFaceContainer setMultiplier(Direction face, float value) {
        multiplierSlot.set(face, value);
        return this;
    }
}
