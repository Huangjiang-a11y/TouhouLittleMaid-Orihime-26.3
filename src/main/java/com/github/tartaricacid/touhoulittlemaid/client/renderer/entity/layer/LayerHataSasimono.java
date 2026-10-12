package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.layer;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.EntityMaidRenderer;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHataSasimono;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import static com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid.MOD_ID;
import static com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry.MAID_BANNER;
import static net.minecraft.client.renderer.blockentity.BannerRenderer.submitPatterns;
import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

/**
 * 旗指物渲染层（基岩模型）。复用背旗的模型（旗杆 + 旗面），
 * 样式由 {@code state.sasimonoStyle} 决定（目前是不同底色）。
 * <p>
 * 定位：优先挂在 {@code body} 骨上（跟随躯干动作），没有 body 骨的模型退回模型 root。
 * 偏移沿用现有背旗层 {@link LayerMaidBanner} 的参数，保证位置一致。
 */
public class LayerHataSasimono extends RenderLayer<EntityMaidRenderState, EntityMaidModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/bedrock/entity/maid_banner.png");
    private final SimpleBedrockModel<Unit> banner;
    private final SpriteGetter sprites;
    private final BannerFlagModel flag;
    private static boolean logged = false;

    public LayerHataSasimono(EntityMaidRenderer renderer, EntityRendererProvider.Context context) {
        super(renderer);
        this.banner = InternalBedrockModelRegistry.getModel(MAID_BANNER);
        this.flag = new BannerFlagModel(context.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.sprites = context.getSprites();
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNode, int light,
                       EntityMaidRenderState state, float yRot, float xRot) {
        DyeColor color = ItemHataSasimono.getFlagColor(state.sasimonoStyle);
        if (color == null) {
            return;
        }
        poseStack.pushPose();

        // 定位：优先 body 骨（旗子随躯干摆动），无 body 骨则退回 root
        BedrockPart body = this.getParentModel().getModelMap().get("body");
        if (body == null) {
            body = this.getParentModel().getModelMap().get("Body");
        }
        if (body != null) {
            body.translateAndRotate(poseStack);
            // 抵消 body 骨的 pivot 位移：静止时落点与已验证的背旗层（挂 root）完全一致，
            // 躯干转动/摆动时旗子仍跟着身体走。
            poseStack.translate(-body.x / 16.0F, -body.y / 16.0F, -body.z / 16.0F);
            if (!logged && state.sasimonoStyle > 0) {
                logged = true;
                TouhouLittleMaid.LOGGER.info("[Sasimono] 渲染：挂 body 骨 pivot=({},{},{}) style={}",
                        body.x, body.y, body.z, state.sasimonoStyle);
            }
        } else if (this.getParentModel().root() instanceof BedrockPart part) {
            part.translateAndRotate(poseStack);
            if (!logged && state.sasimonoStyle > 0) {
                logged = true;
                TouhouLittleMaid.LOGGER.info("[Sasimono] 渲染：无 body 骨，退回 root，style={}", state.sasimonoStyle);
            }
        }

        poseStack.translate(0, -0.25, 0.25);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.rotate(Axis.XN.rotationDegrees(5));

        // 旗杆
        submitNode.submitModel(banner, Unit.INSTANCE, poseStack, RenderTypes.entityCutout(TEXTURE),
                light, NO_OVERLAY, state.outlineColor);

        // 旗面 + 底色
        poseStack.rotate(Axis.YN.rotationDegrees(90));
        poseStack.translate(0.75, 0.2, 0.1);
        SpriteId sprite = Sheets.BANNER_BASE;
        submitNode.submitModel(flag, 0f, poseStack, light, NO_OVERLAY, -1, sprite, sprites, state.outlineColor);
        submitPatterns(sprites, poseStack, submitNode, light, NO_OVERLAY, flag,
                0f, true, color, BannerPatternLayers.EMPTY);

        poseStack.popPose();
    }
}
