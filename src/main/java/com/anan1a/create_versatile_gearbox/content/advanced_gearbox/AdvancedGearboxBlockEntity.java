package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import com.anan1a.create_versatile_gearbox.foundation.EnumFaceContainer;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;

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
 * 状态通过 {@link EnumFaceContainer} 以 NBT 格式存储，替代 BlockState 属性，
 * 突破枚举属性数量上限，支持后续扩展更多面变种。
 * <p>
 * 数据流：NBT（磁盘/网络）↔ EnumFaceContainer（运行时）→ toArray() → ModelData（渲染）
 * <ul>
 *   <li>{@link #write(CompoundTag, HolderLookup.Provider, boolean)} → 持久化到 NBT</li>
 *   <li>{@link #read(CompoundTag, HolderLookup.Provider, boolean)} → 从 NBT 恢复</li>
 *   <li>{@link #getModelData()} → 导出到渲染管线</li>
 * </ul>
 */
public class AdvancedGearboxBlockEntity extends SplitShaftBlockEntity implements TransformableBlockEntity {

    /**
     * 运行时面状态缓存，使用 NBT 作为持久化存储后端。
     * <p>
     * 初始化时所有面默认 FWD（同向旋转），
     * 在 {@link #read(CompoundTag, HolderLookup.Provider, boolean)} 中从 NBT 恢复覆盖。
     * <p>
     * faceStates 是本实体状态数据的<em>唯一真实来源</em>（Single Source of Truth）：
     * <ul>
     *   <li>NBT 读写 → faceStates.toNbt() / faceStates.fromNbt()</li>
     *   <li>动力逻辑 → faceStates.get(face) 读取面状态</li>
     *   <li>渲染管线 → faceStates.toArray() 导出到 ModelData</li>
     *   <li>扳手交互 → setShaftState() 经 faceStates.set() 修改</li>
     * </ul>
     * BlockState 中也保留了一份相同的数据（由 {@link AdvancedGearboxBlock} 维护），
     * 主要用于 {@code hasShaftTowards()}（动力网络查询）和蓝图 rotate/mirror。
     */
    private final EnumFaceContainer<AdvancedGearboxShaftState> faceStatesNbt =
        EnumFaceContainer.of(AdvancedGearboxBlock.DEFAULT_SHAFT_STATE);

    public AdvancedGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 获取指定面的传动轴状态。
     * <p>
     * 直接委托给 {@link EnumFaceContainer#get(Direction)}，O(1) 数组索引。
     *
     * @param face 要查询的面方向
     * @return 该面的传动轴状态
     */
    public AdvancedGearboxShaftState getShaftState(Direction face) {
        return faceStatesNbt.get(face);
    }

    /**
     * 设置指定面的传动轴状态。
     * <p>
     * 触发三个连锁操作：
     * <ol>
     *   <li>{@code setChanged()} — 标记方块数据已变更，等待持久化（自动在下一次 save tick 写入 NBT）</li>
     *   <li>{@code requestModelDataUpdate()} — 通知渲染系统重新查询 {@link #getModelData()}，触发模型重载</li>
     *   <li>{@code onDataPacket()} 或 {@code saveAdditional()} 在后续调用中序列化 faceStates</li>
     * </ol>
     *
     * @param face  要修改的面方向
     * @param value 新的状态值
     */
    public void setShaftState(Direction face, AdvancedGearboxShaftState value) {
        faceStatesNbt.set(face, value);
        setChanged();
        requestModelDataUpdate();
    }

    /**
     * 循环切换指定面的传动轴状态。
     * <p>
     * 状态切换顺序：FWD → REV → OFF → FWD
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
                .with(AdvancedGearboxModel.FACE_STATES, faceStatesNbt.toArray())
                .build();
    }

    // ===== 动力传输逻辑 =====

    /**
     * 计算指定面的旋转速度倍率（单参数版，供 Create 动力网络调用）。
     * <p>
     * 仅考虑该面自身的轴向和状态，不与动力源面做联合计算。
     * 公式：{@code axisStep × modifier}
     * <ul>
     *   <li>{@code axisStep} = 该面的轴方向（POSITIVE=1, NEGATIVE=-1）</li>
     *   <li>{@code modifier} = 该面状态的倍率（FWD=1, REV=-1, OFF=0 ...）</li>
     * </ul>
     *
     * @param face 要计算的面方向
     * @return 旋转速度倍率（-1=反向, 0=关闭, 1=正向）
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 计算旋转速度倍率：轴方向 × 状态倍率
        return face.getAxisDirection().getStep()
                * getShaftState(face).getModifier();
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
        // 快照旧状态，避免刚写入的面影响后续映射
        AdvancedGearboxShaftState[] oldStates = faceStatesNbt.toArray();

        for (Direction newFace : Direction.values()) {
            // 逆向映射：从新位置反推出变换前的旧方向，再从快照取值
            Direction oldFace = newFace;

            // 绕 Y 轴旋转：getCounterClockWise 是"逆时针"，
            // 循环调用 n 次可实现"顺时针旋转 n 个 90°"的映射效果
            if (transform.rotation != null && transform.rotationAxis == Direction.Axis.Y) {
                int steps = transform.rotation.ordinal();
                for (int i = 0; i < steps; i++) {
                    oldFace = oldFace.getCounterClockWise(transform.rotationAxis);
                }
            }

            // 镜像：LEFT_RIGHT ↔ N/S，FRONT_BACK ↔ W/E
            if (transform.mirror != null) {
                oldFace = transform.mirror.mirror(oldFace);
            }

            faceStatesNbt.set(newFace, oldStates[oldFace.get3DDataValue()]);
        }
    }

    // ===== NBT 读写（通过 SmartBlockEntity 的 write/read 钩子）=====

    /**
     * 写入 NBT：序列化面状态到持久化/网络数据包。
     * <p>
     * {@code write()} 是 {@link com.simibubi.create.foundation.blockEntity.SmartBlockEntity}
     * 提供的非 final 钩子，在 {@code saveAdditional()}（final）内部被调用。
     * 覆写此方法以写入自定义 NBT 数据。
     * <p>
     * 序列化格式：{@code {"FaceStateData": {"DOWN":{"FaceState":"fwd"},"UP":{"FaceState":"rev"},...}}}
     *
     * @param tag          目标 NBT（最终写入磁盘或发往客户端）
     * @param registries   注册表查找器（用于序列化注册表引用）
     * @param clientPacket true=发送给客户端同步，false=保存到磁盘
     */
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put(EnumFaceContainer.FACE_ROOT_KEY, faceStatesNbt.toNbt());
    }

    /**
     * 读取 NBT：从持久化/网络数据包恢复面状态。
     * <p>
     * {@code read()} 是 SmartBlockEntity 提供的非 final 钩子，
     * 在 {@code load()}（final）内部被调用。覆写此方法以恢复自定义 NBT 数据。
     * <p>
     * <b>缺失键处理</b>：新放置的方块没有 {@code "FaceStateData"} 键，
     * 此时保持 {@link #faceStatesNbt} 初始默认值（全 FWD），不报错。
     *
     * @param tag          数据源 NBT（来自磁盘或客户端数据包）
     * @param registries   注册表查找器（用于反序列化注册表引用）
     * @param clientPacket true=来自客户端同步包，false=来自磁盘加载
     */
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(EnumFaceContainer.FACE_ROOT_KEY)) {
            faceStatesNbt.fromNbt(tag.getCompound(EnumFaceContainer.FACE_ROOT_KEY));
        }
        if (clientPacket) {
            requestModelDataUpdate();
        }
    }
}
