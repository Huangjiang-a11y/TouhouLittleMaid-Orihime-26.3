package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.layer;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.SimpleBedrockModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.GeckoMaidRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoLayerRenderer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoLocatorType;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHataSasimono;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
 * 旗指物渲染层（GeckoLib 模型）。定位与偏移完全沿用现成的背旗层 {@link GeckoLayerMaidBanner}
 * （BACKPACK 定位骨 + 同一组平移/缩放/旋转），保证落点一致；样式由 {@code state.sasimonoStyle} 决定。
 */
public class GeckoLayerHataSasimono implements GeoLayerRenderer<EntityMaidRenderState, GeckoMaidRenderData> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MOD_ID, "textures/bedrock/entity/maid_banner.png");
    private final SimpleBedrockModel<Unit> banner;
    private final SpriteGetter sprites;
    private final BannerFlagModel flag;

    public GeckoLayerHataSasimono(EntityRendererProvider.Context context) {
        this.banner = InternalBedrockModelRegistry.getModel(MAID_BANNER);
        this.flag = new BannerFlagModel(context.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.sprites = context.getSprites();
    }

    @Override
    public void submit(SubmitNodeCollector submitNode, PoseStack poseStack, EntityMaidRenderState state,
                       GeckoMaidRenderData data, CameraRenderState camera) {
        DyeColor color = ItemHataSasimono.getFlagColor(state.sasimonoStyle);
        if (color == null) {
            return;
        }
        // 用现成背旗层同一套定位（BACKPACK 骨 + 同一组偏移），保证落点一致
        data.modelState.visitLocatorGroup(GeoLocatorType.BACKPACK, poseStack, locator -> {
            locator.translate(0, 0.75, 0.3);
            locator.scale(0.65F, -0.65F, -0.65F);
            locator.rotate(Axis.YN.rotationDegrees(180));
            locator.rotate(Axis.XN.rotationDegrees(5));

            int light = state.lightCoords;
            submitNode.submitModel(banner, Unit.INSTANCE, poseStack, RenderTypes.entityCutout(TEXTURE),
                    light, NO_OVERLAY, state.outlineColor);

            poseStack.rotate(Axis.YN.rotationDegrees(90));
            poseStack.translate(0.75, 0.2, 0.1);
            SpriteId sprite = Sheets.BANNER_BASE;
            submitNode.submitModel(flag, 0f, poseStack, light, NO_OVERLAY, -1, sprite, sprites, state.outlineColor);
            submitPatterns(sprites, poseStack, submitNode, light, NO_OVERLAY, flag,
                    0f, true, color, BannerPatternLayers.EMPTY);
        });
    }
}
