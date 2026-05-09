package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.Arrays;
import java.util.List;

import com.anan1a.create_versatile_gearbox.AllBlockEntityTypes;
import com.anan1a.create_versatile_gearbox.AllItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

/**
 * 多功能传动箱方块（六面轴版本）
 * <p>
 * 支持六个面全部连接传动轴，实现全方位动力传输。
 * 每个面可独立翻转旋转方向（通过扳手切换）。
 */
public class VersatileGearboxBlock extends RotatedPillarKineticBlock implements IBE<VersatileGearboxBlockEntity> {

    /**
     * 六个面的传动轴状态属性
     * 支持三种状态：OFF(关闭), SAME(同向), OPPOSITE(反向)
     */
    public static final EnumProperty<ShaftState> DOWN_STATE = EnumProperty.create("down_state", ShaftState.class);
    public static final EnumProperty<ShaftState> UP_STATE = EnumProperty.create("up_state", ShaftState.class);
    public static final EnumProperty<ShaftState> NORTH_STATE = EnumProperty.create("north_state", ShaftState.class);
    public static final EnumProperty<ShaftState> SOUTH_STATE = EnumProperty.create("south_state", ShaftState.class);
    public static final EnumProperty<ShaftState> WEST_STATE = EnumProperty.create("west_state", ShaftState.class);
    public static final EnumProperty<ShaftState> EAST_STATE = EnumProperty.create("east_state", ShaftState.class);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
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
                .setValue(DOWN_STATE, ShaftState.SAME)
                .setValue(UP_STATE, ShaftState.SAME)
                .setValue(NORTH_STATE, ShaftState.SAME)
                .setValue(SOUTH_STATE, ShaftState.SAME)
                .setValue(WEST_STATE, ShaftState.SAME)
                .setValue(EAST_STATE, ShaftState.SAME)
        );
    }

    /**
     * 所有方向的列表缓存
     * 顺序：DOWN, UP, NORTH, SOUTH, WEST, EAST
     */
    private static final List<Direction> DIRECTIONS = Direction.stream().toList();

    /**
     * 将方向转换为 Face ID（1-6）
     * 顺序：DOWN(1), UP(2), NORTH(3), SOUTH(4), WEST(5), EAST(6)
     */
    public static int getFaceId(Direction face, Axis blockAxis) {
        return DIRECTIONS.indexOf(face) + 1;
    }

    /**
     * 获取指定方向的传动轴状态
     */
    public static ShaftState getShaftState(Direction face, BlockState state) {
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
    public static BlockState setShaftState(Direction face, BlockState state, ShaftState shaftState) {
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
    public static ShaftState getNextState(ShaftState current) {
        return switch (current) {
            case OFF -> ShaftState.SAME;
            case SAME -> ShaftState.OPPOSITE;
            case OPPOSITE -> ShaftState.OFF;
        };
    }

    /**
     * 获取指定方向对应的状态属性
     */
    public static EnumProperty<ShaftState> getStateProperty(Direction face) {
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
     * - 朝向轴为Y轴（垂直）：掉落普通齿轮箱物品 -> 对应 AllBlocks.VERSATILE_GEARBOX
     * - 朝向轴为X/Z轴（水平）：掉落垂直齿轮箱物品 -> 对应 AllItems.VERTICAL_VERSATILE_GEARBOX
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
        // 检查朝向轴是否为垂直（Y轴）
        if (state.getValue(AXIS).isVertical()) {
            // 垂直朝向轴使用默认掉落（普通齿轮箱物品）
            return super.getDrops(state, builder);
        }
        // 水平朝向轴掉落垂直齿轮箱物品（方便玩家获取垂直版本）
        return Arrays.asList(new ItemStack(AllItems.VERTICAL_VERSATILE_GEARBOX.get()));
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
        // 检查朝向轴是否为垂直（Y轴）
        if (state.getValue(AXIS).isVertical()) {
            // 垂直朝向轴使用默认逻辑（显示为普通齿轮箱物品）
            return super.getCloneItemStack(state, target, level, pos, player);
        }
        // 水平朝向轴显示为垂直齿轮箱物品
        return new ItemStack(AllItems.VERTICAL_VERSATILE_GEARBOX.get());
    }

    /**
     * 获取放置时的方块状态
     * <p>
     * 默认将朝向轴设置为垂直（Y轴），即方块垂直放置
     * 玩家可以使用扳手改变朝向轴方向
     *
     * @param context 放置上下文
     * @return 方块状态
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, Axis.Y);
    }

    /**
     * 判断指定面是否有传动轴接口（六面轴版本）
     * <p>
     * 只有当该面状态不为 OFF 时才返回 true。
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return getShaftState(face, state) != ShaftState.OFF;
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
     * 获取方块的旋转轴（继承自父类）
     * <p>
     * 六面轴版本中，此方法主要用于方块渲染朝向和动力系统兼容性。
     * 实际传动轴的旋转轴由各面方向决定（如 EAST 面围绕 X 轴旋转）。
     */
    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
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
        return AllBlockEntityTypes.VERSATILE_GEARBOX.get();
    }
}
