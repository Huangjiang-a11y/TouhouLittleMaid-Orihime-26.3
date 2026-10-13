package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client;

import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.LoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.AnimationEvent;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * 枪械动画（GeckoLib 侧）：按 TACZ 的射击冷却 / 瞄准进度 / 换弹进度选动画，
 * 并按枪型（手枪 / 火箭筒 / 其余步枪）拼动画名。资源在 tac.animation.json。
 */
public class GunGeckoAnimation {
    /** 基础动作（idle/walk/run…）优先用 tac: 前缀的版本 */
    public static PlayState playGunMainAnimation(AnimationEvent<GeckoMaidEntity<?>> event, String animationName, LoopType loopType) {
        EntityMaid maid = event.getAnimatableEntity().getMaid();
        if (isMaidCarrying(maid)) {
            return null;
        }
        return playAnimation(event, "tac:" + animationName, loopType);
    }

    public static PlayState playGunHoldAnimation(AnimationEvent<GeckoMaidEntity<?>> event, ItemStack heldItem) {
        IGun gun = IGun.getIGunOrNull(heldItem);
        if (gun == null) {
            return PlayState.STOP;
        }
        Optional<CommonGunIndex> indexOptional = TimelessAPI.getCommonGunIndex(gun.getGunId(heldItem));
        if (indexOptional.isEmpty()) {
            return PlayState.STOP;
        }
        CommonGunIndex gunIndex = indexOptional.get();
        String weaponType = gunIndex.getType();
        EntityMaid maid = event.getAnimatableEntity().getMaid();
        if (maid == null || isMaidCarrying(maid)) {
            return PlayState.STOP;
        }

        IGunOperator operator = IGunOperator.fromLivingEntity(maid);
        long fireTick = operator.getSynShootCoolDown();

        // 游泳/攀爬
        if (!maid.isSwimming() && maid.getPose() == Pose.SWIMMING) {
            if (Math.abs(event.getLimbSwingAmount()) > 0.05) {
                return getGunTypeAnimation(event, weaponType, "tac:climb:");
            }
            return fireTick > 0 ? getGunTypeAnimation(event, weaponType, "tac:climbing:fire:")
                    : getGunTypeAnimation(event, weaponType, "tac:climbing:");
        }

        // 换弹
        if (operator.getSynReloadState().getCountDown() > 0) {
            return getGunTypeAnimation(event, weaponType, "tac:reload:");
        }

        // 瞄准
        if (operator.getSynAimingProgress() > 0) {
            return fireTick > 0 ? getGunTypeAnimation(event, weaponType, "tac:aim:fire:")
                    : getGunTypeAnimation(event, weaponType, "tac:aim:");
        }

        // 持枪
        if (maid.onGround() && maid.isSprinting()) {
            return getGunTypeAnimation(event, weaponType, "tac:run:");
        }
        return fireTick > 0 ? getGunTypeAnimation(event, weaponType, "tac:hold:fire:")
                : getGunTypeAnimation(event, weaponType, "tac:hold:");
    }

    private static PlayState getGunTypeAnimation(AnimationEvent<GeckoMaidEntity<?>> event, String weaponType, String prefix) {
        if (isType(weaponType, GunTabType.PISTOL)) {
            return playLoopAnimation(event, prefix + "pistol");
        }
        if (isType(weaponType, GunTabType.RPG)) {
            return playLoopAnimation(event, prefix + "rpg");
        }
        return playLoopAnimation(event, prefix + "rifle");
    }

    private static PlayState playLoopAnimation(AnimationEvent<?> event, String animationName) {
        return playAnimation(event, animationName, LoopType.LOOP);
    }

    private static PlayState playAnimation(AnimationEvent<?> event, String animationName, LoopType loopType) {
        event.getCodedController().setAnimation(animationName, loopType);
        return PlayState.CONTINUE;
    }

    private static boolean isType(String type, GunTabType tabType) {
        return type.equals(tabType.name().toLowerCase(Locale.ENGLISH));
    }

    private static boolean isMaidCarrying(EntityMaid maid) {
        return maid != null && maid.getVehicle() instanceof Player;
    }
}
