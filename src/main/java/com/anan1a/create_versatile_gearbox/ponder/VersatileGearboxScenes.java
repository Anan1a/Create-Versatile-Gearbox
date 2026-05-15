package com.anan1a.create_versatile_gearbox.ponder;

import com.anan1a.create_versatile_gearbox.CreateVersatileGearbox;
import com.anan1a.create_versatile_gearbox.CVGBlocks;
import com.anan1a.create_versatile_gearbox.content.versatile_gearbox.ShaftState;
import com.simibubi.create.AllBlocks;
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
        //   z=0    z=1    z=2    z=3    z=4
        // x=0  [ ]    [ ]    [电机]  [ ]    [ ]
        // x=1  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=2  [轴]   [轴]   [齿轮箱] [轴]   [轴]
        // x=3  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=4  [ ]    [ ]    [锯木机][ ]    [ ]
        
        BlockPos gearbox = util.grid().at(2, 1, 2);   // 齿轮箱在中心位置
        BlockPos motor = util.grid().at(0, 1, 2);     // 电机在左侧（动力源）
        BlockPos shaft1 = util.grid().at(4, 1, 2);    // 输出轴 1 在右侧（连接锯木机）
        BlockPos shaft2 = util.grid().at(2, 1, 0);    // 输出轴 2 在前侧
        BlockPos shaft3 = util.grid().at(2, 1, 4);    // 输出轴 3 在后侧

        // ==================== 定义方块选择区域 ====================
        // Selection 用于批量操作一组方块（显示/隐藏/设置动力等）
        // 
        // motorGroup: 电机及其连接的轴（x=0 到 x=1）
        Selection motorGroup = util.select().fromTo(0, 1, 2, 1, 1, 2);
        
        // gearboxSelection: 仅齿轮箱本身
        Selection gearboxSelection = util.select().position(gearbox);
        
        // shaft1Group: 右侧输出轴（x=3 到 x=4，连接锯木机）
        Selection shaft1Group = util.select().fromTo(3, 1, 2, 4, 1, 2);
        
        // shaft2Group: 前侧输出轴（z=0 到 z=1）
        Selection shaft2Group = util.select().fromTo(2, 1, 0, 2, 1, 1);
        
        // shaft3Group: 后侧输出轴（z=3 到 z=4）
        Selection shaft3Group = util.select().fromTo(2, 1, 3, 2, 1, 4);

        // ==================== 动画时间线 ====================
        // Minecraft 时间单位：20 ticks = 1 秒
        // idle() 用于控制动画节奏，让每个步骤有时间让玩家看清
        
        // 等待 10 ticks（0.5 秒），让基板稳定显示
        scene.idle(10);

        // --- 步骤 1：显示动力源 ---
        // 显示电机及其连接的轴（从上方落下）
        // Direction.DOWN 表示方块从上方进入场景
        scene.world().showSection(motorGroup, Direction.DOWN);
        scene.idle(10);  // 等待 0.5 秒让玩家看清

        // --- 步骤 2：显示齿轮箱 ---
        // 显示中心的齿轮箱
        scene.world().showSection(gearboxSelection, Direction.DOWN);
        scene.idle(15);  // 等待 0.75 秒

        // --- 步骤 3：显示介绍文本 ---
        // showText(60): 文本显示 60 ticks（3 秒）
        // text(): Ponder 会自动查找翻译键 {modid}.ponder.{scene_id}.text_N
        // pointAt(): 文本指向的目标位置（齿轮箱顶部）
        scene.overlay().showText(60)
                .text("Versatile Gearbox can transmit power and independently control each face's shaft")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(70);  // 等待 3.5 秒（包含文本显示时间）

        // --- 步骤 4：显示输出轴 ---
        // 显示右侧的输出轴和锯木机
        scene.world().showSection(shaft1Group, Direction.DOWN);
        scene.idle(10);

        // --- 步骤 5：启动动力传输 ---
        // setKineticSpeed: 设置方块的动力速度（RPM - 每分钟转速）
        // 正值 = 顺时针旋转，负值 = 逆时针旋转，0 = 停止
        // 32 RPM 是 Create 模组的常见速度
        scene.world().setKineticSpeed(motorGroup, 32);      // 电机开始转动
        scene.world().setKineticSpeed(gearboxSelection, 32); // 齿轮箱开始转动
        scene.world().setKineticSpeed(shaft1Group, 32);     // 输出轴开始转动

        // 显示说明文本
        scene.overlay().showText(50)
                .text("Gearbox can receive power input from any face")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(60);

        // --- 步骤 6：显示更多输出面 ---
        // 同时显示前侧和后侧的输出轴
        scene.world().showSection(shaft2Group, Direction.DOWN);
        scene.world().showSection(shaft3Group, Direction.DOWN);
        scene.idle(10);

        // 为所有输出面设置动力
        scene.world().setKineticSpeed(shaft2Group, 32);
        scene.world().setKineticSpeed(shaft3Group, 32);

        scene.overlay().showText(60)
                .text("And transmit power to all other faces")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(70);

        // --- 步骤 7：演示扳手交互 ---
        // showControls: 显示玩家交互提示（如左键、右键、蹲下等）
        // blockSurface: 获取方块表面中心位置
        // Pointing.DOWN: 箭头从上方指向目标
        // rightClick(): 显示右键点击图标
        scene.overlay().showControls(util.vector().blockSurface(gearbox, Direction.UP), Pointing.DOWN, 40)
                .rightClick();
        scene.idle(20);

        scene.overlay().showText(50)
                .text("Right-click a face with a wrench to toggle shaft state")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(60);

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
    public static void shaftControl(SceneBuilder builder, SceneBuildingUtil util) {
        // ==================== 场景初始化 ====================
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        
        // 设置场景标题
        scene.title("shaft_control", "Independent Shaft Control");
        
        // 配置基板（5x5 正方形）
        scene.configureBasePlate(0, 0, 5);
        
        // 显示基板（从下往上出现）
        scene.world().showSection(util.select().layer(0), Direction.UP);

        // ==================== 定义关键位置 ====================
        // 布局示意（俯视图，y=1 层）：
        //   z=0    z=1    z=2    z=3    z=4
        // x=0  [ ]    [ ]    [电机]  [ ]    [ ]
        // x=1  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=2  [ ]    [ ]    [齿轮箱][ ]    [ ]
        // x=3  [ ]    [ ]    [轴]   [ ]    [ ]
        // x=4  [ ]    [ ]    [锯木机][ ]    [ ]
        
        BlockPos gearbox = util.grid().at(2, 1, 2);   // 齿轮箱在中心
        BlockPos motor = util.grid().at(0, 1, 2);     // 电机在左侧（动力源）
        BlockPos saw = util.grid().at(4, 1, 2);       // 锯木机在右侧（动力输出端）

        // ==================== 定义方块选择区域 ====================
        // motorGroup: 电机及其连接的轴
        Selection motorGroup = util.select().fromTo(0, 1, 2, 1, 1, 2);
        
        // gearboxSelection: 齿轮箱本身
        Selection gearboxSelection = util.select().position(gearbox);
        
        // sawGroup: 右侧输出轴和锯木机（需要展示停用效果的目标）
        Selection sawGroup = util.select().fromTo(3, 1, 2, 4, 1, 2);

        // ==================== 动画时间线 ====================
        // Minecraft 时间单位：20 ticks = 1 秒
        
        // 等待基板稳定显示
        scene.idle(10);

        // --- 步骤 1：显示完整结构 ---
        // 依次显示电机、齿轮箱、锯木机（形成完整的动力传输链）
        scene.world().showSection(motorGroup, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(gearboxSelection, Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(sawGroup, Direction.DOWN);
        scene.idle(10);

        // --- 步骤 2：启动动力 ---
        // everywhere(): 选择场景中所有方块
        // 32: 正转 32 RPM（Create 模组常见转速）
        scene.world().setKineticSpeed(util.select().everywhere(), 32);
        scene.idle(20);

        // 显示说明文本
        scene.overlay().showText(50)
                .text("All shafts are active by default")
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(60);

        // 添加关键帧（用于场景分段，方便玩家在 Ponder 中跳转到这个位置）
        scene.addKeyframe();

        // --- 步骤 3：演示停用某个面 ---
        // 先显示提示文本，告诉玩家接下来要做什么
        scene.overlay().showText(60)
                .text("Right-click with a wrench to deactivate a shaft")
                .attachKeyFrame()  // 将此文本与关键帧关联
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(70);

        // 显示扳手交互提示（右键点击齿轮箱的东侧面）
        // Direction.EAST 表示东侧面（+X 方向，即右侧面）
        scene.overlay().showControls(util.vector().blockSurface(gearbox, Direction.EAST), Pointing.LEFT, 40)
                .rightClick();
        scene.idle(20);

        // --- 步骤 4：展示停用效果 ---
        // 停用东侧面后，该面不再传输动力
        // 设置 sawGroup 的动力速度为 0（停止转动）
        scene.world().setKineticSpeed(sawGroup, 0);

        // 显示说明文本（指向锯木机位置）
        // util.vector().of(4, 1.5, 2) 表示绝对坐标 (4, 1.5, 2)
        // 1.5 是方块中心偏上的位置，适合文本指向
        scene.overlay().showText(60)
                .text("After deactivation, this face no longer transmits power")
                .pointAt(util.vector().of(4, 1.5, 2));
        scene.idle(70);

        // 添加第二个关键帧（用于跳转到重新激活的部分）
        scene.addKeyframe();

        // --- 步骤 5：重新激活该面 ---
        // 先显示提示文本
        scene.overlay().showText(50)
                .text("Click again to reactivate the shaft")
                .attachKeyFrame()  // 与关键帧关联
                .pointAt(util.vector().topOf(gearbox));
        scene.idle(60);

        // 再次显示扳手交互提示（右键点击东侧面）
        scene.overlay().showControls(util.vector().blockSurface(gearbox, Direction.EAST), Pointing.LEFT, 40)
                .rightClick();
        scene.idle(20);

        // --- 步骤 6：展示恢复效果 ---
        // 恢复锯木机的动力传输（32 RPM）
        scene.world().setKineticSpeed(sawGroup, 32);

        // 显示说明文本
        scene.overlay().showText(50)
                .text("Shaft reactivated, power transmission restored")
                .pointAt(util.vector().of(4, 1.5, 2));
        scene.idle(60);

        scene.markAsFinished();
    }
}
