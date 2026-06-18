package com.anan1a.create_versatile_gearbox.foundation.behaviour.option;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.anan1a.create_versatile_gearbox.foundation.gui.CVGIcons;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.lang.Lang;

import net.minecraft.network.chat.Component;

/**
 * 操作模式滑条，控制该面输出速度与输入速度和设定值的运算关系。
 * <p>
 * 提供 7 种运算模式：保持、设置、绝对值加/减、乘、除、反除。
 * 运算公式中的 {@code s} 为输入速度，{@code v} 为滑条设定值。
 * <p>
 * 示例化添加方式：
 * <pre>{@code
 * behaviours.add(new OperationModeBehaviour(
 *     Component.translatable("gui.advanced_gearbox.face_operation_mode", dir.getName()),
 *     be,
 *     new FaceValueBoxTransform(dir, 12, 12, () -> condition),
 *     netId, typeSuffix
 * ));
 * }</pre>
 */
public class OperationModeBehaviour extends AbstractOptionBehaviour<OperationModeBehaviour.OperationMode> {

    /** 选项枚举：操作模式。 */
    public enum OperationMode implements INamedIconOptions {
        HOLD(CVGIcons.I_IDLE),              // s = s                // 保持
        SET(CVGIcons.I_SET),                // s = v                // 设置
        MAG_ADD(CVGIcons.I_MAG_ADD),        // s = s + sign(s)*v    // 绝对值加
        MAG_SUB(CVGIcons.I_MAG_SUB),        // s = s - sign(s)*v    // 绝对值减
        MUL(CVGIcons.I_MUL),                // s = s * v            // 乘法
        DIV(CVGIcons.I_DIV),                // s = s / v            // 除法
        REV_DIV(CVGIcons.I_REV_DIV);        // s = v / s            // 反除法

        /** 选项翻译键。 */
        private final String translationKey;
        /** 选项图标。 */
        private final AllIcons icon;

        OperationMode(AllIcons icon) {
            this.translationKey = "create_versatile_gearbox.operation_mode." + Lang.asId(name());
            this.icon = icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }
    }

    /** 操作模式行为类型前缀。 */
    private static final String TYPE_PREFIX = "operation_mode_";

    public OperationModeBehaviour(Component label, SmartBlockEntity be,
                                 FaceValueBoxTransform slot, int netId,
                                 String typeSuffix) {
        super(OperationMode.class, label, be, slot, netId, TYPE_PREFIX + typeSuffix);
    }
}