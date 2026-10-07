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
     * 26.3：返回 true ＝ 不参与"按区块可见性/遮挡"的过滤（走方块实体提交两趟里的"全局列表"那趟），
     * 64 格内每帧都会提取+提交。这里必须开（2026-10-07 真机结论）：
     *   · 祭坛实测 11.7×7.3×10.9 格，模型远大于方块自身范围 —— 若只在所在区块可见时提交，
     *     站在区块边界往另一侧看会"半截消失"；
     *   · 更狠的是剔除模组：EntityCulling（异步视线路径追踪，比 Sodium 的区块可见性激进得多）
     *     与 MoreCulling 都会按"方块实体所在方块是否被遮挡"来剔除 —— 锚点方块被挡住时，
     *     整个模型直接不渲染（真机反馈："没看到方块直接不渲染"）。
     * 代价是它不可见时也在提交（棋盘/祭坛/垫子几何简单，代价可接受）。
     */
    @Override
    public boolean shouldRenderOffScreen() {
        return true;
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
