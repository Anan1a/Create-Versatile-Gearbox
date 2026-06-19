package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import java.util.List;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import com.anan1a.create_versatile_gearbox.foundation.behaviour.option.RotationModeBehaviour.RotationMode;
import com.anan1a.create_versatile_gearbox.foundation.behaviour.option.OperationModeBehaviour.OperationMode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 高级齿轮箱方块实体
 * <p>
 * 继承 SplitShaftBlockEntity，实现多方向动力传输。
 * 核心逻辑：根据输出面与动力源面的轴方向关系决定旋转方向，
 * 支持每个输出面独立翻转（通过扳手切换）。
 * <p>
 * <b>状态存储方案</b><br>
 * 状态通过 {@link AdvancedGearboxFaceContainer} 以 NBT 格式存储，替代 BlockState 属性，
 * 突破枚举属性数量上限，支持后续扩展更多面变种。
 * 每面额外存储 {@code SpeedValue}（整数值）和 {@code Multiplier}（浮点倍率）。
 * <p>
 * 数据流：NBT（磁盘/网络）↔ AdvancedGearboxFaceContainer（运行时）→ toArray() → ModelData（渲染）
 * <ul>
 *   <li>{@link #write(CompoundTag, HolderLookup.Provider, boolean)} → 持久化到 NBT</li>
 *   <li>{@link #read(CompoundTag, HolderLookup.Provider, boolean)} → 从 NBT 恢复</li>
 *   <li>{@link #getModelData()} → 导出到渲染管线</li>
 * </ul>
 */
public class AdvancedGearboxBlockEntity extends SplitShaftBlockEntity implements TransformableBlockEntity {

    /**
     * 六面数据的唯一真实来源（Single Source of Truth）。
     * <p>
     * 包含每面的枚举状态（ShaftState）、转速值（SpeedValue）和倍率值（Multiplier）。
     * 初始化时全默认为：SHAFT、0、1.0，在 {@link #read} 中从 NBT 恢复覆盖。
     * <p>
     * 数据流关系：
     * <ul>
     *   <li>NBT 持久化 → {@link #write}/{@link #read} 调 faceData.writeToRoot()/readFromRoot()</li>
     *   <li>动力计算 → getShaftState/getSettingValue/getMultiplier 直接从 faceData 读</li>
     *   <li>渲染管线 → getModelData() 调 faceData.toArray() 导出</li>
     *   <li>扳手交互 → setShaftState() 调 faceData.set() 修改</li>
     *   <li>滑条交互 → AdvancedGearboxConfigBehaviours callback 写入 faceData.setSpeedValue()</li>
     * </ul>
     * <p>
     * <b>延迟初始化</b>：不在字段声明处初始化，而是在 {@link #addBehaviours} 中首次使用时创建。
     * 原因是父类 {@code SmartBlockEntity} 构造器在子类字段初始化器执行前就会调用
     * {@code addBehaviours()}，直接初始化会导致 NPE。
     */
    private AdvancedGearboxFaceContainer faceData;

    /**
     * 六面转速滑条管理，每个面独立一个 {@link com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour}。
     * <p>
     * 玩家点击不同面时，对应面的滑条激活并通过 callback 写入 faceData。
     * 数据真实来源是 {@link #faceData}，滑条 value 在 NBT 读取后经 syncFromFaceData 同步。
     * 与 faceData 在同一时机延迟初始化。
     */
    private AdvancedGearboxConfigBehaviours sliderHelper;

    public AdvancedGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ===== 选项模式访问 =====

    /**
     * 获取指定面的设定值（滑条值，即 OperationMode 中的 {@code v}）。
     */
    public int getSettingValue(Direction face) {
        return faceData.getSettingValue(face);
    }

    /**
     * 获取指定面的旋转模式枚举。
     */
    public RotationMode getRotationMode(Direction face) {
        return faceData.resolveRotationMode(face);
    }

    /**
     * 获取指定面的操作模式枚举。
     */
    public OperationMode getOperationMode(Direction face) {
        return faceData.resolveOperationMode(face);
    }

    // ===== 面状态访问 =====

    /**
     * 获取指定面的传动轴状态。
     * <p>
     * 直接委托给 {@link AdvancedGearboxFaceContainer#getState(Direction)}，O(1) 数组索引。
     *
     * @param face 要查询的面方向
     * @return 该面的传动轴状态
     */
    public AdvancedGearboxShaftState getShaftState(Direction face) {
        return faceData.getState(face);
    }

    /**
     * 设置指定面的传动轴状态。
     * <p>
     * 触发三个连锁操作：
     * <ol>
     *   <li>{@code setChanged()} — 标记方块数据已变更，等待持久化</li>
     *   <li>{@code requestModelDataUpdate()} — 通知渲染系统重新查询 {@link #getModelData()}</li>
     *   <li>{@code onDataPacket()} 或 {@code saveAdditional()} 在后续调用中序列化 faceData</li>
     * </ol>
     *
     * @param face  要修改的面方向
     * @param value 新的状态值
     */
    public void setShaftState(Direction face, AdvancedGearboxShaftState value) {
        faceData.setState(face, value);
        setChanged();
        requestModelDataUpdate();
    }

    /**
     * 循环切换指定面的传动轴状态。
     * <p>
     * 状态切换顺序：SHAFT → OFF → CFG → SHAFT
     * <p>
     * 与 {@link #setShaftState(Direction, AdvancedGearboxShaftState)} 的区别：
     * 此方法直接基于当前状态计算下一个状态，无需外部传入目标值。
     * 常用于扳手交互场景（每次右键切换到下一个状态）。
     *
     * @param face 要切换的面方向
     */
    public void cycleShaftState(Direction face) {
        AdvancedGearboxShaftState current = getShaftState(face);
        setShaftState(face, current.next());
    }

    // ===== ModelData（渲染管线数据传递）=====

    /**
     * BE 层级的 ModelData 钩子，通过渲染管线传递面状态数组。
     * <p>
     * 与模型层级的 {@link AdvancedGearboxModel#gatherModelData} 共同构成双通道方案：
     * <ul>
     *   <li><b>模型级钩子</b>（{@code BakedModelWrapperWithData.gatherModelData}）—
     *       由 NeoForge 在 {@code renderBatched} 路径中调用，覆盖区块重建（SectionCompiler）。</li>
     *   <li><b>BE 级钩子</b>（{@code BlockEntity.getModelData}）—
     *       由 NeoForge 在 {@code renderSingleBlock} 等路径中调用，覆盖单方块物理化渲染。</li>
     * </ul>
     *
     * @return 包含面状态数组的 ModelData
     */
    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(AdvancedGearboxModel.FACE_STATES, faceData.toArray())
                .build();
    }

    // ===== 动力传输逻辑 =====

    /**
     * 滑条值变更后重新传播动力网络。
     * <p>
     * 参照 {@code SpeedControllerBlockEntity.updateTargetRotation()}：
     * <ol>
     *   <li>从应力网络移除旧数据</li>
     *   <li>断开所有邻居的轴连接</li>
     *   <li>清除当前动力源信息</li>
     *   <li>重新连接 → 触发全套传播，所有面都用新值计算</li>
     * </ol>
     */
    public void repropagateKinetics() {
        // 如果有网络连接
        if (hasNetwork())
            // 从应力网络（Stress/KineticNetwork）注销，消除旧应力贡献
            getOrCreateNetwork().remove(this);
        // 断开与所有邻居方块的轴连接
        RotationPropagator.handleRemoved(level, worldPosition, this);
        // 清除动力源位置（source）和内部速度，标记为"无源"
        removeSource();
        // 重新与邻居建立轴连接 → 触发 RotationPropagator 从邻居传播到自身 → 再传播到所有输出面
        attachKinetics();
    }

    /**
     * 计算指定面的旋转速度倍率，供 Create 动力网络传播调用。
     * <p>
     * Create 的 {@code RotationPropagator} 在轴连接时通过双向公式计算倍率：
     * <pre>
     * result = M_source(方向) × (1 / M_target(反向))
     * </pre>
     * 此方法为 {@code getAxisModifier} 提供每个面的倍率（即公式中的 M）。
     * <p>
     * 输入面（face == source）出现在双向公式的分母中，需返回逆倍率
     * 使输入速度正确传入。输出面出现在分子中，直接返回 output / input。
     *
     * @param face 要计算的面方向
     * @return 该面在双向公式中的倍率 M
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 获取该面的轴方向
        float axisStep = face.getAxisDirection().getStep();
        // 获取动力源面
        Direction source = getSourceFacing();
        // 获取理论速度
        float theoreticalSpeed = getTheoreticalSpeed();

        // 如果是动力源面，返回旋转方向倍率
        if (face == source) {
            float output = axisStep * getRotationModeApply(face, 1.0f);
            return output != 0 ? output : 1;
        }

        // 如果动力源面不转动或自身速度为 0，直接返回 0
        if (!getRotationMode(source).isRotating() || theoreticalSpeed == 0)
            return 0;

        // 计算输出速度
        float output = computeFaceOutput(face, theoreticalSpeed);
        return axisStep * output / theoreticalSpeed;
    }

    /**
     * 计算指定方向的实际旋转速度（Renderer / Visual 统一入口）。
     * <p>
     * 从动力网络读取基础速度，叠加该面的倍率后返回。
     * 速度 = 0 时直接返回 0（无动力传入）。
     * 倍率为 0 时也返回 0（该面关闭）。
     *
     * @param direction 要计算的方向
     * @return 该方向的旋转速度（正=正向旋转，负=反向旋转，0=静止）
     */
    public float getSpeedForDirection(Direction direction) {
        float baseSpeed = getSpeed();
        return baseSpeed != 0
                ? baseSpeed * getRotationSpeedModifier(direction)
                : 0;
    }

    /**
     * 计算指定面在输入值下的最终输出值。
     * <p>
     * 按顺序应用 OperationMode 和 RotationMode：
     * <ol>
     *   <li>OperationMode：用设定值 {@code v} 对输入值做运算（SET、MUL 等）</li>
     *   <li>RotationMode：对运算结果应用旋向修正（绝对/相对、正/反）</li>
     * </ol>
     *
     * @param face  目标面
     * @param input 输入值（动力源速度或中间倍率）
     * @return 该面的最终输出值
     */
    public float computeFaceOutput(Direction face, float input) {
        return getRotationModeApply(face, getOperationModeApply(face, input));
    }

    /**
     * 对指定面的 RotationMode 应用输入值。
     * <p>
     * 该方法用于在需要直接应用旋转修正时，如计算输出速度。
     * @param face 目标面
     * @param input 输入值
     * @return 输出值
     */
    public float getRotationModeApply(Direction face, float input) {
        return getRotationMode(face).apply(input);
    }

    /**
     * 计算指定面在输入值下的最终输出值。
     * <p>
     * 按顺序应用 OperationMode 和 RotationMode：
     * @param face  目标面
     * @param input 输入值（动力源速度或中间倍率）
     * @return 该面的最终输出值
     */
    public float getOperationModeApply(Direction face, float input) {
        return getOperationMode(face).apply(input, getSettingValue(face));
    }

    // ===== 蓝图变换 =====

    /**
     * 蓝图/装置变换时旋转面状态。
     * <p>
     * 实现 {@link TransformableBlockEntity} 接口，在蓝图旋转/镜像时被
     * {@link StructureTransform#apply(BlockEntity)} 调用。
     * <p>
     * 变换规则：
     * <ul>
     *   <li><b>旋转</b>：新位置的状态 = 旋转前在该位置的面的状态</li>
     *   <li><b>镜像</b>：镜像面交换状态（如 LEFT_RIGHT 镜像时 NORTH ↔ SOUTH）</li>
     * </ul>
     * <p>
     * 示例：绕 Y 轴顺时针旋转 90° 时，原来在 WEST 的面转到 NORTH 位置，
     * 因此新 NORTH 的状态 = 旧 WEST 的状态。
     *
     * @param blockEntity 本方块实体（由接口传入）
     * @param transform   变换信息（包含旋转轴、角度、镜像方式）
     */
    @Override
    public void transform(BlockEntity blockEntity, StructureTransform transform) {
        // 构建重映射索引数组：remap[新位置] = 旧位置索引
        int[] remap = new int[6];
        for (Direction newFace : Direction.values()) {
            int newIdx = newFace.get3DDataValue();
            Direction oldFace = newFace;

            // 绕 Y 轴旋转：逆时针循环 n 次 = 顺时针旋转 n × 90°
            if (transform.rotation != null && transform.rotationAxis == Direction.Axis.Y) {
                int steps = transform.rotation.ordinal();
                for (int i = 0; i < steps; i++) {
                    oldFace = oldFace.getCounterClockWise(transform.rotationAxis);
                }
            }

            // 镜像：LEFT_RIGHT ↔ NORTH/SOUTH，FRONT_BACK ↔ WEST/EAST
            if (transform.mirror != null) {
                oldFace = transform.mirror.mirror(oldFace);
            }

            remap[newIdx] = oldFace.get3DDataValue();
        }

        // 将重排索引应用到所有三组数据（枚举状态 / 转速值 / 倍率值）
        faceData.transform(remap);
    }

    // ===== NBT 读写（通过 SmartBlockEntity 的 write/read 钩子）=====

    /**
     * 写入 NBT：序列化面状态（枚举 + 转速 + 倍率）到磁盘/网络数据包。
     * <p>
     * {@code write()} 是 SmartBlockEntity 的非 final 钩子，在 {@code saveAdditional()}
     * 内部被调用。覆写后通过 {@code faceData.writeToRoot(tag)} 和 {@code faceData.readFromRoot(tag)} 持久化。
     * <p>
     * 序列化格式（每个面包含枚举状态 + 转速值 + 倍率值）：
     * {@code {"FaceStateData": {"DOWN":{"FaceState":"fwd","SpeedValue":0,"Multiplier":1.0},"UP":{...}}}}
     *
     * @param tag          目标 NBT（最终写入磁盘或发往客户端）
     * @param registries   注册表查找器（用于序列化注册表引用）
     * @param clientPacket true=发送给客户端同步，false=保存到磁盘
     */
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        faceData.writeToRoot(tag);
    }

    /**
     * 读取 NBT：从持久化/网络数据包恢复面状态。
     * <p>
     * {@code read()} 是 SmartBlockEntity 提供的非 final 钩子，
     * 在 {@code load()}（final）内部被调用。
     * <p>
     * <b>缺失键处理</b>：新放置的方块没有 {@code "FaceStateData"} 键，
     * 此时保持 {@link #faceData} 初始默认值（全 FWD / 0 / 1.0），不报错。
     *
     * @param tag          数据源 NBT（来自磁盘或客户端数据包）
     * @param registries   注册表查找器（用于反序列化注册表引用）
     * @param clientPacket true=来自客户端同步包，false=来自磁盘加载
     */
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        faceData.readFromRoot(tag);
        // 将 faceData 中的转速值同步到各面滑条
        if (sliderHelper != null) {
            sliderHelper.syncFromFaceData();
        }
        if (clientPacket) {
            requestModelDataUpdate();
        }
    }

    // ===== 滑条 behaviour 注册 =====

    /**
     * 注册 BlockEntityBehaviour。
     * <p>
     * 调用父类注册基础 behaviours 后，通过 {@link AdvancedGearboxConfigBehaviours}
     * 将 6 个面的转速滑条 behaviour 添加到列表中。
     * <p>
     * <b>延迟初始化</b>：{@link #faceData} 和 {@link #sliderHelper} 在此处首次使用时创建，
     * 因为父类 {@code SmartBlockEntity} 构造器会在子类字段初始化器执行前调用此方法。
     */
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 延迟初始化 faceData 和 sliderHelper
        // （子类字段初始化器在 super() 返回后才执行，但 addBehaviours 在 super() 内部调用）
        if (faceData == null) {
            faceData = new AdvancedGearboxFaceContainer();
        }
        super.addBehaviours(behaviours);
        if (sliderHelper == null) {
            sliderHelper = new AdvancedGearboxConfigBehaviours(this, faceData, behaviours);
        }
    }
}
