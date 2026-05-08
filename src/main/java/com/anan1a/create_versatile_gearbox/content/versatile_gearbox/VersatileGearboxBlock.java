package com.anan1a.create_versatile_gearbox.content.versatile_gearbox;

import java.util.Arrays;
import java.util.List;

import com.anan1a.create_versatile_gearbox.AllBlockEntityTypes;
import com.anan1a.create_versatile_gearbox.AllItems;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

/**
 * 多功能传动箱方块
 * <p>
 * 这是一个高性能的齿轮箱，支持多方向动力传输。
 * 与普通齿轮箱不同，它可以在多个方向上同时输入/输出动力，实现并行传动功能。
 * <p>
 * 【核心概念区分】
 * - 朝向轴：方块的放置方向（Y轴=垂直放置，X/Z轴=水平放置），决定方块的外观朝向
 * - 传动轴：方块侧面连接传动轴的接口，用于传递旋转动力
 * <p>
 * 【方块特性】
 * - 朝向轴为Y轴（垂直）时：侧面四个面都可以连接传动轴
 * - 朝向轴为X/Z轴（水平）时：顶面、底面和前后两面可连接传动轴（与朝向轴平行的面除外）
 * - 破坏时：朝向轴为Y轴掉落普通物品，为X/Z轴掉落垂直物品
 */
public class VersatileGearboxBlock extends RotatedPillarKineticBlock implements IBE<VersatileGearboxBlockEntity> {

    /**
     * 构造函数
     * @param properties 方块属性
     */
    public VersatileGearboxBlock(Properties properties) {
        super(properties);
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
     * 判断指定面是否有传动轴接口
     * <p>
     * 多功能传动箱的设计原则：
     * - 与朝向轴平行的面：不是传动轴接口（因为那是箱体本身）
     * - 与朝向轴垂直的面：是传动轴接口（可以连接传动轴）
     * <p>
     * 示例（朝向轴为Y轴时）：
     * - 顶面(Y+)和底面(Y-)：无传动轴（与朝向轴平行）
     * - 北(N)、南(S)、东(E)、西(W)面：都有传动轴
     *
     * @param world 世界
     * @param pos   方块位置
     * @param state 方块状态
     * @param face  检测的方向
     * @return      该方向是否有传动轴接口
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        // 与朝向轴平行的面没有传动轴，垂直的面才有传动轴
        return face.getAxis() != state.getValue(AXIS);
    }

    /**
     * 获取方块的传动轴方向
     * <p>
     * 返回方块状态中存储的朝向轴方向
     * 该方向决定了方块的外观朝向和哪些面有传动轴接口
     *
     * @param state 方块状态
     * @return 朝向轴方向（也是传动轴的旋转轴方向）
     */
    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    /**
     * 获取对应的 BlockEntity 类
     * <p>
     * 实现 IBE 接口的方法
     * 
     * @return BlockEntity 类
     */
    @Override
    public Class<VersatileGearboxBlockEntity> getBlockEntityClass() {
        return VersatileGearboxBlockEntity.class;
    }

    /**
     * 获取对应的 BlockEntityType
     * <p>
     * 实现 IBE 接口的方法
     * 
     * @return BlockEntityType
     */
    @Override
    public BlockEntityType<? extends VersatileGearboxBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.VERSATILE_GEARBOX.get();
    }
}
