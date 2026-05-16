package com.anan1a.create_versatile_gearbox.ponder;

import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.ShaftState;
import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.VersatileGearboxBlock;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 多功能齿轮箱思索场景
 * <p>
 * 作用：定义 Ponder 教学动画的具体内容
 * <p>
 * 工作原理：
 * - 每个方法代表一个完整的场景（一段教学动画）
 * - 通过控制方块的显示/隐藏、动力传输、文本提示等，向玩家演示方块用法
 * - 类似于导演脚本，按时间线安排每个动作
 * <p>
 * 类比：就像电影剧本，定义了场景中出现什么、发生什么、说什么台词
 */
public class VersatileGearboxScenes {

    /**
     * 场景 1：多功能齿轮箱的基本用法
     * <p>
     * 展示内容：
     * - 齿轮箱如何传输动力
     * - 动力从输入面流向输出面的过程
     * - 使用扳手切换轴状态的交互方式
     * <p>
     * 动画流程：
     * 1. 显示基板 -> 2. 显示电机 -> 3. 显示齿轮箱 -> 4. 显示输出轴
     * 5. 启动动力 -> 6. 展示扳手交互
     * <p>
     * @param builder Ponder 场景构建器（基础 API）
     * @param util    场景构建工具（提供更便捷的方法）
     */
    public static void versatileGearboxUsage(SceneBuilder builder, SceneBuildingUtil util) {
        // ==================== 场景初始化 ====================
        // CreateSceneBuilder 是 Create 模组提供的增强版场景构建器
        // 相比原生 SceneBuilder，提供了更多便捷方法
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        
        // 设置场景标题（内部 ID 和显示文本）
        // Ponder 会自动查找翻译键：{modid}.ponder.{scene_id}.header
        scene.title("versatile_gearbox_usage", "Power Transmission of Versatile Gearbox");
        
        // 配置基板（地板）
        // 参数：x, y, z 起始坐标和基板大小（5x5 的正方形）
        scene.configureBasePlate(0, 0, 5);
        
        // 显示基板（从下往上出现）
        // layer(0) 表示 y=0 层（基板层）
        scene.world().showSection(util.select().layer(0), Direction.UP);

        // ==================== 定义场景中的关键位置 ====================
        // 坐标系统说明：
        // - Minecraft 使用 (x, y, z) 坐标系
        // - x: 东西方向（东为正），y: 上下方向（上为正），z: 南北方向（南为正）
        // - 场景中心在 (2, 1, 2)，因为基板是 5x5，中心正好在 2
        // 
        // 布局示意（俯视图，y=1 层）：
        // x=4  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=3  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=2  [锯木机][轴]   [齿轮箱][轴]   [电机]
        // x=1  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=0  [ ]    [ ]    [轴]   [ ]    [ ]
        //   z=0    z=1    z=2    z=3    z=4
        
        BlockPos gearbox = util.grid().at(2, 1, 2);   // 齿轮箱在中心位置
        BlockPos motor = util.grid().at(2, 1, 4);     // 电机在后侧（动力源）
        BlockPos saw = util.grid().at(2, 1, 0);       // 锯木机在前侧（动力输出端）

        // ==================== 定义方块选择区域 ====================
        // Selection 用于批量操作一组方块（显示/隐藏/设置动力等）
        // 
        // gearboxSelection: 仅齿轮箱本身
        Selection gearboxSelection = util.select().position(gearbox);

        // shaftXGroup1: X轴方向的传动轴（上）
        Selection shaftXGroup1 = util.select().fromTo(3, 1, 2, 4, 1, 2);
        
        // shaftXGroup2: X轴方向的传动轴（下）
        Selection shaftXGroup2 = util.select().fromTo(0, 1, 2, 1, 1, 2);

        // sawGroup: 锯木机及其连接的轴（左）
        Selection sawGroup = util.select()
                .position(saw)
                .add(util.select().position(util.grid().at(2, 1, 1)));

        // motorGroup: 电机及其连接的轴（右）
        Selection motorGroup = util.select()
                .position(motor)
                .add(util.select().position(util.grid().at(2, 1, 3)));

        // ==================== 动画时间线 ====================
        // Minecraft 时间单位：20 ticks = 1 秒
        // idle() 用于控制动画节奏，让每个步骤有时间让玩家看清
        
        // 等待 10 ticks（0.5 秒），让基板稳定显示
        scene.idle(10);

        // --- 步骤 1：显示齿轮箱 ---
        // 首先显示中心的齿轮箱（核心组件）
        scene.world().showSection(gearboxSelection, Direction.DOWN);
        scene.idle(5);

        // --- 步骤 2：显示所有传动轴 ---
        // 显示 Z 轴方向的轴（前后方向）
        scene.world().showSection(shaftXGroup1, Direction.DOWN);
        scene.idle(5);
        
        // 显示 X 轴方向的轴（左右方向）
        scene.world().showSection(shaftXGroup2, Direction.DOWN);
        scene.idle(5);

        // --- 步骤 4：显示动力输出端（锯木机）---
        // 显示前侧的锯木机及其连接的轴
        scene.world().showSection(sawGroup, Direction.DOWN);
        scene.idle(5);

        // --- 步骤 3：显示动力源（电机）---
        // 显示后侧的电机及其连接的轴
        scene.world().showSection(motorGroup, Direction.DOWN);
        scene.idle(10); // 等待动画稳定

        // --- 步骤 5：启动动力传输 ---
        // setKineticSpeed: 设置方块的动力速度（RPM - 每分钟转速）
        // 正值 = 顺时针旋转，负值 = 逆时针旋转，0 = 停止
        // 32 RPM 是 Create 模组的常见速度
        scene.world().setKineticSpeed(motorGroup, -16);      // 电机开始转动
        scene.world().setKineticSpeed(gearboxSelection, 16); // 齿轮箱开始转动
        scene.world().setKineticSpeed(sawGroup, 16);        // 锯木机组开始转动
        scene.world().setKineticSpeed(shaftXGroup1, -16);     // X轴方向轴转动
        scene.world().setKineticSpeed(shaftXGroup2, 16);     // X轴方向轴转动
        scene.idle(20);  // 等待动力稳定

        // 添加第一个关键帧
        scene.addKeyframe();

        // --- 步骤 6：显示介绍文本 ---
        // showText(60): 文本显示 60 ticks（3 秒）
        // text(): Ponder 会自动查找翻译键 {modid}.ponder.{scene_id}.text_N
        // pointAt(): 文本指向的目标位置（齿轮箱顶部）
        scene.overlay().showText(70)
                .text("Versatile Gearbox can transmit power and independently control each face's shaft")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(80);

        // 添加第二个关键帧
        scene.addKeyframe();

        // 显示说明文本
        scene.overlay().showText(60)
                .text("Gearbox can receive power input from any face")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(70);

        // 添加第三个关键帧
        scene.addKeyframe();

        scene.overlay().showText(60)
                .text("And transmit power to all other faces")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(70);

        // 添加第四个关键帧
        scene.addKeyframe();

        // --- 步骤 7：演示扳手交互 ---
        // showControls: 显示玩家交互提示（如左键、右键、蹲下等）
        // blockSurface: 获取方块表面中心位置
        // Pointing.DOWN: 箭头从上方指向目标
        // rightClick(): 显示右键点击图标
        scene.overlay().showControls(util.vector().blockSurface(gearbox, Direction.UP), Pointing.DOWN, 40)
                .rightClick()
                .withItem(AllItems.WRENCH.asStack());  // 显示扳手物品
        scene.idle(7);

        // 模拟实际切换效果：将北侧面的轴状态从 FWD → REV → OFF
        // 这里我们直接设置为 OFF（关闭）状态来演示停用效果
        scene.world().modifyBlock(gearbox, state ->
                        VersatileGearboxBlock.setShaftState(Direction.UP, state, ShaftState.REV),
                false);
        scene.idle(5);

        scene.overlay().showText(60)
                .text("Right-click a face with a wrench to toggle shaft state")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(70);


        scene.overlay().showControls(util.vector().blockSurface(gearbox, Direction.UP), Pointing.DOWN, 10)
                .rightClick()
                .withItem(AllItems.WRENCH.asStack());  // 显示扳手物品
        scene.idle(7);

        // 模拟实际切换效果：将北侧面的轴状态从 FWD → REV → OFF
        // 这里我们直接设置为 OFF（关闭）状态来演示停用效果
        scene.world().modifyBlock(gearbox, state ->
                        VersatileGearboxBlock.setShaftState(Direction.UP, state, ShaftState.OFF),
                false);
        scene.idle(10);

        scene.overlay().showControls(util.vector().blockSurface(gearbox, Direction.UP), Pointing.DOWN, 10)
                .rightClick()
                .withItem(AllItems.WRENCH.asStack());  // 显示扳手物品
        scene.idle(7);

        // 模拟实际切换效果：将北侧面的轴状态从 FWD → REV → OFF
        // 这里我们直接设置为 OFF（关闭）状态来演示停用效果
        scene.world().modifyBlock(gearbox, state ->
                        VersatileGearboxBlock.setShaftState(Direction.UP, state, ShaftState.FWD),
                false);
        scene.idle(20);

        scene.markAsFinished();
    }

    /**
     * 场景 2：独立轴控制
     * <p>
     * 展示内容：
     * - 如何控制每个面的轴状态（激活/停用）
     * - 停用轴后动力传输的变化
     * - 重新激活轴的恢复效果
     * <p>
     * 动画流程：
     * 1. 显示完整结构 -> 2. 启动动力 -> 3. 演示停用某个面
     * 4. 展示停用效果 -> 5. 重新激活 -> 6. 展示恢复效果
     * <p>
     * @param builder Ponder 场景构建器（基础 API）
     * @param util    场景构建工具（提供更便捷的方法）
     */
    public static void versatileGearboxShaftControl(SceneBuilder builder, SceneBuildingUtil util) {
        // ==================== 场景初始化 ====================
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        
        // 设置场景标题
        scene.title("versatile_gearbox_shaft_control", "Independent Shaft Control");
        
        // 配置基板（5x5 正方形）
        scene.configureBasePlate(0, 0, 5);
        
        // 显示基板（从下往上出现）
        scene.world().showSection(util.select().layer(0), Direction.UP);

        // ==================== 定义关键位置 ====================
        // 布局示意（俯视图，y=1 层）：
        //   z=0    z=1    z=2    z=3    z=4
        // x=0  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=1  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=2  [锯木机][轴]   [齿轮箱][轴]   [电机]
        // x=3  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=4  [ ]    [ ]    [轴]   [ ]    [ ]
        
        BlockPos gearbox = util.grid().at(2, 1, 2);   // 齿轮箱在中心
        BlockPos motor = util.grid().at(2, 1, 4);     // 电机在后侧（动力源）
        BlockPos saw = util.grid().at(2, 1, 0);       // 锯木机在前侧（动力输出端）

        // ==================== 定义方块选择区域 ====================
        // gearboxSelection: 仅齿轮箱本身
        Selection gearboxSelection = util.select().position(gearbox);

        // shaftXGroup1: X轴方向的传动轴（上）
        Selection shaftXGroup1 = util.select().fromTo(3, 1, 2, 4, 1, 2);
        
        // shaftXGroup2: X轴方向的传动轴（下）
        Selection shaftXGroup2 = util.select().fromTo(0, 1, 2, 1, 1, 2);

        // sawGroup: 锯木机及其连接的轴（左）
        Selection sawGroup = util.select()
                .position(saw)
                .add(util.select().position(util.grid().at(2, 1, 1)));

        // motorGroup: 电机及其连接的轴（右）
        Selection motorGroup = util.select()
                .position(motor)
                .add(util.select().position(util.grid().at(2, 1, 3)));

        // ==================== 动画时间线 ====================
        // Minecraft 时间单位：20 ticks = 1 秒
        
        // 等待基板稳定显示
        scene.idle(10);

        // --- 步骤 1：显示完整结构 ---
        // 按顺序显示：齿轮箱 → 轴 → 电机 → 锯木机
        
        // 首先显示齿轮箱
        scene.world().showSection(gearboxSelection, Direction.DOWN);
        scene.idle(5);
        
        // 显示所有传动轴
        scene.world().showSection(shaftXGroup1, Direction.DOWN);
        scene.idle(3);
        scene.world().showSection(shaftXGroup2, Direction.DOWN);
        scene.idle(3);
        
        // 显示电机
        scene.world().showSection(motorGroup, Direction.DOWN);
        scene.idle(3);
        
        // 显示锯木机
        scene.world().showSection(sawGroup, Direction.DOWN);
        scene.idle(10);

        // --- 步骤 2：启动动力 ---
        // setKineticSpeed: 设置方块的动力速度（RPM - 每分钟转速）
        // 正值 = 顺时针旋转，负值 = 逆时针旋转，0 = 停止
        scene.world().setKineticSpeed(motorGroup, 32);      // 电机开始转动
        scene.world().setKineticSpeed(gearboxSelection, -32); // 齿轮箱开始转动
        scene.world().setKineticSpeed(sawGroup, -32);        // 锯木机组开始转动
        scene.world().setKineticSpeed(shaftXGroup1, 32);     // X轴方向轴转动
        scene.world().setKineticSpeed(shaftXGroup2, -32);    // X轴方向轴转动
        scene.idle(20);  // 等待动力稳定

        // 添加第一个关键帧（初始状态）
        scene.addKeyframe();

        // 显示说明文本
        scene.overlay().showText(50)
                .text("All shafts are active by default")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(60);

        // 添加第二个关键帧（准备停用）
        scene.addKeyframe();

        // --- 步骤 3：演示停用某个面 ---
        // 先显示提示文本，告诉玩家接下来要做什么
        scene.overlay().showText(60)
                .text("Right-click with a wrench to deactivate a shaft")
                .attachKeyFrame()  // 将此文本与关键帧关联
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(70);

        // 显示扳手交互提示（右键点击齿轮箱的前侧面）
        // Direction.NORTH 表示前侧面（-Z 方向，即朝向锯木机的面）
        scene.overlay().showControls(util.vector().blockSurface(gearbox, Direction.NORTH), Pointing.LEFT, 40)
                .rightClick()
                .withItem(AllItems.WRENCH.asStack());  // 显示扳手物品
        scene.idle(7);
        
        // 模拟实际切换效果：将北侧面的轴状态从 FWD → REV → OFF
        // 这里我们直接设置为 OFF（关闭）状态来演示停用效果
        scene.world().modifyBlock(gearbox, state -> 
            VersatileGearboxBlock.setShaftState(Direction.NORTH, state, ShaftState.OFF), 
        false);

        // --- 步骤 4：展示停用效果 ---
        // 停用前侧面后，该面不再传输动力
        // 设置 sawGroup 的动力速度为 0（停止转动）
        scene.world().setKineticSpeed(sawGroup, 0);

        // 添加第三个关键帧（停用状态）
        scene.addKeyframe();

        // 显示说明文本（指向锯木机位置）
        // util.vector().of(2, 1.5, 0) 表示绝对坐标 (2, 1.5, 0)
        // 1.5 是方块中心偏上的位置，适合文本指向
        scene.overlay().showText(60)
                .text("After deactivation, this face no longer transmits power")
                .pointAt(util.vector().of(2, 1.5, 0));
        scene.idle(70);

        // 添加第四个关键帧（准备重新激活）
        scene.addKeyframe();

        // --- 步骤 5：重新激活该面 ---
        // 先显示提示文本
        scene.overlay().showText(50)
                .text("Click again to reactivate the shaft")
                .attachKeyFrame()  // 与关键帧关联
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(60);

        // 再次显示扳手交互提示（右键点击前侧面）
        scene.overlay().showControls(util.vector().blockSurface(gearbox, Direction.NORTH), Pointing.LEFT, 40)
                .rightClick()
                .withItem(AllItems.WRENCH.asStack());  // 显示扳手物品
        scene.idle(7);
        
        // 重新激活北侧面：将状态从 OFF → FWD
        scene.world().modifyBlock(gearbox, state -> 
            VersatileGearboxBlock.setShaftState(Direction.NORTH, state, ShaftState.FWD), 
        false);

        // --- 步骤 6：展示恢复效果 ---
        // 恢复锯木机的动力传输（32 RPM）
        scene.world().setKineticSpeed(sawGroup, 32);

        // 添加第五个关键帧（恢复状态）
        scene.addKeyframe();

        // 显示说明文本
        scene.overlay().showText(50)
                .text("Shaft reactivated, power transmission restored")
                .pointAt(util.vector().of(2, 1.5, 0));
        scene.idle(60);

        scene.markAsFinished();
    }
}
