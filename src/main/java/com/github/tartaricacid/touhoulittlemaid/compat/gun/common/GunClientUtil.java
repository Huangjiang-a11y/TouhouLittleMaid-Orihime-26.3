package com.github.tartaricacid.touhoulittlemaid.compat.gun.common;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client.GunBaseAnimation;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client.GunGeckoAnimation;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.LoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.AnimationEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.item.IGun;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 枪械客户端工具（方法签名按 26.3 架构重建）。背枪显示走
 * {@code EntityMaidRenderStateCompatMixin} + 现成的 LayerMaidBackItem / GeckoLayerMaidBackItem，
 * 不在这里重复实现。
 */
public class GunClientUtil {
    @Nullable
    public static PlayState playGunMainAnimation(EntityMaid maid, AnimationEvent<GeckoMaidEntity<?>> event,
                                                 String animationName, LoopType loopType) {
        if (TacCompat.isInstalled()) {
            return GunGeckoAnimation.playGunMainAnimation(event, animationName, loopType);
        }
        return null;
    }

    @Nullable
    public static PlayState playGunHoldAnimation(ItemStack mainHandItem, AnimationEvent<GeckoMaidEntity<?>> event) {
        if (TacCompat.isInstalled() && TacCompat.isGun(mainHandItem)) {
            return GunGeckoAnimation.playGunHoldAnimation(event, mainHandItem);
        }
        return null;
    }

    public static boolean onHoldGun(EntityMaid maid, @Nullable BedrockPart armLeft, @Nullable BedrockPart armRight) {
        return TacCompat.isInstalled() && GunBaseAnimation.onHoldGun(maid, armLeft, armRight);
    }

    /** 手持枪械时的位移（加特林额外转个角度） */
    public static void addItemTranslate(PoseStack poseStack, ItemStack itemStack, boolean isLeft) {
        if (!isLeft && itemStack.getItem() instanceof IGun gun) {
            poseStack.translate(0, -0.125, 0);
            if (TacCompat.MINIGUN_ID.equals(gun.getGunId(itemStack))) {
                poseStack.rotate(Axis.ZP.rotationDegrees(20));
                poseStack.rotate(Axis.XP.rotationDegrees(50));
            }
        }
    }
}
