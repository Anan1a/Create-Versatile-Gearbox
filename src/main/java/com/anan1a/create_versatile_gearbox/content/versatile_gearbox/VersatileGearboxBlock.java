package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.List;

import com.anan1a.create_versatile_gearbox.CVGBlockEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

/**
 * 多功能传动箱方块（六面轴版本）
 * <p>
 * 支持六个面全部连接传动轴，实现全方位动力传输。
 * 每个面可独立翻转旋转方向（通过扳手切换）。
 * <p>
 * 【蓝图兼容性】方块没有方向属性，放置时永远保持相同朝向。
 */
public class VersatileGearboxBlock extends KineticBlock implements IBE<VersatileGearboxBlockEntity> {

    /**
     * 六个面的传动轴状态属性数组
     * <p>
     * 索引顺序：DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5
     * 与 Direction.get3DDataValue() 返回值一致
     */
    @SuppressWarnings("unchecked")
    private static final EnumProperty<VersatileGearboxShaftState>[] STATE_PROPERTIES = new EnumProperty[]{
            EnumProperty.create("down_state", VersatileGearboxShaftState.class),
            EnumProperty.create("up_state", VersatileGearboxShaftState.class),
            EnumProperty.create("north_state", VersatileGearboxShaftState.class),
            EnumProperty.create("south_state", VersatileGearboxShaftState.class),
            EnumProperty.create("west_state", VersatileGearboxShaftState.class),
            EnumProperty.create("east_state", VersatileGearboxShaftState.class)
    };

    // 单方向便捷访问常量
    public static final EnumProperty<VersatileGearboxShaftState> DOWN_STATE = STATE_PROPERTIES[0];
    public static final EnumProperty<VersatileGearboxShaftState> UP_STATE = STATE_PROPERTIES[1];
    public static final EnumProperty<VersatileGearboxShaftState> NORTH_STATE = STATE_PROPERTIES[2];
    public static final EnumProperty<VersatileGearboxShaftState> SOUTH_STATE = STATE_PROPERTIES[3];
    public static final EnumProperty<VersatileGearboxShaftState> WEST_STATE = STATE_PROPERTIES[4];
    public static final EnumProperty<VersatileGearboxShaftState> EAST_STATE = STATE_PROPERTIES[5];

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // KineticBlock 没有方向属性，只需添加六个面的状态
        super.createBlockStateDefinition(builder);
        builder.add(DOWN_STATE, UP_STATE, NORTH_STATE, SOUTH_STATE, WEST_STATE, EAST_STATE);
    }

    /**
     * 构造函数
     * @param properties 方块属性
     */
    public VersatileGearboxBlock(Properties properties) {
        super(properties);
        // 初始化默认状态：所有面同向旋转
        this.registerDefaultState(this.defaultBlockState()
                .setValue(DOWN_STATE, VersatileGearboxShaftState.FWD)
                .setValue(UP_STATE, VersatileGearboxShaftState.FWD)
                .setValue(NORTH_STATE, VersatileGearboxShaftState.FWD)
                .setValue(SOUTH_STATE, VersatileGearboxShaftState.FWD)
                .setValue(WEST_STATE, VersatileGearboxShaftState.FWD)
                .setValue(EAST_STATE, VersatileGearboxShaftState.FWD)
        );
    }

    /**
     * 获取指定方向的传动轴状态
     */
    public static VersatileGearboxShaftState getShaftState(Direction face, BlockState state) {
        return state.getValue(STATE_PROPERTIES[face.get3DDataValue()]);
    }

    /**
     * 设置指定方向的传动轴状态
     */
    public static BlockState setShaftState(Direction face, BlockState state, VersatileGearboxShaftState shaftState) {
        return state.setValue(STATE_PROPERTIES[face.get3DDataValue()], shaftState);
    }

    /**
     * 获取指定方向对应的状态属性
     */
    public static EnumProperty<VersatileGearboxShaftState> getStateProperty(Direction face) {
        return STATE_PROPERTIES[face.get3DDataValue()];
    }

    /**
     * 获取活塞推动反应
     * <p>
     * 返回 PUSH_ONLY，表示方块可以被活塞推动，但不会推动其他方块
     * @param state 方块状态
     * @return 推动反应类型
     */
    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.PUSH_ONLY;
    }

    /**
     * 获取方块破坏时的掉落物
     * <p>
     * 当前使用默认掉落逻辑，由战利品表控制。
     * 可根据需要重写此方法，实现基于方块状态的自定义掉落。
     *
     * @param state   方块当前状态
     * @param builder 战利品参数构建器
     * @return        物品栈列表
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return super.getDrops(state, builder);
    }

    /**
     * 获取创造模式/UI显示用的物品栈
     * <p>
     * 此方法不破坏方块，仅返回表示该方块的物品栈，用于创造模式物品栏和提示显示。
     * 返回值不受战利品表影响，直接返回表示方块的物品。
     *
     * @param state  方块当前状态
     * @param target 玩家瞄准的位置
     * @param level  世界实例
     * @param pos    方块位置
     * @param player 交互的玩家
     * @return       表示该方块的物品栈
     */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    /**
     * 判断指定面是否有传动轴接口（六面轴版本）
     * <p>
     * 只有当该面状态不为 OFF 时才返回 true。
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return getShaftState(face, state) != VersatileGearboxShaftState.OFF;
    }

    /**
     * 判断两个状态是否在动力学上等效
     * <p>
     * 用于优化动力网络更新：如果状态变化不影响动力传输，不需要重建网络。
     */
    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return super.areStatesKineticallyEquivalent(oldState, newState)
            && oldState.getValue(DOWN_STATE) == newState.getValue(DOWN_STATE)
            && oldState.getValue(UP_STATE) == newState.getValue(UP_STATE)
            && oldState.getValue(NORTH_STATE) == newState.getValue(NORTH_STATE)
            && oldState.getValue(SOUTH_STATE) == newState.getValue(SOUTH_STATE)
            && oldState.getValue(WEST_STATE) == newState.getValue(WEST_STATE)
            && oldState.getValue(EAST_STATE) == newState.getValue(EAST_STATE);
    }

    /**
     * 获取方块的旋转轴（IRotate 接口要求）
     * <p>
     * 【蓝图兼容性】始终返回 Y 轴，使方块在任何情况下都保持相同的视觉朝向。
     * 方块本身没有方向属性，所有面的功能通过绝对方向系统处理。
     *
     * @param state 方块状态
     * @return 固定为 Y 轴
     */
    @Override
    public Axis getRotationAxis(BlockState state) {
        return Axis.Y;
    }

    /**
     * 旋转方块状态（蓝图旋转支持）
     * <p>
     * 【蓝图兼容性】当蓝图旋转时，六个面的状态需要相应地旋转。
     * 旋转规则：新位置的状态 = 旋转前在该位置的面的状态
     * <p>
     * 例如：绕 Y 轴顺时针旋转 90° 时：
     * - 原来在 WEST 的面转到 NORTH 位置 → 新 NORTH = 旧 WEST
     * - 原来在 NORTH 的面转到 EAST 位置 → 新 EAST = 旧 NORTH
     * - 原来在 EAST 的面转到 SOUTH 位置 → 新 SOUTH = 旧 EAST
     * - 原来在 SOUTH 的面转到 WEST 位置 → 新 WEST = 旧 SOUTH
     *
     * @param state    原始方块状态
     * @param rotation 旋转方式
     * @return 旋转后的方块状态
     */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation == Rotation.NONE) return state;

        VersatileGearboxShaftState north = state.getValue(NORTH_STATE);
        VersatileGearboxShaftState south = state.getValue(SOUTH_STATE);
        VersatileGearboxShaftState west = state.getValue(WEST_STATE);
        VersatileGearboxShaftState east = state.getValue(EAST_STATE);

        return switch (rotation) {
            case CLOCKWISE_90 -> state
                    .setValue(NORTH_STATE, west)
                    .setValue(SOUTH_STATE, east)
                    .setValue(WEST_STATE, south)
                    .setValue(EAST_STATE, north);
            case CLOCKWISE_180 -> state
                    .setValue(NORTH_STATE, south)
                    .setValue(SOUTH_STATE, north)
                    .setValue(WEST_STATE, east)
                    .setValue(EAST_STATE, west);
            default -> state
                    .setValue(NORTH_STATE, east)
                    .setValue(SOUTH_STATE, west)
                    .setValue(WEST_STATE, north)
                    .setValue(EAST_STATE, south);
        };
    }

    /**
     * 镜像方块状态（蓝图镜像支持）
     * <p>
     * 【蓝图兼容性】当蓝图镜像时，六个面的状态需要相应地翻转。
     * <p>
     * Minecraft 镜像规则：
     * - LEFT_RIGHT: 左右镜像 → NORTH ↔ SOUTH 交换
     * - FRONT_BACK: 前后镜像 → WEST ↔ EAST 交换
     *
     * @param state  原始方块状态
     * @param mirror 镜像方式
     * @return 镜像后的方块状态
     */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.LEFT_RIGHT) {
            VersatileGearboxShaftState north = state.getValue(NORTH_STATE);
            VersatileGearboxShaftState south = state.getValue(SOUTH_STATE);
            return state
                    .setValue(NORTH_STATE, south)
                    .setValue(SOUTH_STATE, north);
        }
        if (mirror == Mirror.FRONT_BACK) {
            VersatileGearboxShaftState west = state.getValue(WEST_STATE);
            VersatileGearboxShaftState east = state.getValue(EAST_STATE);
            return state
                    .setValue(WEST_STATE, east)
                    .setValue(EAST_STATE, west);
        }
        return state;
    }

    /**
     * 扳手交互处理
     * <p>
     * - Shift+右键（潜行）：执行快速拆除（调用父类方法）
     * - 右键（非潜行）：循环切换点击面传动轴的状态
     *
     * @param state   方块状态
     * @param context 使用上下文
     * @return 交互结果
     */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return super.onWrenched(state, context);
        }
        return onWrenchRightClick(state, context);
    }

    /**
     * 扳手右键交互：循环切换点击面的轴状态
     * <p>
     * 状态切换顺序：FWD（同向）→ REV（反向）→ OFF（关闭）→ FWD
     * <p>
     * 实现要点：
     * 1. 仅服务端处理：客户端直接返回，避免重复操作
     * 2. 使用 BlockState.cycle() 自动按枚举顺序切换状态
     * 3. 调用 switchToBlockState() 触发 Create 的动力网络重建
     * 4. 播放音效并触发玩家挥臂动画
     *
     * @param state   当前方块状态
     * @param context 扳手使用上下文（包含玩家、位置、点击面等）
     * @return 交互成功结果
     */
    protected InteractionResult onWrenchRightClick(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        // 仅服务端处理：避免客户端和服务端重复执行
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // 提取交互上下文信息
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction clickedFace = context.getClickedFace();

        // cycle() 按枚举声明顺序自动切换：FWD → REV → OFF → FWD
        BlockState newState = state.cycle(getStateProperty(clickedFace));
        // 通知 Create 动力网络：方块状态已变更，需要重新计算连接关系
        KineticBlockEntity.switchToBlockState(level, pos, newState);

        // 播放扳手交互音效
        playRotateSound(level, pos);

        // 触发玩家挥臂动画（仅当玩家存在时）
        if (player != null) {
            player.swing(context.getHand());
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 播放扳手交互音效（使用 Create 标准扳手声音）
     */
    protected void playRotateSound(Level level, BlockPos pos) {
        AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, level.getRandom().nextFloat() + .5f);
    }

    // ===== IBE 接口实现 =====
    @Override
    public Class<VersatileGearboxBlockEntity> getBlockEntityClass() {
        return VersatileGearboxBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends VersatileGearboxBlockEntity> getBlockEntityType() {
        return CVGBlockEntityTypes.VERSATILE_GEARBOX.get();
    }
}
