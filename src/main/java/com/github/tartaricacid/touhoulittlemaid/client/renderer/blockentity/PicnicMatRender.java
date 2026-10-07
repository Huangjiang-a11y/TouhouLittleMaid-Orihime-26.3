package com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity;

import com.github.tartaricacid.touhoulittlemaid.block.BlockPicnicMat;
import com.github.tartaricacid.touhoulittlemaid.blockentity.BlockEntityPicnicMat;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.blockentity.state.PicnicMatRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PicnicMatRender implements BlockEntityRenderer<BlockEntityPicnicMat, PicnicMatRenderState> {
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/picnic_mat.png");

    private final SimpleBedrockModel<Unit> model;
    private final ItemModelResolver resolver;

    public PicnicMatRender(BlockEntityRendererProvider.Context context) {
        this.model = InternalBedrockModelRegistry.getModel(InternalBedrockModelRegistry.PICNIC_MAT);
        this.resolver = context.itemModelResolver();
    }

    @Override
    public PicnicMatRenderState createRenderState() {
        return new PicnicMatRenderState();
    }

    @Override
    public void extractRenderState(BlockEntityPicnicMat picnicMat, PicnicMatRenderState state, float partialTick,
                                   Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(picnicMat, state, partialTick, cameraPos, breakProgress);
        state.isCenter = picnicMat.getBlockState().getValue(BlockPicnicMat.PART).isCenter();
        state.facing = picnicMat.getBlockState().getValue(BlockPicnicMat.FACING);
        for (int i = 0; i < 9; i++) {
            state.slotItems[i] = picnicMat.getStorageItem(i);
        }
    }

    @Override
    public void submit(PicnicMatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.isCenter) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.rotate(Axis.ZN.rotationDegrees(180));
        poseStack.rotate(Axis.YN.rotationDegrees(180 - state.facing.get2DDataValue() * 90));

        // 渲染食物物品
        renderFoodItem(state.slotItems[3], -0.6f, -1.5f, 1.4125f, poseStack, collector, state);
        renderFoodItem(state.slotItems[4], 0.15f, -1.2f, 1.4125f, poseStack, collector, state);
        renderFoodItem(state.slotItems[5], 0.55f, -1.6f, 1.4125f, poseStack, collector, state);

        renderFoodItem(state.slotItems[6], -0.5f, 1.65f, 1.4125f, poseStack, collector, state);
        renderFoodItem(state.slotItems[7], 0.375f, 1.575f, 1.4125f, poseStack, collector, state);
        renderFoodItem(state.slotItems[8], -0.05f, 1.2f, 1.25f, poseStack, collector, state);

        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
            // 根据槽位内容控制模型部件可见性
            this.model.getPart("basketHide").visible = !state.slotItems[0].isEmpty();
            this.model.getPart("breadHide").visible = !state.slotItems[1].isEmpty();
            this.model.getPart("cakeHide").visible = !state.slotItems[2].isEmpty();

            // 渲染底座模型
            poseStack.pushPose();
            poseStack.last().set(pose);
            model.renderToBuffer(poseStack, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
        });

        poseStack.popPose();
    }

    private void renderFoodItem(ItemStack storageItem, float x, float y, float z,
                                PoseStack poseStack, SubmitNodeCollector collector,
                                PicnicMatRenderState state) {
        if (storageItem.isEmpty()) {
            return;
        }

        int count = storageItem.getCount();
        Level level = Minecraft.getInstance().level;

        poseStack.pushPose();
        poseStack.rotate(Axis.XN.rotationDegrees(90));
        poseStack.translate(x, y, z);
        poseStack.scale(0.4f, 0.4f, 0.4f);

        ItemStackRenderState itemRenderState = new ItemStackRenderState();
        resolver.updateForTopItem(itemRenderState, storageItem, ItemDisplayContext.FIXED, level, null, 0);
        itemRenderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        if (count >= 10) {
            int stackCount = count / 10;
            for (int i = 0; i < stackCount; i++) {
                poseStack.translate(Math.sin(i) * 0.05, Math.cos(i) * 0.03, -0.07);
                poseStack.rotate(Axis.ZN.rotationDegrees((float) Math.cos(i) * 60));

                ItemStackRenderState stackedRenderState = new ItemStackRenderState();
                resolver.updateForTopItem(stackedRenderState, storageItem, ItemDisplayContext.FIXED, level, null, 0);
                stackedRenderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            }
        }

        poseStack.popPose();
    }

    /**
     * 26.3：返回 true ＝ 不参与"按区块可见性/遮挡"的过滤（走方块实体提交两趟里的"全局列表"那趟），
     * 64 格内每帧都会提取+提交。这里必须开（2026-10-07 真机结论）：
     *   · 野餐垫实测 3.9×4.5 格，模型远大于方块自身范围 —— 若只在所在区块可见时提交，
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
//    public AABB getRenderBoundingBox(BlockEntityPicnicMat blockEntity) {
//        BlockState blockState = blockEntity.getBlockState();
//        BlockPos pos = blockEntity.getBlockPos();
//        if (blockState.getValue(BlockPicnicMat.PART).isCenter()) {
//            return RenderHelper.getAABB(
//                    pos.offset(-3, 0, -3),
//                    pos.offset(3, 1, 3)
//            );
//        }
//        return new AABB(pos);
//    }
}
