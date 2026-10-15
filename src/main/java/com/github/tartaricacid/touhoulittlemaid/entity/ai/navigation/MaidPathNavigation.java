package com.github.tartaricacid.touhoulittlemaid.entity.ai.navigation;

import com.github.tartaricacid.touhoulittlemaid.entity.ai.path.MaidWrappedPathFinder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;

public class MaidPathNavigation extends GroundPathNavigation {
    /**
     * 寻路目标格的最小容差，见 {@link #createPath(BlockPos, int)}
     */
    private static final int MIN_PATH_REACH_RANGE = 2;

    public MaidPathNavigation(Mob mob, Level level) {
        super(mob, level);
        this.mob.setPathfindingMalus(PathType.COCOA, -1.0F);
    }

    /**
     * 这是"女仆工作/休息时卡在方块旁边不动"的真正根因。
     * <p>
     * 引擎的 {@code MoveToTargetSink#tryComputePath} 里写死了
     * {@code navigation.createPath(targetPos, 0)} —— reachRange 恒为 0，
     * <b>完全不看 WalkTarget 自带的 closeEnoughDist</b>（那个只用于"到了没"的判定）。
     * <p>
     * 但女仆的走位目标绝大多数是"POI 方块本身"：床、唱片机、围棋桌、野餐垫、蜂箱、
     * 耕地…… 女仆实际是站在这些方块<b>上面那一格</b>的，用 A* 的节点表示就是
     * {@code 目标.above()}；而目标方块自己（床 0.5625 高、耕地 0.9375 高……）在
     * {@code getPathTypeFromState} 里是 BLOCKED。于是"精确命中那一格"永远做不到，
     * 寻路直接失败 → 女仆站在旁边一动不动。
     * <p>
     * 任务那边其实早就知道这件事 —— 床/唱片机/棋桌/野餐垫/蜂箱的 WalkTarget
     * closeEnoughDist 都给了 1~2 的容差，只有"寻路"这半边漏了。这里补上。
     * <p>
     * 注意：抵达判定仍然走 WalkTarget 自己的 closeEnoughDist，不受本改动影响；
     * 只是让寻路愿意把"目标附近的可达点"当成终点。
     */
    @Override
    public Path createPath(BlockPos pos, int reachRange) {
        return super.createPath(pos, Math.max(reachRange, MIN_PATH_REACH_RANGE));
    }

    @Override
    protected PathFinder createPathFinder(int range) {
        this.nodeEvaluator = new MaidNodeEvaluator();
        this.nodeEvaluator.setCanOpenDoors(true);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanFloat(true);
        return new MaidWrappedPathFinder(this.nodeEvaluator, range);
    }
}