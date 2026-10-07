package com.github.tartaricacid.touhoulittlemaid.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.github.tartaricacid.touhoulittlemaid.api.mixin.ICarryingMaidRenderState;

/**
 * 玩家背着女仆（女仆为玩家第一乘客）时，双手摆出托举姿态（1.21.1 旧功能）。
 * 上游 26.x 因 HumanoidModel#setupAnim 改收渲染状态而整条注释禁用，
 * 此处基于 AvatarRendererMixin 写入的标记在新 API 下恢复。
 */
@Mixin(HumanoidModel.class)
public class HumanoidModelMixin<T extends HumanoidRenderState> {
    @Shadow
    @Final
    public ModelPart leftArm;
    @Shadow
    @Final
    public ModelPart rightArm;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("TAIL"))
    private void tlm$setCarryingMaidArmPose(T state, CallbackInfo ci) {
        if (state instanceof ICarryingMaidRenderState carryingMaid && carryingMaid.tlm$isCarryingMaid()) {
            leftArm.xRot = (float) Math.toRadians(-65);
            leftArm.yRot = (float) Math.toRadians(10);
            rightArm.xRot = (float) Math.toRadians(-65);
            rightArm.yRot = (float) Math.toRadians(-10);
        }
    }
}
