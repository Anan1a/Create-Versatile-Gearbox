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
 * behaviours.add(new RotationModeBehaviour(
 *     Component.translatable("gui.advanced_gearbox.face_rotation_mode", dir.getName()),
 *     be,
 *     new FaceValueBoxTransform(dir, 12, 12, () -> condition),
 *     netId, typeSuffix
 * ));
 * }</pre>
 */
public class RotationModeBehaviour extends AbstractOptionBehaviour<RotationModeBehaviour.RotationMode> {

    /** 选项枚举：参考系（绝对/相对） × 方向（正/反），带转动开关。 */
    public enum RotationMode implements INamedIconOptions {
        NONE(CVGIcons.I_NONE, false, false, false),                         // 不启用
        ABSOLUTE_FORWARD(CVGIcons.I_ABSOLUTE_FORWARD, true, true, true),    // 绝对正转
        ABSOLUTE_REVERSE(CVGIcons.I_ABSOLUTE_REVERSE, true, false, true),   // 绝对反转
        RELATIVE_FORWARD(CVGIcons.I_RELATIVE_FORWARD, false, true, true),   // 相对正转
        RELATIVE_REVERSE(CVGIcons.I_RELATIVE_REVERSE, false, false, true);  // 相对反转

        /** 选项翻译键。 */
        private final String translationKey;
        /** 选项图标。 */
        private final AllIcons icon;
        /** 是否为绝对旋向（false = 相对旋向）。 */
        private final boolean absolute;
        /** 是否为正转（false = 反转）。 */
        private final boolean forward;
        /** 是否转动（false = 不传动力）。 */
        private final boolean rotating;

        RotationMode(AllIcons icon, boolean absolute, boolean forward, boolean rotating) {
            this.translationKey = "create_versatile_gearbox.rotation_mode." + Lang.asId(name());
            this.icon = icon;
            this.absolute = absolute;
            this.forward = forward;
            this.rotating = rotating;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        /** 是否为绝对旋向（false = 相对旋向）。 */
        public boolean isAbsolute() {
            return absolute;
        }

        /** 是否为正转（false = 反转）。 */
        public boolean isForward() {
            return forward;
        }

        /** 是否传动（false = 不传动力）。 */
        public boolean isRotating() {
            return rotating;
        }

        /**
         * 根据输入值计算该面的输出值。
         * <p>
         * <ul>
         *   <li>NONE → 0（不传动力）</li>
         *   <li>相对正转 → input（输出 = 输入）</li>
         *   <li>相对反转 → -input（输出 = -输入）</li>
         *   <li>绝对正转 → |input|（方向固定正转）</li>
         *   <li>绝对反转 → -|input|（方向固定反转）</li>
         * </ul>
         *
         * @param input 输入值
         * @return 输出值
         */
        public float apply(float input) {
            return rotating ? (absolute ? Math.abs(input) : input) * (forward ? 1 : -1) : 0;
        }
    }

    /** 旋转模式行为类型前缀。 */
    private static final String TYPE_PREFIX = "rotation_mode_";

    public RotationModeBehaviour(Component label, SmartBlockEntity be,
                                 FaceValueBoxTransform slot, int netId,
                                 String typeSuffix) {
        super(RotationMode.class, label, be, slot, netId, TYPE_PREFIX + typeSuffix);
    }
}
