package com.anan1a.create_versatile_gearbox.foundation.behaviour.option;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.anan1a.create_versatile_gearbox.foundation.gui.CVGIcons;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.lang.Lang;

import net.minecraft.network.chat.Component;

/**
 * 旋转模式滑条，控制该面的旋向参考系和方向。
 * <p>
 * 示例化添加方式：
 * <pre>{@code
 * behaviours.add(new RotModeBehaviour(
 *     Component.translatable("gui.advanced_gearbox.face_rotation_mode", dir.getName()),
 *     be,
 *     new FaceValueBoxTransform(dir, 12, 12, () -> condition),
 *     netId, typeSuffix
 * ));
 * }</pre>
 */
public class RotModeBehaviour extends AbstractOptionBehaviour<RotModeBehaviour.Mode> {

    /** 选项枚举：参考系（绝对/相对） × 方向（正/反），NONE 表示不启用。 */
    public enum Mode implements INamedIconOptions {
        NONE(CVGIcons.I_NONE),
        ABSOLUTE_FORWARD(CVGIcons.I_ABSOLUTE_FORWARD),
        ABSOLUTE_REVERSE(CVGIcons.I_ABSOLUTE_REVERSE),
        RELATIVE_FORWARD(CVGIcons.I_RELATIVE_FORWARD),
        RELATIVE_REVERSE(CVGIcons.I_RELATIVE_REVERSE);

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

    private static final String TYPE_PREFIX = "rot_mode_";

    public RotModeBehaviour(Component label, SmartBlockEntity be,
                            FaceValueBoxTransform slot, int netId,
                            String typeSuffix) {
        super(Mode.class, label, be, slot, netId, TYPE_PREFIX + typeSuffix);
    }
}
