package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.client;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz.TacCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 基岩模型侧：持枪时的手臂姿态（26.3 的手臂是 BedrockPart，直接改 xRot/yRot）。
 */
public class GunBaseAnimation {
    public static boolean onHoldGun(EntityMaid maid, @Nullable BedrockPart armLeft, @Nullable BedrockPart armRight) {
        if (maid == null) {
            return false;
        }
        ItemStack handItem = maid.getMainHandItem();
        IGun gun = IGun.getIGunOrNull(handItem);
        if (gun == null) {
            return false;
        }
        Identifier gunId = gun.getGunId(handItem);

        // 因为现在还没有 minigun 的专属标签，故只能用特判
        if (gunId != null && gunId.equals(TacCompat.MINIGUN_ID)) {
            if (armLeft != null) {
                armLeft.xRot = -1.45f;
                armLeft.yRot = 1f;
            }
            if (armRight != null) {
                armRight.xRot = 0.75f;
                armRight.yRot = 0;
            }
            return true;
        }

        if (armLeft != null) {
            armLeft.xRot = -1.75f;
            armLeft.yRot = 0.5f;
        }
        if (armRight != null) {
            armRight.xRot = -1.65f;
            armRight.yRot = -0.174f;
        }
        return true;
    }
}
