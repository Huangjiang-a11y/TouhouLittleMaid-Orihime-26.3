package com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity;

import com.github.tartaricacid.touhoulittlemaid.block.BlockJoy;
import com.github.tartaricacid.touhoulittlemaid.blockentity.BlockEntityJoy;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity.state.JoyRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class JoyRenderer<T extends BlockEntityJoy> implements BlockEntityRenderer<T, JoyRenderState> {
    private final SimpleBedrockModel<Unit> model;
    private final Identifier texture;

    public JoyRenderer(Identifier model, Identifier texture) {
        this.model = InternalBedrockModelRegistry.getModel(model);
        this.texture = texture;
    }

    @Override
    public JoyRenderState createRenderState() {
        return new JoyRenderState();
    }

    @Override
    public void extractRenderState(T entity, JoyRenderState state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        state.facing = entity.getBlockState().getValue(BlockJoy.FACING);
    }

    @Override
    public void submit(JoyRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.rotate(Axis.ZN.rotationDegrees(180));
        poseStack.rotate(Axis.YN.rotationDegrees(180 - state.facing.get2DDataValue() * 90));
        collector.submitModel(
                         this.model, Unit.INSTANCE, poseStack, RenderTypes.entityCutout(this.texture),
                         state.lightCoords, OverlayTexture.NO_OVERLAY, 0
                 );
                 if (state.breakProgress != null) {
                     // 26.3：方块破坏进度改由独立的 submitCrumblingOverlay 提交
                     collector.submitCrumblingOverlay(this.model, Unit.INSTANCE, poseStack, RenderTypes.entityCutout(this.texture), state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
                 }
        poseStack.popPose();
    }

    /**
     * 26.3：返回 false（等价接口默认）＝ 只在该方块所在区块可见时提交，
     * 省掉不可见时每帧的提取+提交。这里可以关（2026-10-07 真机结论）：
     * 坐垫模型由数据包/资源包自定义，被剔除时玩家本来也看不到，上面那条"半截消失 / 被剔除模组整块吞掉"的影响
     * 实测几乎为零，权衡下取性能。
     */
    @Override
    public boolean shouldRenderOffScreen() {
        return false;
    }

    // TODO
//    @Override
//    public AABB getRenderBoundingBox(T te) {
//        return RenderHelper.getAABB(
//                te.getBlockPos().offset(-2, 0, -2),
//                te.getBlockPos().offset(2, 1, 2)
//        );
//    }
}
