package com.github.tartaricacid.touhoulittlemaid.compat.gun.common.ai;

import com.github.tartaricacid.touhoulittlemaid.compat.gun.common.GunCommonUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class GunAttackStrafingTask extends Behavior<EntityMaid> {
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public GunAttackStrafingTask() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                        MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
                        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT),
                1200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        ItemStack item = owner.getMainHandItem();
        return GunCommonUtil.isGun(item) && owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                .filter(Entity::isAlive)
                .isPresent();
    }

    @Override
    protected void tick(ServerLevel worldIn, EntityMaid owner, long gameTime) {
        ItemStack stack = owner.getMainHandItem();
        if (!GunCommonUtil.isGun(stack)) {
            return;
        }
        owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent((target) -> {
            double distance = owner.distanceTo(target);
            // maxAttackDistance 是"保持距离"的目标值（下面按 *0.6 / *0.65 前后走位用它），
            // 不能直接拿武器射程当它 —— 否则女仆会一路后退到 ~40 格（64*0.65）。
            // 取"合适交战距离"：不超过武器射程，也不超过 20 格（64 射程 → 约 13 格）。
            float maxAttackDistance = Math.min(owner.searchRadius(), 20.0F);

            // 如果在自己的交战距离之内，而且看见的时长足够长
            if (distance < owner.searchRadius()) {
                ++this.strafingTime;
            } else {
                this.strafingTime = -1;
            }

            // 如果攻击时间也足够长，随机对走位方向和前后走位进行反转
            if (this.strafingTime >= 20) {
                if (owner.getRandom().nextFloat() < 0.3) {
                    this.strafingClockwise = !this.strafingClockwise;
                }
                if (owner.getRandom().nextFloat() < 0.3) {
                    this.strafingBackwards = !this.strafingBackwards;
                }
                this.strafingTime = 0;
            }

            // 如果攻击时间大于 -1
            if (this.strafingTime > -1) {
                // 依据距离远近决定是否前后走位
                if (distance > maxAttackDistance * 0.65) {
                    this.strafingBackwards = false;
                } else if (distance < maxAttackDistance * 0.6) {
                    this.strafingBackwards = true;
                }

                // 停住阈值另算：用武器射程（能打多远就能追多远），与上面的"保持距离"解耦，
                // 否则未开家模式时家半径=8 会把女仆按在原地"卡住不动"
                if (!owner.hasHome() && owner.getOwner() instanceof Player player && owner.distanceTo(player) >= owner.searchRadius()) {
                    owner.stopInPlace();
                } else {
                    owner.getMoveControl().strafe(this.strafingBackwards ? -0.4F : 0.4F, this.strafingClockwise ? 0.2F : -0.2F);
                }
                owner.setYRot(Mth.rotateIfNecessary(owner.getYRot(), owner.yHeadRot, 0.0F));
                BehaviorUtils.lookAtEntity(owner, target);
            } else {
                // 否则只朝向攻击目标
                BehaviorUtils.lookAtEntity(owner, target);
            }
        });
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        entityIn.setSwingingArms(true);
    }

    @Override
    protected void stop(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        entityIn.setSwingingArms(false);
        entityIn.getMoveControl().strafe(0, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        return this.checkExtraStartConditions(worldIn, entityIn);
    }
}