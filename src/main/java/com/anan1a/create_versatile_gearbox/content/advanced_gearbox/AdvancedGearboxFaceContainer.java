package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.Arrays;

import com.anan1a.create_versatile_gearbox.foundation.container.EnumFaceContainer;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * 高级齿轮箱面数据容器：在 {@link EnumFaceContainer} 的枚举状态基础上，
 * 扩展存储每面的转速值（int）和倍率值（float）。
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
 * 序列化委托给父类处理 {@code FaceState}（枚举），
 * 自身处理 {@code SpeedValue}（整数转速）和 {@code Multiplier}（浮点倍率）。
 */
public class AdvancedGearboxFaceContainer
        extends EnumFaceContainer<AdvancedGearboxShaftState> {

    /** NBT 键名：每面转速值（int）。 */
    public static final String SPEED_VALUE_KEY = "SpeedValue";

    /** NBT 键名：每面倍率值（float）。 */
    public static final String MULTIPLIER_KEY = "Multiplier";

    /** 转速默认值。 */
    private static final int DEFAULT_SPEED_VALUE = 16;

    /** 倍率默认值。 */
    private static final float DEFAULT_MULTIPLIER = 1.0f;

    /** 六面转速值，索引 = Direction.get3DDataValue()。 */
    private final int[] speedValues = new int[6];

    /** 六面倍率值，索引 = Direction.get3DDataValue()。 */
    private final float[] multipliers = new float[6];

    /**
     * 构造全默认值的容器。
     * <p>
     * 所有面的状态初始化为 {@link AdvancedGearboxBlock#DEFAULT_SHAFT_STATE}，
     * SpeedValue = 0，Multiplier = 1.0。
     */
    public AdvancedGearboxFaceContainer() {
        super(AdvancedGearboxBlock.DEFAULT_SHAFT_STATE);
        fillSpeedMultiplier(DEFAULT_SPEED_VALUE, DEFAULT_MULTIPLIER);
    }

    /**
     * 构造全默认值的容器（指定枚举默认值）。
     *
     * @param defaultState 默认面状态
     */
    public AdvancedGearboxFaceContainer(AdvancedGearboxShaftState defaultState) {
        super(defaultState);
        fillSpeedMultiplier(DEFAULT_SPEED_VALUE, DEFAULT_MULTIPLIER);
    }

    // ===== 写入/读取扩展数据 =====

    @Override
    protected void writeFace(CompoundTag faceTag, Direction face) {
        // 父类写入 FaceState 枚举值
        super.writeFace(faceTag, face);
        // 本类写入扩展字段
        int idx = face.get3DDataValue();
        faceTag.putInt(SPEED_VALUE_KEY, speedValues[idx]);
        faceTag.putFloat(MULTIPLIER_KEY, multipliers[idx]);
    }

    @Override
    protected void readFace(CompoundTag faceTag, Direction face) {
        // 父类读取 FaceState 枚举值
        super.readFace(faceTag, face);
        // 本类读取扩展字段
        int idx = face.get3DDataValue();
        speedValues[idx] = faceTag.contains(SPEED_VALUE_KEY)
                ? faceTag.getInt(SPEED_VALUE_KEY)
                : DEFAULT_SPEED_VALUE;
        multipliers[idx] = faceTag.contains(MULTIPLIER_KEY)
                ? faceTag.getFloat(MULTIPLIER_KEY)
                : DEFAULT_MULTIPLIER;
    }

    // ===== SpeedValue 访问 =====

    /**
     * 获取指定面的转速值。
     *
     * @param face 要查询的面方向
     * @return 转速值
     */
    public int getSpeedValue(Direction face) {
        return speedValues[face.get3DDataValue()];
    }

    /**
     * 设置指定面的转速值。
     *
     * @param face  要修改的面方向
     * @param value 新的转速值
     * @return 自身
     */
    public AdvancedGearboxFaceContainer setSpeedValue(Direction face, int value) {
        speedValues[face.get3DDataValue()] = value;
        return this;
    }

    // ===== Multiplier 访问 =====

    /**
     * 获取指定面的倍率值。
     *
     * @param face 要查询的面方向
     * @return 倍率值
     */
    public float getMultiplier(Direction face) {
        return multipliers[face.get3DDataValue()];
    }

    /**
     * 设置指定面的倍率值。
     *
     * @param face  要修改的面方向
     * @param value 新的倍率值
     * @return 自身
     */
    public AdvancedGearboxFaceContainer setMultiplier(Direction face, float value) {
        multipliers[face.get3DDataValue()] = value;
        return this;
    }

    // ===== 批量操作 =====

    /**
     * 将所有面的 SpeedValue 和 Multiplier 设为相同值。
     *
     * @param speedValue 转速值
     * @param multiplier 倍率值
     * @return 自身
     */
    public AdvancedGearboxFaceContainer fillSpeedMultiplier(int speedValue, float multiplier) {
        Arrays.fill(speedValues, speedValue);
        Arrays.fill(multipliers, multiplier);
        return this;
    }

    // ===== 变换支持（旋转/镜像时联动旋转扩展数据）=====

    /**
     * 蓝图变换：先让父类重排枚举状态，再重排本类的 speedValues 和 multipliers。
     */
    @Override
    public void transform(int[] remap) {
        // 快照本类的扩展数据（父类 transform 会改 states，但不会改这里的数组）
        int[] oldSpeed = speedValues.clone();
        float[] oldMultipliers = multipliers.clone();

        // 让父类重排枚举状态
        super.transform(remap);

        // 重排本类新增的数据数组
        for (int newIdx = 0; newIdx < 6; newIdx++) {
            speedValues[newIdx] = oldSpeed[remap[newIdx]];
            multipliers[newIdx] = oldMultipliers[remap[newIdx]];
        }
    }

    /**
     * 创建当前容器的独立副本（深拷贝所有数据）。
     */
    @Override
    public AdvancedGearboxFaceContainer copy() {
        AdvancedGearboxFaceContainer clone = new AdvancedGearboxFaceContainer(
                getDefaultStateForCopy());
        // 手动拷贝所有 6 个面的数据
        for (Direction dir : Direction.values()) {
            int idx = dir.get3DDataValue();
            clone.set(dir, get(dir));
            clone.speedValues[idx] = speedValues[idx];
            clone.multipliers[idx] = multipliers[idx];
        }
        return clone;
    }

    /**
     * 获取父类中使用的默认状态值（用于 copy 构造）。
     * 通过读任意面在父类中的默认值来获取。
     */
    private AdvancedGearboxShaftState getDefaultStateForCopy() {
        // 从父类存储的默认值推断 — 由于父类字段 private，
        // 这里通过 Array.getLength 等反射方式不可取，
        // 直接使用 AdvancedGearboxBlock.DEFAULT_SHAFT_STATE
        return AdvancedGearboxBlock.DEFAULT_SHAFT_STATE;
    }
}
