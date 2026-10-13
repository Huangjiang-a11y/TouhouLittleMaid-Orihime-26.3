package com.github.tartaricacid.touhoulittlemaid.compat.gun.common;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.LoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.AnimationEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 枪械客户端工具。方法签名已按 26.3 的架构重建（旧版的 IMaid / ModelRendererWrapper /
 * ILocationModel / MultiBufferSource 在 26.3 都不存在了）：
 * <ul>
 *     <li>动画：{@code AnimationManager} 调用（gecko 侧）</li>
 *     <li>手臂姿态：{@code MaidBaseAnimation} 调用（基岩侧）</li>
 *     <li>背枪渲染 / 手持位移：{@code LayerMaidBackItem}、{@code LayerMaidHeldItem} 调用</li>
 * </ul>
 * TODO 方法体待按 26.3 的动画与渲染管线补全（见 tacz-wip 分支说明）。
 */
public class GunClientUtil {
    @Nullable
    public static PlayState playGunMainAnimation(EntityMaid maid, AnimationEvent<GeckoMaidEntity<?>> event,
                                                 String animationName, LoopType loopType) {
        return null;
    }

    @Nullable
    public static PlayState playGunHoldAnimation(ItemStack mainHandItem, AnimationEvent<GeckoMaidEntity<?>> event) {
        return null;
    }

    public static boolean onHoldGun(EntityMaid maid, @Nullable BedrockPart armLeft, @Nullable BedrockPart armRight) {
        return false;
    }

    public static void renderBackGun(PoseStack poseStack, SubmitNodeCollector buffer, int packedLight,
                                     ItemStack stack, EntityMaid maid) {
    }

    public static void addItemTranslate(PoseStack poseStack, ItemStack itemStack, boolean isLeft) {
    }
}
