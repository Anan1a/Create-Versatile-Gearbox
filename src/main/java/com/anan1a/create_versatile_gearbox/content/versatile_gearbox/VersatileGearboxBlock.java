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
 * 与普通齿轮箱不同，它可以在多个方向上同时输入/输出动力，
 * 实现并行传动功能。
 * <p>
 * 特性：
 * - 支持 X/Y/Z 三个轴向
 * - 垂直方向(Y轴)放置时掉落普通齿轮箱物品
 * - 水平方向(X/Z轴)放置时掉落垂直齿轮箱物品
 * - 所有非箱体轴向的面都可以连接传动轴
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
     * 获取方块掉落物
     * <p>
     * 根据轴方向返回不同的物品：
     * - 垂直方向(Y轴)：返回普通齿轮箱物品
     * - 水平方向(X/Z轴)：返回垂直齿轮箱物品（方便玩家获取）
     * 
     * @param state 方块状态
     * @param builder 战利品参数构建器
     * @return 掉落物品列表
     */
    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // 检查轴方向是否为垂直
        if (state.getValue(AXIS).isVertical()) {
            // 垂直方向使用默认掉落（普通齿轮箱）
            return super.getDrops(state, builder);
        }
        // 水平方向掉落垂直齿轮箱物品
        return Arrays.asList(new ItemStack(AllItems.VERTICAL_VERSATILE_GEARBOX.get()));
    }

    /**
     * 获取克隆物品栈（用于玩家拾取方块时显示）
     * <p>
     * 与 getDrops 逻辑类似，根据轴方向返回不同的物品显示
     * 
     * @param state 方块状态
     * @param target 命中结果
     * @param level 世界
     * @param pos 方块位置
     * @param player 玩家
     * @return 物品栈
     */
    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        // 检查轴方向是否为垂直
        if (state.getValue(AXIS).isVertical()) {
            // 垂直方向使用默认逻辑
            return super.getCloneItemStack(state, target, level, pos, player);
        }
        // 水平方向返回垂直齿轮箱物品
        return new ItemStack(AllItems.VERTICAL_VERSATILE_GEARBOX.get());
    }

    /**
     * 获取放置时的方块状态
     * <p>
     * 默认将轴方向设置为垂直(Y轴)
     * 玩家可以使用扳手改变方向
     * 
     * @param context 放置上下文
     * @return 方块状态
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, Axis.Y);
    }

    /**
     * 判断方块是否有朝向指定方向的轴
     * <p>
     * 多功能传动箱的设计：与箱体轴平行的面是箱体本身，没有轴；
     * 与箱体轴垂直的面才有轴，可以连接传动轴
     * 
     * @param world 世界
     * @param pos 方块位置
     * @param state 方块状态
     * @param face 检测的方向
     * @return 是否有朝向该方向的轴
     */
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        // 只有当检测方向的轴与箱体轴不同时，才有轴
        return face.getAxis() != state.getValue(AXIS);
    }

    /**
     * 获取旋转轴
     * <p>
     * 返回方块状态中存储的轴方向
     * 
     * @param state 方块状态
     * @return 旋转轴
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
