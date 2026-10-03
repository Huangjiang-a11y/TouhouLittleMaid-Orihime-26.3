package cn.sh1rocu.touhoulittlemaid.mixin.client;

import cn.sh1rocu.touhoulittlemaid.api.event.RenderHandEvent;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.3 把第一人称手部渲染从 {@code ItemInHandRenderer} 搬到了
 * {@link FirstPersonHandsAndItemsRenderer}：原 {@code submitArmWithItem} 仍在，
 * 但多了两个渲染状态参数（PlayerRenderState / FirstPersonHandsAndItemsRenderState）。
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    @WrapWithCondition(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private boolean tlm$renderHand(FirstPersonHandsAndItemsRenderer instance, PlayerRenderState playerRenderState, FirstPersonHandsAndItemsRenderState handsAndItemsState, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
        RenderHandEvent event = new RenderHandEvent(hand, poseStack, submitNodeCollector, lightCoords, frameInterp, xRot, attack, inverseArmHeight, itemStack);
        RenderHandEvent.CALLBACK.invoker().post(event);
        return !event.isCanceled();
    }
}
