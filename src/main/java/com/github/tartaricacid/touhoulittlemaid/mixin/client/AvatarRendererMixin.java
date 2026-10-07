package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.github.tartaricacid.touhoulittlemaid.api.mixin.ICarryingMaidRenderState;

/**
 * 26.3 渲染状态重构后，HumanoidModel#setupAnim 只接收渲染状态、不再有实体引用。
 * 此处趁 AvatarRenderer 提取渲染状态时，把"玩家正背着女仆"记进 AvatarRenderState，
 * 供 HumanoidModelMixin 在摆姿势时读取。
 */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void tlm$extractCarryingMaid(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        if (avatar.getFirstPassenger() instanceof EntityMaid) {
            ((ICarryingMaidRenderState) state).tlm$setCarryingMaid(true);
        }
    }
}
