package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import com.anan1a.create_versatile_gearbox.foundation.container.EnumFaceContainer;
import com.anan1a.create_versatile_gearbox.foundation.container.FieldSlot;
import com.anan1a.create_versatile_gearbox.foundation.container.serializer.IntSerializer;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.option.RotationModeBehaviour.RotationMode;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.option.OperationModeBehaviour.OperationMode;

import net.minecraft.core.Direction;

/**
 * 高级齿轮箱面数据容器：在 {@link EnumFaceContainer} 的枚举状态基础上，
 * 通过继承的 {@link #add} 方法扩展存储每面的转速值（int）和倍率值（float）。
 * <p>
 * NBT 结构：
 * <pre>
 * {
 *   "FaceStateData": {
 *     "DOWN":  { "FaceState": "fwd", "ArgValue": 5, "RotationMode": 0, "OpMode": 5},
 *     "UP":    { "FaceState": "rev", "ArgValue": 0, "RotationMode": 0, "OpMode": 0},
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
    public static final String ROT_MODE_KEY = "RotationMode";
    /** 选项模式序数字段槽。 */
    private final FieldSlot<Integer> rotModeSlot;

    /** NBT 键名：每面操作模式序数。 */
    public static final String OP_MODE_KEY = "OperationMode";
    /** 操作模式序数字段槽。 */
    private final FieldSlot<Integer> opModeSlot;

    /**
     * 构造全默认值的容器。
     * <p>
     * 所有面的状态初始化为 {@link AdvancedGearboxBlock#DEFAULT_SHAFT_STATE}，
     * ArgValue = 16，Multiplier = 1.0。
     */
    public AdvancedGearboxFaceContainer() {
        this(AdvancedGearboxBlock.DEFAULT_SHAFT_STATE,
            16,
            0,
            0);
    }

    /**
     * 构造全默认值的容器（指定枚举默认值）。
     *
     * @param defaultState 默认面状态
     */
    public AdvancedGearboxFaceContainer(AdvancedGearboxShaftState defaultState,
                                        int defaultArgValue,
                                        int defaultRotMode,
                                        int defaultOpMode) {
        super(defaultState);
        this.ArgValueSlot = add(SPEED_VALUE_KEY, new IntSerializer(defaultArgValue));
        this.rotModeSlot = add(ROT_MODE_KEY, new IntSerializer(defaultRotMode));
        this.opModeSlot = add(OP_MODE_KEY, new IntSerializer(defaultOpMode));
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

    // ===== RotationMode 访问 =====

    /**
     * 获取指定面的选项模式枚举（从序数转换）。
     */
    public RotationMode resolveRotMode(Direction face) {
        return RotationMode.values()[rotModeSlot.get(face)];
    }

    /**
     * 设置指定面的选项模式枚举。
     */
    public void setRotMode(Direction face, RotationMode rotationMode) {
        rotModeSlot.set(face, rotationMode.ordinal());
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

    // ===== OperationMode 访问 =====

    /**
     * 获取指定面的操作模式枚举（从序数转换）。
     */
    public OperationMode resolveOpMode(Direction face) {
        return OperationMode.values()[opModeSlot.get(face)];
    }

    /**
     * 设置指定面的操作模式枚举。
     */
    public void setOpMode(Direction face, OperationMode operationMode) {
        opModeSlot.set(face, operationMode.ordinal());
    }

    /**
     * 获取指定面的操作模式序数。
     */
    public int getOpMode(Direction face) {
        return opModeSlot.get(face);
    }

    /**
     * 设置指定面的操作模式序数。
     */
    public void setOpMode(Direction face, int value) {
        opModeSlot.set(face, value);
    }

}
