package com.github.tartaricacid.touhoulittlemaid.event;

import cn.sh1rocu.touhoulittlemaid.api.event.ProjectileImpactEvent;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class EntityHurtEvent {
    public static void onArrowImpact(ProjectileImpactEvent event) {
        Entity attacker = event.getProjectile().getOwner();
        HitResult ray = event.getRayTraceResult();
        if (attacker instanceof TamableAnimal thrower && ray instanceof EntityHitResult hitResult) {
            Entity victim = hitResult.getEntity();
            if (victim instanceof TamableAnimal tameable) {
                LivingEntity owner = thrower.getOwner();
                // 同一主人，那么免伤
                if (owner != null && tameable.isOwnedBy(owner)) {
                    event.setCanceled(true);
                }
            }
            if (victim instanceof LivingEntity livingVictim) {
                // 主人和同 Team 玩家免伤
                if (thrower.isAlliedTo(livingVictim)) {
                    event.setCanceled(true);
                }
                // 女仆本来就不会主动攻击玩家（IAttackTask#canAttack 直接排除 Player），
                // 弹幕那边也早就是"女仆射出的弹幕不能伤害玩家" —— 只有箭矢这条路漏了。
                // 单机玩家没有队伍，isAlliedTo 恒为 false，于是会被自己的女仆射中。
                if (thrower instanceof EntityMaid && livingVictim instanceof Player) {
                    event.setCanceled(true);
                }
            }
            Identifier registryName = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
            if (MaidConfig.MAID_RANGED_ATTACK_IGNORE.get().contains(registryName.toString())) {
                event.setCanceled(true);
            }
        }
    }
}
