package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.VanillaConfig;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;

/**
 * 史莱姆替换成油库里模型（1.21.1 EntityMarisaYukkuriSlimeRender 的 26.3 版）
 * <p>
 * 配置关掉时委托给原版 {@link SlimeRenderer}，与官方行为一致。
 */
public class EntityMarisaYukkuriSlimeRender extends MobRenderer<MagmaCube, SlimeRenderState, EntityModel<SlimeRenderState>> {
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/entity/marisa_yukkuri.png");

    private final MagmaCubeRenderer vanillaRenderer;

    public EntityMarisaYukkuriSlimeRender(EntityRendererProvider.Context context) {
        super(context, InternalBedrockModelRegistry.getEntityModel(InternalBedrockModelRegistry.MARISA_YUKKURI), 0.25F);
        this.vanillaRenderer = new MagmaCubeRenderer(context);
    }

    @Override
    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }

    @Override
    public void extractRenderState(MagmaCube entity, SlimeRenderState state, float partialTick) {
        this.vanillaRenderer.extractRenderState(entity, state, partialTick);
    }

    @Override
    public void submit(SlimeRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (VanillaConfig.REPLACE_SLIME_MODEL.get()) {
            super.submit(state, poseStack, collector, camera);
        } else {
            this.vanillaRenderer.submit(state, poseStack, collector, camera);
        }
    }

    @Override
    public Identifier getTextureLocation(SlimeRenderState state) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(MagmaCube entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    protected float getShadowRadius(SlimeRenderState state) {
        return 0.25F * state.size;
    }

    @Override
    protected void scale(SlimeRenderState state, PoseStack poseStack) {
        poseStack.scale(0.999F, 0.999F, 0.999F);
        poseStack.translate(0.0F, 0.001F, 0.0F);
        float slimeSize = state.size;
        float tmp = state.squish / (slimeSize * 0.5F + 1.0F);
        float scale = 1.0F / (tmp + 1.0F);
        poseStack.scale(scale * slimeSize, 1.0F / scale * slimeSize, scale * slimeSize);
    }
}
