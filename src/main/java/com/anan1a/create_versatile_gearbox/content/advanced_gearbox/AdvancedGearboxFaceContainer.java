package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import com.anan1a.create_versatile_gearbox.foundation.container.EnumFaceContainer;
import com.anan1a.create_versatile_gearbox.foundation.container.FieldSlot;
import com.anan1a.create_versatile_gearbox.foundation.container.serializer.FloatSerializer;
import com.anan1a.create_versatile_gearbox.foundation.container.serializer.IntSerializer;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.option.RotModeBehaviour.Mode;

import net.minecraft.core.Direction;

/**
 * 高级齿轮箱面数据容器：在 {@link EnumFaceContainer} 的枚举状态基础上，
 * 通过继承的 {@link #add} 方法扩展存储每面的转速值（int）和倍率值（float）。
 * <p>
 * NBT 结构：
 * <pre>
 * {
 *   "FaceStateData": {
 *     "DOWN":  { "FaceState": "fwd", "ArgValue": 5, "Multiplier": 1.5 },
 *     "UP":    { "FaceState": "rev", "ArgValue": 0, "Multiplier": 1.0 },
 *     ...
 *   }
 * }
 * </pre>
 * <p>
 * 所有字段由父类的内部 {@code CompositeFaceContainer} 统一管理序列化和变换。
 */
public class AdvancedGearboxFaceContainer extends EnumFaceContainer<AdvancedGearboxShaftState> {

    /** NBT 键名：每面转速值（int）。 */
    public static final String SPEED_VALUE_KEY = "ArgValue";
    /** 转速字段槽。 */
    private final FieldSlot<Integer> ArgValueSlot;

    /** NBT 键名：每面选项模式序数。 */
    public static final String ROT_MODE_KEY = "RotMode";
    /** 选项模式序数字段槽。 */
    private final FieldSlot<Integer> rotModeSlot;

    /**
     * 构造全默认值的容器。
     * <p>
     * 所有面的状态初始化为 {@link AdvancedGearboxBlock#DEFAULT_SHAFT_STATE}，
     * ArgValue = 16，Multiplier = 1.0。
     */
    public AdvancedGearboxFaceContainer() {
        this(AdvancedGearboxBlock.DEFAULT_SHAFT_STATE,
            16,
            0);
    }

    /**
     * 构造全默认值的容器（指定枚举默认值）。
     *
     * @param defaultState 默认面状态
     */
    public AdvancedGearboxFaceContainer(AdvancedGearboxShaftState defaultState,
                                        int defaultArgValue,
                                        int defaultRotMode) {
        super(defaultState);
        this.ArgValueSlot = add(SPEED_VALUE_KEY, new IntSerializer(defaultArgValue));
        this.rotModeSlot = add(ROT_MODE_KEY, new IntSerializer(defaultRotMode));
    }

    // ===== ArgValue 访问 =====

    /**
     * 获取指定面的转速值。
     */
    public int getArgValue(Direction face) {
        return ArgValueSlot.get(face);
    }

    /**
     * 设置指定面的转速值。
     */
    public void setArgValue(Direction face, int value) {
        ArgValueSlot.set(face, value);
    }

    // ===== RotMode 访问 =====

    /**
     * 获取指定面的选项模式枚举（从序数转换）。
     */
    public Mode resolveRotMode(Direction face) {
        return Mode.values()[rotModeSlot.get(face)];
    }

    /**
     * 设置指定面的选项模式枚举。
     */
    public void setRotMode(Direction face, Mode mode) {
        rotModeSlot.set(face, mode.ordinal());
    }

    /**
     * 获取指定面的选项模式序数。
     */
    public int getRotMode(Direction face) {
        return rotModeSlot.get(face);
    }

    /**
     * 设置指定面的选项模式序数。
     */
    public void setRotMode(Direction face, int value) {
        rotModeSlot.set(face, value);
    }
}
