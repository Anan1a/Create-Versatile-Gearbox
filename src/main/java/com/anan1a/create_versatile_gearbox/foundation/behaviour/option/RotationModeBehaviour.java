package com.anan1a.create_versatile_gearbox.foundation.behaviour.option;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.anan1a.create_versatile_gearbox.foundation.gui.CVGIcons;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.lang.Lang;

import net.minecraft.network.chat.Component;

/**
 * 面旋转模式滑条，控制该面使用绝对旋向还是相对旋向的转速/倍率。
 * <p>
 * 实例化时坐标 (12, 12)，添加方式：
 * <pre>{@code
 * behaviours.add(new RotationModeBehaviour(
 *     Component.translatable("gui.advanced_gearbox.face_rotation_mode", dir.getName()),
 *     be,
 *     new FaceValueBoxTransform(dir, 12, 12, () -> condition),
 *     faceIndex + 6 * 0, // netId
 *     dir.getName()
 * ));
 * }</pre>
 */
public class RotationModeBehaviour extends AbstractOptionBehaviour<RotationModeBehaviour.Mode> {

    /** 选项枚举：控制参考系 × 控制值类型，NONE 表示不使用。 */
    public enum Mode implements INamedIconOptions {
        NONE(CVGIcons.I_NONE),
        ABSOLUTE_SPEED(CVGIcons.I_ABSOLUTE_SPEED),
        ABSOLUTE_MULTIPLIER(CVGIcons.I_ABSOLUTE_MULTIPLIER),
        RELATIVE_SPEED(CVGIcons.I_RELATIVE_SPEED),
        RELATIVE_MULTIPLIER(CVGIcons.I_RELATIVE_MULTIPLIER);

        private final String translationKey;
        private final AllIcons icon;

        Mode(AllIcons icon) {
            this.icon = icon;
            this.translationKey = "create_versatile_gearbox.rotation_mode." + Lang.asId(name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }

    private static final String TYPE_PREFIX = "face_rotation_mode_";

    public RotationModeBehaviour(Component label, SmartBlockEntity be,
                                 FaceValueBoxTransform slot, int netId,
                                 String typeSuffix) {
        super(Mode.class, label, be, slot, netId, TYPE_PREFIX + typeSuffix);
    }
}
