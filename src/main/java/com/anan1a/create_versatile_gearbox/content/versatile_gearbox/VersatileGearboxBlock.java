package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.List;

import com.anan1a.create_versatile_gearbox.CVGBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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
 * 【朝向系统】
 * - 继承自 HorizontalKineticBlock，使用 HORIZONTAL_FACING 属性
 * - 旋转菜单仅显示四个水平朝向（NORTH/SOUTH/EAST/WEST）
 * - 使用 FACE_1-6 存储相对面状态，跟随方块旋转
 */
public class VersatileGearboxBlock extends HorizontalKineticBlock implements IBE<VersatileGearboxBlockEntity> {



    /**
     * 六个面的传动轴状态属性（使用相对朝向）
     * <p>
     * 【相对朝向系统】
     * - FACE_1/2: 沿主轴的两个面（如 Y 轴时为 DOWN/UP）
     * - FACE_3/4: 沿次轴的两个面（如 Y 轴时为 NORTH/SOUTH）
     * - FACE_5/6: 沿第三轴的两个面（如 Y 轴时为 WEST/EAST）
     * <p>
     * 【优势】
     * - 方块旋转时，状态跟随方块一起旋转
     * - 玩家配置的面状态不会因方块朝向改变而错乱
     */
    public static final EnumProperty<ShaftState> FACE_1 = EnumProperty.create("face_1", ShaftState.class);
    public static final EnumProperty<ShaftState> FACE_2 = EnumProperty.create("face_2", ShaftState.class);
    public static final EnumProperty<ShaftState> FACE_3 = EnumProperty.create("face_3", ShaftState.class);
    public static final EnumProperty<ShaftState> FACE_4 = EnumProperty.create("face_4", ShaftState.class);
    public static final EnumProperty<ShaftState> FACE_5 = EnumProperty.create("face_5", ShaftState.class);
    public static final EnumProperty<ShaftState> FACE_6 = EnumProperty.create("face_6", ShaftState.class);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // HorizontalKineticBlock 已经添加了 HORIZONTAL_FACING，只需添加 FACE_1-6
        super.createBlockStateDefinition(builder);
        builder.add(FACE_1, FACE_2, FACE_3, FACE_4, FACE_5, FACE_6);
    }

    /**
     * 构造函数
     * @param properties 方块属性
     */
    public VersatileGearboxBlock(Properties properties) {
        super(properties);
        // 初始化默认状态：所有面同向旋转，默认朝向北方
        this.registerDefaultState(this.defaultBlockState()
                .setValue(HORIZONTAL_FACING, Direction.NORTH)
                .setValue(FACE_1, ShaftState.FWD)
                .setValue(FACE_2, ShaftState.FWD)
                .setValue(FACE_3, ShaftState.FWD)
                .setValue(FACE_4, ShaftState.FWD)
                .setValue(FACE_5, ShaftState.FWD)
                .setValue(FACE_6, ShaftState.FWD)
        );
    }

    /**
     * 将绝对方向转换为相对面ID（1-6）
     * <p>
     * 【转换规则】基于方块的 HORIZONTAL_FACING 属性：
     * - FACING=NORTH: FACE_1=DOWN, FACE_2=UP, FACE_3=NORTH, FACE_4=SOUTH, FACE_5=WEST, FACE_6=EAST
     * - FACING=SOUTH: FACE_1=DOWN, FACE_2=UP, FACE_3=SOUTH, FACE_4=NORTH, FACE_5=EAST, FACE_6=WEST
     * - FACING=EAST:  FACE_1=DOWN, FACE_2=UP, FACE_3=EAST, FACE_4=WEST, FACE_5=SOUTH, FACE_6=NORTH
     * - FACING=WEST:  FACE_1=DOWN, FACE_2=UP, FACE_3=WEST, FACE_4=EAST, FACE_5=NORTH, FACE_6=SOUTH
     *
     * @param face       绝对方向
     * @param blockFacing 方块的水平朝向
     * @return 相对面ID (1-6)
     */
    public static int getFaceId(Direction face, Direction blockFacing) {
        if (face == Direction.DOWN) return 1;
        if (face == Direction.UP) return 2;
        
        // 水平方向的映射关系
        // NORTH=0, SOUTH=1, EAST=2, WEST=3
        // 根据blockFacing确定起始位置，然后按顺时针排列
        int[] faceIds = switch (blockFacing) {
            case NORTH -> new int[]{3, 4, 6, 5}; // N,S,E,W -> 3,4,6,5
            case SOUTH -> new int[]{4, 3, 5, 6}; // S,N,W,E -> 4,3,5,6
            case EAST -> new int[]{5, 6, 4, 3};  // E,W,N,S -> 5,6,4,3
            case WEST -> new int[]{6, 5, 3, 4};  // W,E,S,N -> 6,5,3,4
            default -> throw new IllegalArgumentException("Unsupported facing: " + blockFacing);
        };
        
        return faceIds[face.ordinal() - 2]; // Direction.NORTH.ordinal()=2, 所以减去2
    }

    /**
     * 将相对面ID转换为绝对方向
     *
     * @param faceId      相对面ID (1-6)
     * @param blockFacing 方块的水平朝向
     * @return 绝对方向
     */
    public static Direction getDirectionFromFaceId(int faceId, Direction blockFacing) {
        if (faceId == 1) return Direction.DOWN;
        if (faceId == 2) return Direction.UP;
        
        // 使用预定义的映射表进行O(1)查找
        // 索引: faceId-3 (0-3对应faceId 3-6)
        Direction[] directions = switch (blockFacing) {
            case NORTH -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            case SOUTH -> new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST};
            case EAST -> new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH};
            case WEST -> new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
            default -> throw new IllegalArgumentException("Unsupported facing: " + blockFacing);
        };
        
        // 处理水平方向的相对面ID (3-6)，通过数组索引直接获取对应的绝对方向
        if (faceId <= 6) {
            return directions[faceId - 3];
        }
        throw new IllegalArgumentException("Invalid face ID: " + faceId);
    }

    /**
     * 获取指定方向的传动轴状态（自动处理相对朝向转换）
     *
     * @param face  绝对方向
     * @param state 方块状态
     * @return 该方向的轴状态
     */
    public static ShaftState getShaftState(Direction face, BlockState state) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        int faceId = getFaceId(face, facing);
        return getShaftStateByFaceId(faceId, state);
    }

    /**
     * 根据相对面ID获取状态
     *
     * @param faceId 相对面ID (1-6)
     * @param state  方块状态
     * @return 该面的轴状态
     */
    public static ShaftState getShaftStateByFaceId(int faceId, BlockState state) {
        return switch (faceId) {
            case 1 -> state.getValue(FACE_1);
            case 2 -> state.getValue(FACE_2);
            case 3 -> state.getValue(FACE_3);
            case 4 -> state.getValue(FACE_4);
            case 5 -> state.getValue(FACE_5);
            case 6 -> state.getValue(FACE_6);
            default -> throw new IllegalArgumentException("Invalid face ID: " + faceId);
        };
    }

    /**
     * 设置指定方向的传动轴状态（自动处理相对朝向转换）
     *
     * @param face      绝对方向
     * @param state     方块状态
     * @param shaftState 新的轴状态
     * @return 更新后的方块状态
     */
    public static BlockState setShaftState(Direction face, BlockState state, ShaftState shaftState) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        int faceId = getFaceId(face, facing);
        return setShaftStateByFaceId(faceId, state, shaftState);
    }

    /**
     * 根据相对面ID设置状态
     *
     * @param faceId     相对面ID (1-6)
     * @param state      方块状态
     * @param shaftState 新的轴状态
     * @return 更新后的方块状态
     */
    public static BlockState setShaftStateByFaceId(int faceId, BlockState state, ShaftState shaftState) {
        return switch (faceId) {
            case 1 -> state.setValue(FACE_1, shaftState);
            case 2 -> state.setValue(FACE_2, shaftState);
            case 3 -> state.setValue(FACE_3, shaftState);
            case 4 -> state.setValue(FACE_4, shaftState);
            case 5 -> state.setValue(FACE_5, shaftState);
            case 6 -> state.setValue(FACE_6, shaftState);
            default -> throw new IllegalArgumentException("Invalid face ID: " + faceId);
        };
    }

    /**
     * 获取下一个状态（循环切换）
     * OFF → SAME → OPPOSITE → OFF
     */
    public static ShaftState getNextState(ShaftState current) {
        return switch (current) {
            case OFF -> ShaftState.FWD;
            case FWD -> ShaftState.REV;
            case REV -> ShaftState.OFF;
        };
    }

    /**
     * 获取指定方向对应的状态属性（自动处理相对朝向转换）
     *
     * @param face 绝对方向
     * @return 对应的 EnumProperty
     */
    public static EnumProperty<ShaftState> getStateProperty(Direction face) {
        // 注意：这个方法需要 BlockState 来确定 AXIS，所以提供一个重载版本
        throw new UnsupportedOperationException("Use getStateProperty(face, axis) instead");
    }

    /**
     * 获取指定方向和朝向对应的状态属性
     *
     * @param face   绝对方向
     * @param facing 方块朝向
     * @return 对应的 EnumProperty
     */
    public static EnumProperty<ShaftState> getStateProperty(Direction face, Direction facing) {
        int faceId = getFaceId(face, facing);
        return getStatePropertyByFaceId(faceId);
    }

    /**
     * 根据相对面ID获取状态属性
     *
     * @param faceId 相对面ID (1-6)
     * @return 对应的 EnumProperty
     */
    public static EnumProperty<ShaftState> getStatePropertyByFaceId(int faceId) {
        return switch (faceId) {
            case 1 -> FACE_1;
            case 2 -> FACE_2;
            case 3 -> FACE_3;
            case 4 -> FACE_4;
            case 5 -> FACE_5;
            case 6 -> FACE_6;
            default -> throw new IllegalArgumentException("Invalid face ID: " + faceId);
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
     * 获取放置时的方块状态
     * <p>
     * 优先尝试智能朝向（与相邻的动力方块对齐），否则使用玩家面对的反方向
     *
     * @param context 放置上下文
     * @return 方块状态
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction preferredSide = getPreferredHorizontalFacing(context);
        if (preferredSide != null) {
            return defaultBlockState().setValue(HORIZONTAL_FACING, preferredSide);
        }
        return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
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
        return oldState.getValue(FACE_1) == newState.getValue(FACE_1)
            && oldState.getValue(FACE_2) == newState.getValue(FACE_2)
            && oldState.getValue(FACE_3) == newState.getValue(FACE_3)
            && oldState.getValue(FACE_4) == newState.getValue(FACE_4)
            && oldState.getValue(FACE_5) == newState.getValue(FACE_5)
            && oldState.getValue(FACE_6) == newState.getValue(FACE_6)
            && super.areStatesKineticallyEquivalent(oldState, newState);
    }

    /**
     * 获取方块的旋转轴（IRotate 接口要求）
     * <p>
     * 【关键】返回 HORIZONTAL_FACING 的轴向，与动力冲压机一致。
     * 这样旋转菜单会显示四个水平朝向，而不是三轴选择。
     *
     * @param state 方块状态
     * @return 旋转轴（X 或 Z）
     */
    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
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

        // 使用 cycle 方法切换状态（自动处理相对朝向）
        Direction facing = state.getValue(HORIZONTAL_FACING);
        EnumProperty<ShaftState> property = getStateProperty(clickedFace, facing);
        BlockState newState = state.cycle(property);
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
