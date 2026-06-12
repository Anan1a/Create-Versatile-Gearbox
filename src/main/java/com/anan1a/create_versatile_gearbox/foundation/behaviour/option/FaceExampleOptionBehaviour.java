package com.anan1a.create_versatile_gearbox.foundation.behaviour.option;

import com.anan1a.create_versatile_gearbox.content.advanced_gearbox.AdvancedGearboxConfigBehaviours;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

import net.minecraft.network.chat.Component;

/**
 * 示例选项滑条，展示如何继承 {@link AbstractFaceOptionBehaviour}。
 * <p>
 * 实例化时坐标 (12, 12)，添加方式：
 * <pre>{@code
 * behaviours.add(new FaceExampleOptionBehaviour(
 *     Component.literal("示例模式"),
 *     be,
 *     new FaceValueBoxTransform(dir, 12, 12, () -> condition),
 *     faceIndex,
 *     0  // ordinal
 * ));
 * }</pre>
 */
public class FaceExampleOptionBehaviour extends AbstractFaceOptionBehaviour<FaceExampleOptionBehaviour.Mode> {

    /** 选项枚举，实现 INamedIconOptions 以支持 Create 的选项滑条 UI。 */
    public enum Mode implements INamedIconOptions {
        OPTION_A("示例 A"),
        OPTION_B("示例 B"),
        OPTION_C("示例 C");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public AllIcons getIcon() {
            return AllIcons.I_NONE;
        }

        @Override
        public String getTranslationKey() {
            return displayName;
        }
    }

    public FaceExampleOptionBehaviour(Component label, SmartBlockEntity be,
                                       FaceValueBoxTransform slot, int faceIndex, int ordinal) {
        super(Mode.class, label, be, slot, faceIndex, ordinal);
    }

    @Override
    public BehaviourType<?> getType() {
        return AdvancedGearboxConfigBehaviours.FACE_EXAMPLE_TYPES[faceIndex];
    }
}
