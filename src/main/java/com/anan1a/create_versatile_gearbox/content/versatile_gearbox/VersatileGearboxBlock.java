package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.List;

import com.anan1a.create_versatile_gearbox.CVGBlockEntityTypes;
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
     * 六个面的传动轴状态属性
     * 支持三种状态：OFF(关闭), SAME(同向), OPPOSITE(反向)
     */
    public static final EnumProperty<VersatileGearboxShaftState> DOWN_STATE = EnumProperty.create("down_state", VersatileGearboxShaftState.class);
    public static final EnumProperty<VersatileGearboxShaftState> UP_STATE = EnumProperty.create("up_state", VersatileGearboxShaftState.class);
    public static final EnumProperty<VersatileGearboxShaftState> NORTH_STATE = EnumProperty.create("north_state", VersatileGearboxShaftState.class);
    public static final EnumProperty<VersatileGearboxShaftState> SOUTH_STATE = EnumProperty.create("south_state", VersatileGearboxShaftState.class);
    public static final EnumProperty<VersatileGearboxShaftState> WEST_STATE = EnumProperty.create("west_state", VersatileGearboxShaftState.class);
    public static final EnumProperty<VersatileGearboxShaftState> EAST_STATE = EnumProperty.create("east_state", VersatileGearboxShaftState.class);

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
        return switch (face) {
            case DOWN -> state.getValue(DOWN_STATE);
            case UP -> state.getValue(UP_STATE);
            case NORTH -> state.getValue(NORTH_STATE);
            case SOUTH -> state.getValue(SOUTH_STATE);
            case WEST -> state.getValue(WEST_STATE);
            case EAST -> state.getValue(EAST_STATE);
        };
    }

    /**
     * 设置指定方向的传动轴状态
     */
    public static BlockState setShaftState(Direction face, BlockState state, VersatileGearboxShaftState shaftState) {
        return switch (face) {
            case DOWN -> state.setValue(DOWN_STATE, shaftState);
            case UP -> state.setValue(UP_STATE, shaftState);
            case NORTH -> state.setValue(NORTH_STATE, shaftState);
            case SOUTH -> state.setValue(SOUTH_STATE, shaftState);
            case WEST -> state.setValue(WEST_STATE, shaftState);
            case EAST -> state.setValue(EAST_STATE, shaftState);
        };
    }

    /**
     * 获取下一个状态（循环切换）
     * OFF → SAME → OPPOSITE → OFF
     */
    public static VersatileGearboxShaftState getNextState(VersatileGearboxShaftState current) {
        return switch (current) {
            case OFF -> VersatileGearboxShaftState.FWD;
            case FWD -> VersatileGearboxShaftState.REV;
            case REV -> VersatileGearboxShaftState.OFF;
        };
    }

    /**
     * 获取指定方向对应的状态属性
     */
    public static EnumProperty<VersatileGearboxShaftState> getStateProperty(Direction face) {
        return switch (face) {
            case DOWN -> DOWN_STATE;
            case UP -> UP_STATE;
            case NORTH -> NORTH_STATE;
            case SOUTH -> SOUTH_STATE;
            case WEST -> WEST_STATE;
            case EAST -> EAST_STATE;
        };
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
     * 【实际破坏场景】当玩家破坏方块时调用此方法，决定实际获得的物品：
     * - 当前实现委托给父类方法，使用默认掉落逻辑
     * - 可根据需要重写此方法，实现基于方块状态的自定义掉落
     * <p>
     * 【掉落逻辑】
     * - 玩家使用精准采集附魔的镐子时，如果此方法返回空列表，方块不会被破坏
     * - 此方法的返回值受 LootParams 参数影响（可被战利品表修改）
     *
     * @param state    方块当前状态（包含朝向轴信息）
     * @param builder  战利品参数构建器（可用于查询破坏工具等信息）
     * @return         物品栈列表，返回空列表表示不掉落任何物品
     */
    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return super.getDrops(state, builder);
    }

    /**
     * 获取创造模式/UI显示用的物品栈
     * <p>
     * 【非破坏场景】此方法不会破坏方块，仅返回表示该方块的物品栈，用于：
     * - 创造模式物品栏中显示的物品
     * - 玩家按住 Shift 点击方块时显示的提示物品
     * - 某些mod的方块复制功能
     * <p>
     * 【与 getDrops() 的区别】
     * - getDrops() 在方块实际被破坏时调用
     * - getCloneItemStack() 仅用于显示/复制，不涉及破坏
     * - 此方法的返回值不受战利品表影响，直接返回表示方块的物品
     *
     * @param state   方块当前状态
     * @param target  玩家瞄准的位置（可用于区分点击的具体面）
     * @param level   世界实例
     * @param pos     方块位置
     * @param player  交互的玩家（可用于检查玩家状态）
     * @return        表示该方块的物品栈，用于显示
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
        return oldState.getValue(DOWN_STATE) == newState.getValue(DOWN_STATE)
            && oldState.getValue(UP_STATE) == newState.getValue(UP_STATE)
            && oldState.getValue(NORTH_STATE) == newState.getValue(NORTH_STATE)
            && oldState.getValue(SOUTH_STATE) == newState.getValue(SOUTH_STATE)
            && oldState.getValue(WEST_STATE) == newState.getValue(WEST_STATE)
            && oldState.getValue(EAST_STATE) == newState.getValue(EAST_STATE)
            && super.areStatesKineticallyEquivalent(oldState, newState);
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
     * @param state  原始方块状态
     * @param rotation 旋转方式
     * @return 旋转后的方块状态
     */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        // 获取当前六个面的状态
        VersatileGearboxShaftState down = state.getValue(DOWN_STATE);
        VersatileGearboxShaftState up = state.getValue(UP_STATE);
        VersatileGearboxShaftState north = state.getValue(NORTH_STATE);
        VersatileGearboxShaftState south = state.getValue(SOUTH_STATE);
        VersatileGearboxShaftState west = state.getValue(WEST_STATE);
        VersatileGearboxShaftState east = state.getValue(EAST_STATE);

        // 根据旋转角度重新分配水平方向的状态
        return switch (rotation) {
            case NONE -> state; // 不旋转
            case CLOCKWISE_90 -> state.setValue(DOWN_STATE, down)
                    .setValue(UP_STATE, up)
                    .setValue(NORTH_STATE, west)   // 新 NORTH = 旧 WEST（WEST→NORTH）
                    .setValue(SOUTH_STATE, east)   // 新 SOUTH = 旧 EAST（EAST→SOUTH）
                    .setValue(WEST_STATE, south)   // 新 WEST = 旧 SOUTH（SOUTH→WEST）
                    .setValue(EAST_STATE, north);  // 新 EAST = 旧 NORTH（NORTH→EAST）
            case CLOCKWISE_180 -> state.setValue(DOWN_STATE, down)
                    .setValue(UP_STATE, up)
                    .setValue(NORTH_STATE, south)  // 新 NORTH = 旧 SOUTH（SOUTH→NORTH）
                    .setValue(SOUTH_STATE, north)  // 新 SOUTH = 旧 NORTH（NORTH→SOUTH）
                    .setValue(WEST_STATE, east)    // 新 WEST = 旧 EAST（EAST→WEST）
                    .setValue(EAST_STATE, west);   // 新 EAST = 旧 WEST（WEST→EAST）
            case COUNTERCLOCKWISE_90 -> state.setValue(DOWN_STATE, down)
                    .setValue(UP_STATE, up)
                    .setValue(NORTH_STATE, east)   // 新 NORTH = 旧 EAST（EAST→NORTH）
                    .setValue(SOUTH_STATE, west)   // 新 SOUTH = 旧 WEST（WEST→SOUTH）
                    .setValue(WEST_STATE, north)   // 新 WEST = 旧 NORTH（NORTH→WEST）
                    .setValue(EAST_STATE, south);  // 新 EAST = 旧 SOUTH（SOUTH→EAST）
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
        // 获取当前六个面的状态
        VersatileGearboxShaftState down = state.getValue(DOWN_STATE);
        VersatileGearboxShaftState up = state.getValue(UP_STATE);
        VersatileGearboxShaftState north = state.getValue(NORTH_STATE);
        VersatileGearboxShaftState south = state.getValue(SOUTH_STATE);
        VersatileGearboxShaftState west = state.getValue(WEST_STATE);
        VersatileGearboxShaftState east = state.getValue(EAST_STATE);

        // 根据镜像方式重新分配状态
        return switch (mirror) {
            case NONE -> state; // 不镜像
            case LEFT_RIGHT -> state.setValue(DOWN_STATE, down)
                    .setValue(UP_STATE, up)
                    .setValue(NORTH_STATE, south)  // 新 NORTH = 旧 SOUTH（左右镜像时南北交换）
                    .setValue(SOUTH_STATE, north)  // 新 SOUTH = 旧 NORTH
                    .setValue(WEST_STATE, west)    // WEST/EAST 不变
                    .setValue(EAST_STATE, east);
            case FRONT_BACK -> state.setValue(DOWN_STATE, down)
                    .setValue(UP_STATE, up)
                    .setValue(NORTH_STATE, north)  // NORTH/SOUTH 不变
                    .setValue(SOUTH_STATE, south)
                    .setValue(WEST_STATE, east)    // 新 WEST = 旧 EAST（前后镜像时东西交换）
                    .setValue(EAST_STATE, west);   // 新 EAST = 旧 WEST
        };
    }

    /**
     * 扳手交互处理
     * <p>
     * 【交互逻辑】
     * - Shift+右键（潜行）：执行快速拆除（调用父类方法）
     * - 右键（非潜行）：切换点击面的传动轴旋转方向
     *
     * @param state   方块状态
     * @param context 使用上下文
     * @return 交互结果
     */
    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            // Shift+右键：执行快速拆除
            return super.onWrenched(state, context);
        }
        // 右键（非潜行）：调用自定义交互方法
        return onWrenchRightClick(state, context);
    }

    /**
     * 自定义扳手右键交互（非潜行）
     * <p>
     * 【功能】切换点击面的传动轴旋转方向
     * 当玩家用扳手右键点击某个面时，翻转该面的旋转方向
     *
     * @param state   方块状态
     * @param context 使用上下文
     * @return 交互结果
     */
    protected InteractionResult onWrenchRightClick(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();

        // 获取点击的方向
        Direction clickedFace = context.getClickedFace();

        // 使用 cycle 方法切换状态，这会触发 areStatesKineticallyEquivalent 检查
        // 从而自动重建动力网络
        BlockState newState = state.cycle(getStateProperty(clickedFace));
        KineticBlockEntity.switchToBlockState(level, pos, newState);

        // 播放声音反馈
        playRotateSound(level, pos);

        // 播放玩家手臂挥动动画
        if (player != null) {
            player.swing(context.getHand());
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 播放旋转声音（从 IWrenchable 接口复制）
     */
    protected void playRotateSound(Level level, BlockPos pos) {
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOODEN_BUTTON_CLICK_ON,
            net.minecraft.sounds.SoundSource.BLOCKS, 0.2f, level.random.nextFloat() * 0.2f + 0.8f);
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
