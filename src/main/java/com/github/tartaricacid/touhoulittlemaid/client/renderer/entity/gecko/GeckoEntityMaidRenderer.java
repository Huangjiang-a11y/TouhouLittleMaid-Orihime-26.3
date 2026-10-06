package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.AddMaidLayerEvent;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.gecko.layer.*;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.GeoReplacedEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class GeckoEntityMaidRenderer extends GeoReplacedEntityRenderer<EntityMaid, EntityMaidRenderState, GeckoMaidRenderData> {
    public GeckoEntityMaidRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.addLayer(new GeckoLayerMaidHeld());
        this.addLayer(new GeckoLayerMaidBipedHead(context));
        this.addLayer(new GeckoLayerMaidBackpack());
        this.addLayer(new GeckoLayerMaidBackItem());
        this.addLayer(new GeckoLayerMaidBanner(context));

        AddMaidLayerEvent.GECKO.invoker().post(new AddMaidLayerEvent.Gecko(context, this));
    }

    @Override
    public @NonNull EntityMaidRenderState createRenderState() {
        return new EntityMaidRenderState();
    }

    @Override
    public @Nullable GeckoMaidRenderData getGeckoRenderData(EntityMaidRenderState state) {
        if (state.geckoUpdateTask != null) {
            return state.geckoUpdateTask.getResult();
        }
        return null;
    }

    @Override
    protected void setupRotations(@NonNull EntityMaidRenderState state, @NonNull PoseStack poseStack, float bodyRot, float entityScale) {
        var data = getGeckoRenderData(state);
        if (data != null) {
            var ctx = data.ctx;
            if ((ctx.level() || ctx.irisShadow()) && !Float.isNaN(data.climbRotation)) {
                bodyRot = data.climbRotation;
            }
        }
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        // 玩家用鞍抱起女仆时，基岩模型那条分支（EntityMaidRenderer#setupRotations）会把女仆摆到肩上；
        // Gecko 模型此前整条跳过，导致女仆以坐姿留在玩家身前（上游 issue #35 第 1 点）。
        // 两个渲染器都是在未镜像的实体坐标系里做这一步，故数值可直接复用。
        if (state.playerVehicle) {
            poseStack.translate(-0.375, 0.8325, 0.375);
            poseStack.rotate(Axis.ZN.rotationDegrees(65));
            poseStack.rotate(Axis.YN.rotationDegrees(-80));
        }
    }

    @Override
    protected void scale(EntityMaidRenderState state, PoseStack poseStack) {
        var scale = state.modelInfo.getRenderEntityScale();
        poseStack.scale(scale, scale, scale);
    }
}
