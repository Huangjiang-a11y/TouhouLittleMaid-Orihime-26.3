package com.github.tartaricacid.touhoulittlemaid.client.resource.loader;

import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityChairModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityChairRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.accessor.ResourceAccessor;
import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.CustomPackBedrockModelParser;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.ChairModelInfo;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.CustomModelPack;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoContainer;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.lang.reflect.Type;

final class ChairPackLoader {
    private static final Marker MARKER = MarkerManager.getMarker("ChairPackLoader");
    private static final Type PACK_TYPE = new TypeToken<CustomModelPack<ChairModelInfo>>() {
    }.getType();

    static void loadPack(ResourceAccessor accessor, String domain) {
        PackLoaderHelper.loadPack(
                CustomPackLoader.CHAIR_MODELS,
                accessor, domain,
                PACK_TYPE, MARKER,
                ChairPackLoader::loadChairElement
        );
    }

    private static void loadChairElement(ResourceAccessor accessor, ChairModelInfo info) throws IOException {
        if (info.isGeckoModel()) {
            loadGeckoChairModelElement(accessor, info);
        } else {
            loadChairModelElement(accessor, info);
        }
    }

    private static void loadChairModelElement(ResourceAccessor accessor, ChairModelInfo info) {
        EntityChairModel modelJson = CustomPackBedrockModelParser.loadChairModel(accessor, info.getModel());
        CustomPackLoader.registerTexture(accessor, info.getTexture());
        if (modelJson != null) {
            String id = info.getModelId().toString();
            var animations = PackLoaderHelper.<EntityChairRenderState>resolveAnimations(info);
            CustomPackLoader.CHAIR_MODELS.putModel(id, modelJson);
            CustomPackLoader.CHAIR_MODELS.putAnimation(id, animations);
            CustomPackLoader.CHAIR_MODELS.putInfo(id, info);
        }
    }

    private static void loadGeckoChairModelElement(ResourceAccessor accessor, ChairModelInfo info) throws IOException {
        CustomPackBedrockModelParser.loadGeckoModelElement(accessor, info, GeckoContainer.Type.CHAIR);
        // 同 MaidPackLoader：Gecko 容器/动画拿不到时，用同一份 .geo.json 的基岩静态姿势兜底，
        // 否则椅子在预览框/图标/世界里都是整块透明。
        String id = info.getModelId().toString();
        EntityChairModel fallbackModel = CustomPackBedrockModelParser.loadChairModel(accessor, info.getModel());
        if (fallbackModel != null) {
            CustomPackLoader.CHAIR_MODELS.putModel(id, fallbackModel);
            CustomPackLoader.CHAIR_MODELS.putAnimation(id, PackLoaderHelper.<EntityChairRenderState>resolveAnimations(info));
        }
        CustomPackLoader.CHAIR_MODELS.putInfo(id, info);
    }
}
