package com.anan1a.create_versatile_gearbox.content.advanced_gearbox;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

/**
 * 高级齿轮箱方块（六面轴版本）
 * <p>
 * 支持六个面全部连接传动轴，实现全方位动力传输。
 * 每个面可独立切换纹理连接状态（通过扳手切换）。
 * <p>
 * <b>纹理连接面数据</b><br>
 * 此方块使用 BooleanProperty 存储六个面的纹理连接状态：
 * <ol>
 *   <li><b>BlockState 属性</b>（{@code down_connected} ~ {@code east_connected}）—
 *       用于 {@link #hasShaftTowards()}（动力网络连接判断）、
 *       {@link #areStatesKineticallyEquivalent()}（网络增量更新）、
 *       以及蓝图 rotate/mirror 操作。
 *       这是 Minecraft 原版机制要求，不可省略。</li>
 *   <li><b>UPDATE_DATA 属性</b> — 用于触发数据更新的布尔标志。</li>
 * </ol>
 * <p>
 * 【蓝图兼容性】方块没有方向属性，放置时永远保持相同朝向。
 */
public class AdvancedGearboxBlock extends KineticBlock implements IBE<AdvancedGearboxBlockEntity> {

    /**
     * 六个面的纹理连接面属性数组
     * <p>
     * 索引顺序：DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5
     * 与 Direction.get3DDataValue() 返回值一致
     */
    private static final BooleanProperty[] CONNECTION_PROPERTIES = new BooleanProperty[]{
            BooleanProperty.create("down_connected"),
            BooleanProperty.create("up_connected"),
            BooleanProperty.create("north_connected"),
            BooleanProperty.create("south_connected"),
            BooleanProperty.create("west_connected"),
            BooleanProperty.create("east_connected")
    };

    // 更新数据属性
    public static final BooleanProperty UPDATE_DATA = BooleanProperty.create("update_data");

    // 单方向便捷访问常量
    public static final BooleanProperty DOWN_CONNECTED = CONNECTION_PROPERTIES[0];
    public static final BooleanProperty UP_CONNECTED = CONNECTION_PROPERTIES[1];
    public static final BooleanProperty NORTH_CONNECTED = CONNECTION_PROPERTIES[2];
    public static final BooleanProperty SOUTH_CONNECTED = CONNECTION_PROPERTIES[3];
    public static final BooleanProperty WEST_CONNECTED = CONNECTION_PROPERTIES[4];
    public static final BooleanProperty EAST_CONNECTED = CONNECTION_PROPERTIES[5];

    /** 默认面状态：所有面初始化为 FWD（有轴、正向） */
    public static final AdvancedGearboxShaftState DEFAULT_SHAFT_STATE = AdvancedGearboxShaftState.FWD;

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // KineticBlock 没有方向属性，只需添加六个面的状态和更新数据属性
        super.createBlockStateDefinition(builder);
        builder.add(DOWN_CONNECTED, UP_CONNECTED, NORTH_CONNECTED, SOUTH_CONNECTED, WEST_CONNECTED, EAST_CONNECTED, UPDATE_DATA);
    }

    /**
     * 构造函数
     * @param properties 方块属性
     */
    public AdvancedGearboxBlock(Properties properties) {
        super(properties);
        // 初始化默认状态：所有面纹理连接状态与默认面状态一致，更新数据为false
        boolean defaultConnected = DEFAULT_SHAFT_STATE.hasTextureConnection();
        this.registerDefaultState(this.defaultBlockState()
                .setValue(DOWN_CONNECTED, defaultConnected)
                .setValue(UP_CONNECTED, defaultConnected)
                .setValue(NORTH_CONNECTED, defaultConnected)
                .setValue(SOUTH_CONNECTED, defaultConnected)
                .setValue(WEST_CONNECTED, defaultConnected)
                .setValue(EAST_CONNECTED, defaultConnected)
                .setValue(UPDATE_DATA, false)
        );
    }

    /**
     * 获取指定方向对应的连接属性
     */
    public static BooleanProperty getConnectionProperty(Direction face) {
        return CONNECTION_PROPERTIES[face.get3DDataValue()];
    }

    /**
     * 获取指定方向的纹理连接状态
     */
    public static boolean getConnectionState(Direction face, BlockState state) {
        return state.getValue(getConnectionProperty(face));
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
     * 获取创造模式/UI 显示用的物品栈
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
     * 判断指定面是否有传动轴接口。
     * <p>
     * 只有当该面状态有传动轴（FWD/REV）时才返回 true。
     * 从 BlockEntity 的 NBT 面状态数据读取，而非 BlockState 的 BooleanProperty。
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if (world.getBlockEntity(pos) instanceof AdvancedGearboxBlockEntity be) {
            return be.getShaftState(face).shouldRenderShaft();
        }
        return false;
    }

    /**
     * 判断两个状态是否在动力学上等效
     * <p>
     * 用于优化动力网络更新：如果状态变化不影响动力传输，不需要重建网络。
     */
    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        return super.areStatesKineticallyEquivalent(oldState, newState)
            && oldState.getValue(UPDATE_DATA) == newState.getValue(UPDATE_DATA);
    }

    /**
     * 获取方块的旋转轴。
     * <p>
     * AdvancedGearbox 支持六个面的传动轴连接，轴方向由绝对方向（东/西/南/北/上/下）
     * 而非方块朝向决定，因此视觉模型始终保持固定方向，旋转轴固定为 Y 轴。
     *
     * @param state 方块状态（未使用，固定返回 Y 轴）
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

        boolean north = state.getValue(NORTH_CONNECTED);
        boolean south = state.getValue(SOUTH_CONNECTED);
        boolean west = state.getValue(WEST_CONNECTED);
        boolean east = state.getValue(EAST_CONNECTED);

        return switch (rotation) {
            case CLOCKWISE_90 -> state
                    .setValue(NORTH_CONNECTED, west)
                    .setValue(SOUTH_CONNECTED, east)
                    .setValue(WEST_CONNECTED, south)
                    .setValue(EAST_CONNECTED, north);
            case CLOCKWISE_180 -> state
                    .setValue(NORTH_CONNECTED, south)
                    .setValue(SOUTH_CONNECTED, north)
                    .setValue(WEST_CONNECTED, east)
                    .setValue(EAST_CONNECTED, west);
            default -> state
                    .setValue(NORTH_CONNECTED, east)
                    .setValue(SOUTH_CONNECTED, west)
                    .setValue(WEST_CONNECTED, north)
                    .setValue(EAST_CONNECTED, south);
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
            boolean north = state.getValue(NORTH_CONNECTED);
            boolean south = state.getValue(SOUTH_CONNECTED);
            return state
                    .setValue(NORTH_CONNECTED, south)
                    .setValue(SOUTH_CONNECTED, north);
        }
        if (mirror == Mirror.FRONT_BACK) {
            boolean west = state.getValue(WEST_CONNECTED);
            boolean east = state.getValue(EAST_CONNECTED);
            return state
                    .setValue(WEST_CONNECTED, east)
                    .setValue(EAST_CONNECTED, west);
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
     * 扳手右键交互：切换点击面的纹理连接状态
     * <p>
     * 状态切换：true（连接）↔ false（未连接）
     *
     * @param state   当前方块状态
     * @param context 扳手使用上下文（包含玩家、位置、点击面等）
     * @return 交互成功结果
     */
    protected InteractionResult onWrenchRightClick(BlockState state, UseOnContext context) {
        Level level = context.getLevel();

        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        Direction clickedFace = context.getClickedFace();

        // 从 BlockEntity 的 NBT 获取当前面状态（作为真实数据源）并写入新状态
        boolean currentState = false;
        if (level.getBlockEntity(pos) instanceof AdvancedGearboxBlockEntity be) {
            // 先切换状态
            be.cycleShaftState(clickedFace);
            
            // 读取切换后的状态（使用切换后的值）
            AdvancedGearboxShaftState shaftState = be.getShaftState(clickedFace);
            currentState = shaftState.hasTextureConnection();
        }
        
        // 创建新状态，使用切换后的布尔值，并翻转 UPDATE_DATA 属性
        BlockState newState = state.setValue(getConnectionProperty(clickedFace), currentState)
                .setValue(UPDATE_DATA, !state.getValue(UPDATE_DATA));

        // 通知 Create 动力网络，BlockState 已变更
        KineticBlockEntity.switchToBlockState(level, pos, newState);

        playRotateSound(level, pos);

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
    public Class<AdvancedGearboxBlockEntity> getBlockEntityClass() {
        return AdvancedGearboxBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AdvancedGearboxBlockEntity> getBlockEntityType() {
        return CVGBlockEntityTypes.ADVANCED_GEARBOX.get();
    }
}