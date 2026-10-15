package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.event;

import com.github.tartaricacid.touhoulittlemaid.api.event.MaidHurtEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import java.util.List;

public class GunHurtMaidEvent {
    /**
     * 不伤害自己
     */
    public void onMaidHurt(MaidHurtEvent event) {
        DamageSource source = event.getSource();
        EntityMaid maid = event.getMaid();
        if (maid.getOwner() == null) {
            return;
        }
        if (isBulletDamage(source)) {
            event.setCanceled(true);
        }
    }

    /**
     * 避免通过事件引入伤害的附属模组打死玩家
     */
    public void onGunHurt(EntityHurtByGunEvent.Pre event) {
        Entity hurtEntity = event.getHurtEntity();
        LivingEntity attacker = event.getAttacker();

        // 不伤害自己 x2
        if (attacker instanceof EntityMaid) {
            // 女仆不会主动攻击玩家（IAttackTask#canAttack 直接排除 Player），弹幕那边也是
            // "女仆射出的弹幕不能伤害玩家"，所以枪械同样不该伤到任何玩家。
            // 原来只挡 isAlliedTo（同队伍），单机玩家没有队伍 → 会被自己的女仆打死。
            if (hurtEntity instanceof Player) {
                event.setCanceled(true);
            }
        }

        // 不伤害他人 x2
        if (attacker instanceof Player player) {
            // 主人和同 Team 玩家免伤
            if (hurtEntity instanceof EntityMaid maid && maid.isAlliedTo(player)) {
                event.setCanceled(true);
            }
        }
    }

    public void onExplosionDetonateEvent(Level world, Explosion explosion, List<Entity> entities, double diameter) {
        if (explosion.getDirectSourceEntity() instanceof EntityKineticBullet) {
            entities.removeIf(e -> e instanceof EntityMaid);
        }
    }

    private boolean isBulletDamage(DamageSource source) {
        if (source.is(ModDamageTypes.BULLETS_TAG)) {
            return true;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            Entity directEntity = source.getDirectEntity();
            return directEntity instanceof EntityKineticBullet;
        }
        return false;
    }
}
