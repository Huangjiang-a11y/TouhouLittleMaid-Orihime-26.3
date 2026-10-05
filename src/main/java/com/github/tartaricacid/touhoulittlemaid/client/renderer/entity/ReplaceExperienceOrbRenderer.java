package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ExperienceOrbRenderer;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;

/**
 * 经验球替换成"得分道具"贴图（1.21.1 ReplaceExperienceOrbRenderer 的 26.3 版）
 * <p>
 * 配置关掉时直接委托给原版渲染器。几何形状照抄移植版 {@code EntityPowerPointRenderer} 的 billboard 写法。
 */
public class ReplaceExperienceOrbRenderer extends EntityRenderer<ExperienceOrb, ExperienceOrbRenderState> {
    private static final Identifier POINT_ITEM_TEXTURE = IdentifierUtil.modLoc("textures/entity/point_item.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCull(POINT_ITEM_TEXTURE);

    private final ExperienceOrbRenderer vanillaRenderer;

    public ReplaceExperienceOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
        this.vanillaRenderer = new ExperienceOrbRenderer(context);
    }

    @Override
    public ExperienceOrbRenderState createRenderState() {
        return new ExperienceOrbRenderState();
    }

    @Override
    public void extractRenderState(ExperienceOrb entity, ExperienceOrbRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        this.vanillaRenderer.extractRenderState(entity, state, partialTick);
    }

    @Override
    protected int getBlockLightLevel(ExperienceOrb entity, BlockPos pos) {
        return Mth.clamp(super.getBlockLightLevel(entity, pos) + 7, 0, 15);
    }

    @Override
    public void submit(ExperienceOrbRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        if (!VanillaConfig.REPLACE_XP_TEXTURE.get()) {
            this.vanillaRenderer.submit(state, poseStack, collector, camera);
            return;
        }
        int icon = state.icon;
        double texPos1 = (double) (icon % 4 * 16) / 64.0;
        double texPos2 = (double) (icon % 4 * 16 + 16) / 64.0;
        double texPos3 = (double) (icon / 4 * 16) / 64.0;
        double texPos4 = (double) (icon / 4 * 16 + 16) / 64.0;

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.rotate(camera.orientation);
        poseStack.scale(0.3F, 0.3F, 0.3F);
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -0.5, -0.25, texPos1, texPos4, state.lightCoords);
            vertex(buffer, pose, 0.5, -0.25, texPos2, texPos4, state.lightCoords);
            vertex(buffer, pose, 0.5, 0.75, texPos2, texPos3, state.lightCoords);
            vertex(buffer, pose, -0.5, 0.75, texPos1, texPos3, state.lightCoords);
        });
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, double x, double y,
                               double texU, double texV, int lightCoords) {
        buffer.addVertex(pose, (float) x, (float) y, 0.0F)
                .setColor(255, 255, 255, 128)
                .setUv((float) texU, (float) texV)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
