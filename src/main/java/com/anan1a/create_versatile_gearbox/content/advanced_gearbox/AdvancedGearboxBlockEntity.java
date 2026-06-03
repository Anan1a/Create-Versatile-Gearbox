package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

import com.anan1a.create_versatile_gearbox.foundation.FaceStateContainer;
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

/**
 * 高级齿轮箱方块实体
 * <p>
 * 继承 SplitShaftBlockEntity，实现多方向动力传输。
 * 核心逻辑：根据输出面与动力源面的轴方向关系决定旋转方向，
 * 支持每个输出面独立翻转（通过扳手切换）。
 * <p>
 * <b>状态存储方案</b><br>
 * 状态通过 {@link FaceStateContainer} 以 NBT 格式存储，替代 BlockState 属性，
 * 突破枚举属性数量上限，支持后续扩展更多面变种。
 * <p>
 * 数据流：NBT（磁盘/网络）↔ FaceStateContainer（运行时）→ toArray() → ModelData（渲染）
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
    private final FaceStateContainer<AdvancedGearboxShaftState> faceStatesNbt =
        FaceStateContainer.of(AdvancedGearboxShaftState.FWD);

    public AdvancedGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 供同包 Model 直接读取面状态数组（绕过 ModelData 机制）。
     * 返回值是 clone，修改不影响内部状态。
     */
    public AdvancedGearboxShaftState[] getFaceStatesArray() {
        return faceStatesNbt.toArray();
    }

    /**
     * 获取指定面的传动轴状态。
     * <p>
     * 直接委托给 {@link FaceStateContainer#get(Direction)}，O(1) 数组索引。
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
        AdvancedGearboxShaftState current = faceStatesNbt.get(face);
        setShaftState(face, current.next());
    }

    // ===== NBT 读写（通过 SmartBlockEntity 的 write/read 钩子）=====

    /**
     * 写入 NBT：序列化面状态到持久化/网络数据包。
     * <p>
     * {@code write()} 是 {@link com.simibubi.create.foundation.blockEntity.SmartBlockEntity}
     * 提供的非 final 钩子，在 {@code saveAdditional()}（final）内部被调用。
     * 覆写此方法以写入自定义 NBT 数据。
     * <p>
     * 序列化格式：{@code {"face_states": {"down":"fwd","up":"rev",...}}}
     *
     * @param tag          目标 NBT（最终写入磁盘或发往客户端）
     * @param registries   注册表查找器（用于序列化注册表引用）
     * @param clientPacket true=发送给客户端同步，false=保存到磁盘
     */
    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("face_states", faceStatesNbt.toNbt());
    }

    /**
     * 读取 NBT：从持久化/网络数据包恢复面状态。
     * <p>
     * {@code read()} 是 SmartBlockEntity 提供的非 final 钩子，
     * 在 {@code load()}（final）内部被调用。覆写此方法以恢复自定义 NBT 数据。
     * <p>
     * <b>缺失键处理</b>：新放置的方块没有 {@code "face_states"} 键，
     * 此时保持 {@link #faceStatesNbt} 初始默认值（全 FWD），不报错。
     *
     * @param tag          数据源 NBT（来自磁盘或客户端数据包）
     * @param registries   注册表查找器（用于反序列化注册表引用）
     * @param clientPacket true=来自客户端同步包，false=来自磁盘加载
     */
    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("face_states")) {
            faceStatesNbt.fromNbt(tag.getCompound("face_states"));
        }
        if (clientPacket) {
            requestModelDataUpdate();
        }
    }

    // ===== 动力传输逻辑 =====

    /**
     * 计算指定输出面的旋转速度倍率（覆盖模式）。
     * <p>
     * 【判断流程】
     * <ol>
     *   <li>输出面为 OFF → 返回 0（不输出动力）</li>
     *   <li>无动力源 → 返回 0（无动力可传）</li>
     *   <li>动力源面为 OFF → 返回 0（动力被切断）</li>
     *   <li>正常 → 委托给 {@link #getRotationSpeedModifier(Direction, Direction)}</li>
     * </ol>
     *
     * @param face 输出面方向
     * @return 旋转速度倍率（0=关闭，1=同向，-1=反向）
     */
    @Override
    public float getRotationSpeedModifier(Direction face) {
        // 1. 输出面无传动轴 → 断开
        if (!faceStatesNbt.get(face).shouldRenderShaft()) return 0;

        // 2. 无动力源或动力源面无传动轴 → 断开（getSourceFacing() 无源时返回 null）
        Direction source = getSourceFacing();
        if (source == null || !faceStatesNbt.get(source).shouldRenderShaft()) return 0;

        // 3. 计算旋转方向
        return getRotationSpeedModifier(face, source);
    }

    /**
     * 根据动力源面计算指定输出面的旋转速度倍率（双参数版）。
     * <p>
     * 三状态计算逻辑：
     * <pre>
     * modifier = axisAdjust × faceModifier × sourceModifier
     *
     * axisAdjust: 轴方向对齐时 +1，相反时 -1
     *   faceModifier: FWD=+1, REV=-1, OFF=0
     * sourceModifier: FWD=+1, REV=-1, OFF=0
     * </pre>
     * <p>
     * 示例：source=REV(-1), face=FWD(+1), 轴同向(+1)
     * → modifier = 1 × 1 × (-1) = -1（相对于动力源反转输出）
     *
     * @param face   输出面
     * @param source 动力源面
     * @return 旋转速度倍率（-1 ~ 1）
     */
    public float getRotationSpeedModifier(Direction face, Direction source) {
        // 获取动力源面状态
        AdvancedGearboxShaftState sourceState = faceStatesNbt.get(source);

        // 如果动力源面关闭（非传动轴状态），所有输出都停止
        if (!sourceState.shouldRenderShaft()) return 0;

        // 获取输出面状态
        AdvancedGearboxShaftState faceState = faceStatesNbt.get(face);
        // 如果输出面关闭（非传动轴状态），返回 0（不输出动力）
        if (!faceState.shouldRenderShaft()) return 0;

        // 轴方向修正
        int axisAdjust = face.getAxisDirection() == source.getAxisDirection() ? 1 : -1;
        return axisAdjust * faceState.getModifier() * sourceState.getModifier();
    }

    /**
     * 计算指定方向的实际旋转速度。
     * <p>
     * 统一 Renderer 和 Visual 的速度计算逻辑：
     * <ol>
     *   <li>使用传入的基础速度（避免重复调用 getSpeed()）</li>
     *   <li>如果有速度且有动力源，应用方向倍率</li>
     * </ol>
     * <p>
     * 倍率由 {@link #getRotationSpeedModifier(Direction, Direction)} 计算：
     * <ul>
     *   <li>FWD 面：正向传动（倍率为 1）</li>
     *   <li>REV 面：反向传动（倍率为 -1）</li>
     *   <li>OFF 面：断开（倍率为 0，速度归零）</li>
     * </ul>
     *
     * @param baseSpeed    基础速度（来自动力网络）
     * @param direction    要计算的方向
     * @param sourceFacing 动力源方向（可为 null）
     * @return 该方向的旋转速度（负数表示反向旋转）
     */
    public float getSpeedForDirection(float baseSpeed, Direction direction, Direction sourceFacing) {
        if (baseSpeed != 0 && sourceFacing != null) {
            return baseSpeed * getRotationSpeedModifier(direction, sourceFacing);
        }
        return baseSpeed;
    }

    @Override
    protected boolean isNoisy() {
        return true;
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
        // 快照旧状态：先复制所有面的状态，后续循环会覆写 faceStatesNbt，
        // 但读取旧值时必须用这份快照，避免刚写入的面影响后续面的映射。
        AdvancedGearboxShaftState[] oldStates = faceStatesNbt.toArray();

        for (Direction newFace : Direction.values()) {
            // 逆向映射：从新位置反推出变换前的旧方向，再用旧方向从快照中取值。
            // 例如顺时针旋转 90° 后，新 NORTH 位置上的状态来自旧 WEST 面。
            Direction oldFace = newFace;

            // Step 1: 应用旋转
            // 使用逆时针（getCounterClockWise）实现正向旋转映射：
            //   CLOCKWISE_90 (ordinal=1) → 1 次逆时针 → 等效顺时针 90°
            //   CLOCKWISE_180 (ordinal=2) → 2 次逆时针 → 180°
            //   COUNTERCLOCKWISE_90 (ordinal=3) → 3 次逆时针 → 等效逆时针 90°
            // 仅处理水平方向（绕 Y 轴旋转），UP/DOWN 不受水平旋转影响。
            if (transform.rotation != null && transform.rotationAxis == Direction.Axis.Y) {
                for (int i = 0; i < transform.rotation.ordinal(); i++) {
                    oldFace = oldFace.getCounterClockWise(transform.rotationAxis);
                }
            }

            // Step 2: 应用镜像
            //   LEFT_RIGHT → NORTH ↔ SOUTH 交换
            //   FRONT_BACK → WEST ↔ EAST 交换
            // 镜像对 UP/DOWN 无影响（mirror() 内部保证返回自身）。
            if (transform.mirror != null) {
                oldFace = transform.mirror.mirror(oldFace);
            }

            // 将旧方向的状态赋给新位置
            faceStatesNbt.set(newFace, oldStates[oldFace.get3DDataValue()]);
        }
    }
}
