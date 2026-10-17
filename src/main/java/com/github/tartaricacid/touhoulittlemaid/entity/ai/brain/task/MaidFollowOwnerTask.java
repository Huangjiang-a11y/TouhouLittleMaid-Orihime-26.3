package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nullable;

public class MaidFollowOwnerTask extends Behavior<EntityMaid> {
    /**
     * 连续这么多 tick 都"想去但去不了"就直接传送到主人身边
     */
    private static final long STUCK_TELEPORT_TICKS = 40;
    /**
     * 竖直方向差这么多格，就算水平贴着脸也算"不在一层"，得往上/往下够
     */
    private static final double VERTICAL_APART = 2.0;

    private final float speedModifier;
    private final int stopDistance;
    private long nextStuckTeleportTime = 0;

    public MaidFollowOwnerTask(float speedModifier, int stopDistance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED));
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        LivingEntity owner = maid.getOwner();
        if (ownerStateConditions(owner, maid)) {
            // 如果女仆在前往呼吸点，玩家不在水中
            if (maid.getSwimManager().isGoingToBreath()) {
                return !owner.isUnderWater();
            }
            return true;
        }
        return false;
    }

    /**
     * 休息（晚上/午休）期间不跟随主人。
     * 否则"跟随主人"会和"找床睡觉"轮流覆盖 WALK_TARGET，女仆在床和你之间来回走，
     * 永远进不了床的 2 格内，于是永远不上床。
     */
    private static boolean isResting(EntityMaid maid) {
        return maid.getBrain().isActive(Activity.REST);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid maid) {
        return !isResting(maid);
    }

    @Override
    protected boolean canStillUse(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        return !isResting(maid);
    }

    @Override
    protected void tick(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        if (isResting(maid)) {
            return;
        }
        super.tick(worldIn, maid, gameTimeIn);
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        LivingEntity owner = maid.getOwner();

        // 如果女仆在前往呼吸点快要淹死了，那必须就近传送
        // 这个传送会鬼畜，但是没办法，为了救女仆只能这样了
        if (maid.getSwimManager().isGoingToBreath() && ownerStateConditions(owner, maid)
            && maidStateConditions(maid) && maid.teleportToOwner(owner)) {
            maid.getNavigationManager().resetNavigation();
            maid.getSwimManager().setGoingToBreath(false);
            maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            maid.getBrain().eraseMemory(InitBrains.TARGET_POS);
            this.doStop(worldIn, maid, gameTimeIn);
            return;
        }

        if (!ownerStateConditions(owner, maid) || !maidStateConditions(maid)) {
            return;
        }

        // 够不着主人就别原地杵着了 —— 直接传送。
        // 原因：MoveToTargetSink 计算路径时 reachRange 是写死的 0（不看 WalkTarget 的
        // closeEnoughDist），只要"主人所在那一格"走不到就直接判定寻路失败，于是女仆会一直
        // 站着不动；而这时她离主人往往只有几格（前面一堵墙 / 主人在楼上楼下），
        // 走不到 minTeleportDistance，原来的逻辑不会传送她。
        // 引擎在寻路失败时会写 CANT_REACH_WALK_TARGET_SINCE，拿它当"卡住"的信号即可。
        if (gameTimeIn >= this.nextStuckTeleportTime
                && !maid.closerThan(owner, 2)
                && isStuckFor(gameTimeIn, maid)
                && maid.teleportToOwner(owner)) {
            this.nextStuckTeleportTime = gameTimeIn + STUCK_TELEPORT_TICKS;
            maid.getNavigationManager().resetNavigation();
            maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            maid.getBrain().eraseMemory(InitBrains.TARGET_POS);
            this.doStop(worldIn, maid, gameTimeIn);
            return;
        }

        // 否则正常传送 / 走位
        int startDistance = (int) maid.getHomeRadius() - 2;
        int minTeleportDistance = startDistance + 4;
        // 主人在楼上/楼下时，即使水平贴着也得走过去（不然连走位目标都不会设，直接站着）
        boolean verticallyApart = Math.abs(owner.getY() - maid.getY()) > VERTICAL_APART;
        if (!maid.closerThan(owner, startDistance) || verticallyApart) {
            if (!maid.closerThan(owner, minTeleportDistance)) {
                maid.teleportToOwner(owner);
                maid.getNavigationManager().resetNavigation();
            } else if (!ownerIsWalkTarget(maid, owner)) {
                BehaviorUtils.setWalkAndLookTargetMemories(maid, owner, speedModifier, stopDistance);
            }
        }
    }

    /**
     * 女仆是不是"想去但去不了"：寻路失败时引擎会往 CANT_REACH_WALK_TARGET_SINCE 写时间戳
     */
    private boolean isStuckFor(long gameTime, EntityMaid maid) {
        return maid.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
                .map(since -> gameTime - since >= STUCK_TELEPORT_TICKS)
                .orElse(false);
    }

    private boolean maidStateConditions(EntityMaid maid) {
        return !maid.isHomeModeEnable() && maid.canBrainMoving();
    }

    private boolean ownerStateConditions(@Nullable LivingEntity owner, EntityMaid maid) {
        return owner != null && !owner.isSpectator() && !owner.isDeadOrDying() &&
               // 修复一个过去很多年没解决的 bug —— 女仆神秘传送问题
               // 这个 bug 的原因是，传送时没有检查女仆和主人是否在同一个维度
               maid.level == owner.level;
    }

    private boolean ownerIsWalkTarget(EntityMaid maid, LivingEntity owner) {
        return maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET).map(target -> {
            if (target.getTarget() instanceof EntityTracker tracker) {
                return tracker.getEntity().equals(owner);
            }
            return false;
        }).orElse(false);
    }
}
