package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.option.RotationModeBehaviour;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.option.OperationModeBehaviour;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.value.CountBehaviour;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

/**
 * 高级齿轮箱的配置行为管理器。
 * <p>
 * 每面注册两个滑条：
 * <ul>
 *   <li>转速值 — 双排 CCW/CW，独立配置</li>
 *   <li>倍率值 — 双排 ÷/×，离散选择 2^0~2^8 或 1/2^1~1/2^8</li>
 * </ul>
 * 每个滑条绑定该面的交互框，只在对应面激活且 ShaftState=CFG 时显示。
 * <p>
 * 值变更时通过 callback 写入 {@link AdvancedGearboxFaceContainer}，
 * NBT 持久化由 FaceContainer 统一管理，滑条不参与序列化。
 */
public class AdvancedGearboxConfigBehaviours {

    private final AdvancedGearboxFaceContainer faceData;
    private final CountBehaviour[] settingValueBehaviours;
    private final RotationModeBehaviour[] rotationModeBehaviours;
    private final OperationModeBehaviour[] operationModeBehaviours;

    /**
     * 构造 12 个滑条（6 速度 + 6 倍率）并注册到 behaviours 列表。
     *
     * @param be       所属 BlockEntity
     * @param faceData 面数据容器（NBT 由它统一管理）
     * @param list     behaviour 列表（从 addBehaviours 传入）
     */
    public AdvancedGearboxConfigBehaviours(AdvancedGearboxBlockEntity be,
                                           AdvancedGearboxFaceContainer faceData,
                                           List<BlockEntityBehaviour> list) {
        this.faceData = faceData;
        this.settingValueBehaviours = new CountBehaviour[6];
        this.rotationModeBehaviours = new RotationModeBehaviour[6];
        this.operationModeBehaviours = new OperationModeBehaviour[6];

        int maxRotationValue = AllConfigs.server().kinetics.maxRotationSpeed.get();

        int netId = 0;
        for (Direction dir : Direction.values()) {
            int i = dir.get3DDataValue();

            Supplier<Boolean> isCfg = () -> be.getShaftState(dir) == AdvancedGearboxShaftState.FWD
                                      && (!be.hasSource() || be.getSourceFacing() != dir);
            Supplier<Boolean> isFwd = () -> be.getShaftState(dir) == AdvancedGearboxShaftState.FWD;

            // ---- 值滑条 ----
            CountBehaviour settingValue = new CountBehaviour(
                    Component.translatable("gui.advanced_gearbox.face_value", dir.getName()),
                    be,
                    new FaceValueBoxTransform(dir, 12, 12, 4, -1, isCfg),
                    netId++, dir.getName(),
                    maxRotationValue,
                    32,
                    List.of(Component.literal("V").withStyle(ChatFormatting.BOLD))
            );
            settingValue.withCallback(val -> {
                faceData.setSettingValue(dir, val);
                be.repropagateKinetics();
            });
            // 设初始值使其与 faceData 默认值一致
            settingValue.setRawValue(faceData.getSettingValue(dir));
            settingValueBehaviours[i] = settingValue;
            list.add(settingValue);

            // ---- 旋转模式滑条 ----
            RotationModeBehaviour rotationMode = new RotationModeBehaviour(
                    Component.translatable("gui.advanced_gearbox.face_rotation_mode", dir.getName()),
                    be,
                    new FaceValueBoxTransform(dir, 8, 12, 4, -1, isFwd),
                    netId++, dir.getName()
            );
            rotationMode.withCallback(val -> {
                faceData.setRotationMode(dir, val);
                be.repropagateKinetics();
            });
            // 设初始值使其与 faceData 默认值一致
            rotationMode.setRawValue(faceData.getRotationMode(dir));
            rotationModeBehaviours[i] = rotationMode;
            list.add(rotationMode);

            // ---- 倍率模式滑条 ----
            OperationModeBehaviour operationMode = new OperationModeBehaviour(
                    Component.translatable("gui.advanced_gearbox.face_operation_mode", dir.getName()),
                    be,
                    new FaceValueBoxTransform(dir, 4, 12, 4, -1, isCfg),
                    netId++, dir.getName()
            );
            operationMode.withCallback(val -> {
                faceData.setOperationMode(dir, val);
                be.repropagateKinetics();
            });
            // 设初始值使其与 faceData 默认值一致
            operationMode.setRawValue(faceData.getOperationMode(dir));
            operationModeBehaviours[i] = operationMode;
            list.add(operationMode);
        }
    }

    /**
     * 从 faceData 将各面数据同步到滑条的 value 字段。
     * <p>
     * 在 BlockEntity.read() 中 faceData.readFromRoot() 之后调用。
     */
    public void syncFromFaceData() {
        for (Direction dir : Direction.values()) {
            int i = dir.get3DDataValue();
            settingValueBehaviours[i].setRawValue(faceData.getSettingValue(dir));
            rotationModeBehaviours[i].setRawValue(faceData.getRotationMode(dir));
            operationModeBehaviours[i].setRawValue(faceData.getOperationMode(dir));
        }
    }
}
