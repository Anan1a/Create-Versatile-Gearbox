package com.anan1a.create_versatile_gearbox.foundation.behaviour.option;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.anan1a.create_versatile_gearbox.foundation.gui.CVGIcons;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.lang.Lang;

import net.minecraft.network.chat.Component;

public class OperationModeBehaviour extends AbstractOptionBehaviour<OperationModeBehaviour.OperationMode> {
    public enum OperationMode implements INamedIconOptions {
        IDLE(CVGIcons.I_IDLE),              // s = s
        SET(CVGIcons.I_SET),                // s = v
        MAG_ADD(CVGIcons.I_MAG_ADD),        // s = s + sign(s)*v
        MAG_SUB(CVGIcons.I_MAG_SUB),        // s = s - sign(s)*v
        MUL(CVGIcons.I_MUL),                // s = s * v
        DIV(CVGIcons.I_DIV),                // s = s / v
        REV_DIV(CVGIcons.I_REV_DIV);        // s = v / s

        private final String translationKey;
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

    private static final String TYPE_PREFIX = "operation_mode_";

    public OperationModeBehaviour(Component label, SmartBlockEntity be,
                                 FaceValueBoxTransform slot, int netId,
                                 String typeSuffix) {
        super(OperationMode.class, label, be, slot, netId, TYPE_PREFIX + typeSuffix);
    }
}