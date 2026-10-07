package com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity;

import com.github.tartaricacid.touhoulittlemaid.blockentity.BlockEntityAltar;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity.state.AltarRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AltarRenderer implements BlockEntityRenderer<BlockEntityAltar, AltarRenderState> {
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/altar.png");

    private final SimpleBedrockModel<Unit> model;
    private final ItemModelResolver itemModelResolver;

    public AltarRenderer(BlockEntityRendererProvider.Context context) {
        this.model = InternalBedrockModelRegistry.getModel(InternalBedrockModelRegistry.ALTAR);
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public AltarRenderState createRenderState() {
        return new AltarRenderState();
    }

    @Override
    public void extractRenderState(BlockEntityAltar altar, AltarRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(altar, state, partialTicks, cameraPosition, breakProgress);

        state.renderModel = altar.isRender();
        state.direction = altar.getDirection();
        state.canPlaceItem = altar.isCanPlaceItem();

        ItemStack stack = state.canPlaceItem ? altar.getStorageItem() : ItemStack.EMPTY;
        state.hasItem = !stack.isEmpty();
        if (state.hasItem) {
            state.itemRenderState.clear();
            itemModelResolver.updateForTopItem(
                    state.itemRenderState, stack, ItemDisplayContext.GROUND,
                    altar.getLevel(), null, (int) altar.getBlockPos().asLong()
            );
        }
    }

    @Override
    public void submit(AltarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.renderModel) {
            poseStack.pushPose();
            this.setTranslateAndPose(state.direction, poseStack);
            poseStack.rotate(Axis.ZN.rotationDegrees(180));
            collector.submitModel(
                             this.model, Unit.INSTANCE, poseStack, RenderTypes.entityTranslucent(TEXTURE),
                             state.lightCoords, OverlayTexture.NO_OVERLAY, 0
                     );
                     if (state.breakProgress != null) {
                         // 26.3：方块破坏进度改由独立的 submitCrumblingOverlay 提交
                         collector.submitCrumblingOverlay(this.model, Unit.INSTANCE, poseStack, RenderTypes.entityTranslucent(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
                     }
            poseStack.popPose();
        }

        if (state.hasItem) {
            poseStack.pushPose();
            double time = (System.currentTimeMillis() + state.blockPos.asLong()) % 3600;
            poseStack.translate(0.5, 1.25 + Math.sin(time / 1800 * Math.PI) * 0.1, 0.5);
            poseStack.rotate(Axis.YP.rotationDegrees((float) time / 10));
            state.itemRenderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    private void setTranslateAndPose(Direction direction, PoseStack poseStack) {
        switch (direction) {
            case SOUTH:
                poseStack.translate(1, -1.5, -3);
                poseStack.rotate(Axis.YP.rotationDegrees(180));
                break;
            case EAST:
                poseStack.translate(-3, -1.5, 0);
                poseStack.rotate(Axis.YP.rotationDegrees(270));
                break;
            case WEST:
                poseStack.translate(4, -1.5, 1);
                poseStack.rotate(Axis.YP.rotationDegrees(90));
                break;
            case NORTH:
            default:
                poseStack.translate(0, -1.5, 4);
        }
    }

    /**
     * 26.3：统一返回 false（等价接口默认）＝ 只在该方块所在区块可见时提交，
     * 省掉不可见时每帧的提取+提交。
     * <p>
     * 2026-10-07 真机结论：**这个标志挡不住剔除模组**。EntityCulling / MoreCulling 挂在
     * "真正渲染这个方块实体"那一步按遮挡剔除，不看提交走哪一趟 —— 实测即便这里返回 true，
     * 锚点方块被挡住时模型照样整个不渲染，只是白多一份每帧提取+提交。故全部关闭，
     * 可见性交给区块可见性与剔除模组本身（装了剔除模组时"方块被挡→模型不出现"是预期行为，
     * 想让它一定显示请在剔除模组的配置里关掉方块实体剔除）。
     */
    @Override
    public boolean shouldRenderOffScreen() {
        return false;
    }

    // TODO
//    @Override
//    public AABB getRenderBoundingBox(BlockEntityAltar te) {
//        return RenderHelper.getAABB(
//                te.getBlockPos().offset(-9, -5, -9),
//                te.getBlockPos().offset(9, 5, 9)
//        );
//    }
}
