package com.anan1a.create_versatile_gearbox.foundation.behaviour.value;

import java.util.List;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.FaceValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 数值滑条的抽象基类。
 * <p>
 * 提供 {@link BehaviourType} 存储、netId、NBT no-op、createBoard 等公共逻辑。
 * 子类需实现 {@link #formatValue(Integer)}、{@link #formatSettings(ValueSettings)}，
 * 以及可选的 {@link #getValueSettings()} / {@link #setValueSettings(Player, ValueSettings, boolean)} 覆写。
 * 构造时传入拼接好的完整类型名（{@code TYPE_PREFIX + typeSuffix}）。
 */
public abstract class AbstractValueBehaviour extends ScrollValueBehaviour {

    /** 该滑条的 netId（由调用方决定，用于区分不同面/不同类型）。 */
    private final int netId;
    /** 该滑条的 BehaviourType（由子类传前缀，父类在此统一构造）。 */
    private final BehaviourType<?> type;
    /** 每行最大列数（createBoard 的第一参数）。 */
    protected final int maxCols;
    /** 刻度间隔（0 = 无刻度）。 */
    protected final int milestoneInterval;
    /** 行标题列表。 */
    protected final List<Component> rowLabels;

    /**
     * @param label             滑条标签
     * @param be                所属 BlockEntity
     * @param slot              交互框变换
     * @param netId             网络 ID
     * @param typeName          完整类型名（{@code TYPE_PREFIX + typeSuffix}）
     * @param maxCols           每行最大列数（createBoard 的第一参数）
     * @param milestoneInterval 刻度间隔（0 = 无刻度）
     * @param rowLabels         行标题列表
     */
    public AbstractValueBehaviour(Component label, SmartBlockEntity be,
                                  FaceValueBoxTransform slot, int netId,
                                  String typeName, int maxCols,
                                  int milestoneInterval, List<Component> rowLabels) {
        super(label, be, slot);
        this.netId = netId;
        this.type = new BehaviourType<>(typeName);
        this.maxCols = maxCols;
        this.milestoneInterval = milestoneInterval;
        this.rowLabels = rowLabels;
        withFormatter(this::formatValue);
    }

    /** 创建数值滑条的设置面板。 */
    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        return new ValueSettingsBoard(
                label, maxCols, milestoneInterval, rowLabels,
                new ValueSettingsFormatter(this::formatSettings)
        );
    }

    public void setRawValue(int value) {
        this.value = value;
    }

    /** 获取数值滑条的类型。 */
    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    /** 获取数值滑条的网络 ID。 */
    @Override
    public int netId() {
        return netId;
    }

    /** 内部值 → 显示文本。由子类实现具体格式。 */
    public abstract String formatValue(Integer index);

    /** 由子类提供设置面板上（ValueSettings → 显示文本）的格式转换。 */
    protected abstract MutableComponent formatSettings(ValueSettings settings);

    // NBT 由 AdvancedGearboxFaceContainer 统一管理
    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // no-op
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        // no-op
    }
}
