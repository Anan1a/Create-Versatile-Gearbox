package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.List;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.option.RotModeBehaviour;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.value.CountBehaviour;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;

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
    private final CountBehaviour[] countBehaviours;
    private final RotModeBehaviour[] rotModeBehaviours;

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
        this.countBehaviours = new CountBehaviour[6];
        this.rotModeBehaviours = new RotModeBehaviour[6];

        int maxRotationSpeed = AllConfigs.server().kinetics.maxRotationSpeed.get();

        int netId = 0;
        for (Direction dir : Direction.values()) {
            int i = dir.get3DDataValue();

            // ---- 速度值滑条（双排 CCW/CW，值域 ±maxRotationSpeed） ----
            CountBehaviour speed = new CountBehaviour(
                    Component.translatable("gui.advanced_gearbox.face_speed", dir.getName()),
                    be,
                    new FaceValueBoxTransform(dir, 4, 12, () -> be.getShaftState(dir) == AdvancedGearboxShaftState.CFG),
                    netId++, dir.getName(),
                    maxRotationSpeed
            );
            speed.withCallback(val -> faceData.setArgValue(dir, val));
            countBehaviours[i] = speed;
            list.add(speed);

            RotModeBehaviour rotationMode = new RotModeBehaviour(
                    Component.translatable("gui.advanced_gearbox.face_rotation_mode", dir.getName()),
                    be,
                    new FaceValueBoxTransform(dir, 4, 4, () -> be.getShaftState(dir) == AdvancedGearboxShaftState.CFG),
                    netId++, dir.getName()
            );
            rotationMode.withCallback(val -> faceData.setRotMode(dir, val));
            rotModeBehaviours[i] = rotationMode;
            list.add(rotationMode);
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
            countBehaviours[i].value = faceData.getArgValue(dir);
            rotModeBehaviours[i].clampValue(faceData.getRotMode(dir));
        }
    }
}
