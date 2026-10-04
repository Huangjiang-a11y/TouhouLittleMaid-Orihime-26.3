package com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.DefaultGeckoAnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.controller.ControllerResource;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.controller.collection.ChairControllerCollection;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.controller.collection.MaidControllerCollection;
import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.Animation;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.MolangParser;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.file.AnimationFile;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.pojo.Converter;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.pojo.FormatVersion;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.pojo.RawGeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.raw.tree.RawGeometryTree;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.GeoBuilder;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoModel;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.ConditionManager;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoAsset;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoContainer;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.resource.GeckoLibCache;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.json.JsonAnimationUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.sound.data.SoundData;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.sound.data.SoundFormat;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static com.github.tartaricacid.touhoulittlemaid.api.event.client.DefaultGeckoAnimationEvent.AnimationType.*;

public class GeckoContainerBuilder {
    public static final Object2ReferenceOpenHashMap<DefaultGeckoAnimationEvent.AnimationType, Identifier> DEFAULT_ANIMATION_FILES = new Object2ReferenceOpenHashMap<>(
            new DefaultGeckoAnimationEvent.AnimationType[]{
                    MAID,
                    TAC,
                    ISS,
                    IM,
                    CHAIR
            },
            new Identifier[]{
                    IdentifierUtil.modLoc("animation/maid.animation.json"),
                    IdentifierUtil.modLoc("animation/tac.animation.json"),
                    IdentifierUtil.modLoc("animation/iss.animation.json"),
                    IdentifierUtil.modLoc("animation/im.animation.json"),
                    IdentifierUtil.modLoc("animation/chair.animation.json")
            });

    public static final Object2ReferenceOpenHashMap<String, Animation> DEFAULT_MAID_ANIMATIONS = new Object2ReferenceOpenHashMap<>();
    public static final Object2ReferenceOpenHashMap<String, Animation> DEFAULT_CHAIR_ANIMATIONS = new Object2ReferenceOpenHashMap<>();

    public static void reload() {
        clearAllCache();
        loadDefaultAnimation();
    }

    public static <T> void registerModelContainer(Identifier id, InputStreamSupplier geoStreamGetter,
                                                  InputStreamGetter<T> animStreamGetter,
                                                  List<T> animationFileIds,
                                                  Identifier texture,
                                                  GeckoContainer.Type type,
                                                  @Nullable InputStreamGetter<String> customSoundGetter) throws IOException {
        GeoModel geo;
        try (InputStream geoStream = geoStreamGetter.get()) {
            geo = GeckoContainerBuilder.registerGeo(geoStream);
        }

        var animationData = new AnimationFile();
        animationData.animations().putAll(type == GeckoContainer.Type.MAID ? DEFAULT_MAID_ANIMATIONS : DEFAULT_CHAIR_ANIMATIONS);
        for (var animationFileId : animationFileIds) {
            InputStream animStream = animStreamGetter.get(animationFileId);
            if (animStream != null) {
                try (animStream) {
                    animationData.animations().putAll(getAnimationFile(animStream).animations());
                }
            }
        }
        ConditionManager manager = new ConditionManager();
        for (var name : animationData.animations().keySet()) {
            manager.addTest(name);
        }

        var controllerResource = new ControllerResource(
                animationData.animations(),
                manager.armor,
                Object2ReferenceMaps.emptyMap(),
                Object2ReferenceMaps.emptyMap()
        );
        var controllerFactory = type == GeckoContainer.Type.MAID ?
                MaidControllerCollection.build(controllerResource) :
                ChairControllerCollection.build(controllerResource);

        // 音频：收集动画 sound_effects 中“不带冒号”的名字（带冒号的在 SoundInstanceManager#playSound 里直接当原版
        // SoundEvent 播放，无需在此处理），按“名字即 ogg 文件名”的约定从模型包取同名文件填入 sounds 表。
        Object2ReferenceMap<String, SoundData> sounds = Object2ReferenceMaps.emptyMap();
        if (customSoundGetter != null) {
            var effectNames = new LinkedHashSet<String>();
            for (var animation : animationData.animations().values()) {
                for (var keyFrame : animation.soundKeyFrames()) {
                    String effect = keyFrame.getEventData();
                    if (effect != null && !effect.isEmpty() && !effect.contains(":")) {
                        effectNames.add(effect);
                    }
                }
            }
            if (!effectNames.isEmpty()) {
                var loaded = new Object2ReferenceOpenHashMap<String, SoundData>();
                for (String effect : effectNames) {
                    try (InputStream soundStream = customSoundGetter.get(effect)) {
                        if (soundStream != null) {
                            loaded.put(effect, new SoundData(ByteBuffer.wrap(soundStream.readAllBytes()), SoundFormat.VORBIS));
                        }
                    } catch (IOException e) {
                        TouhouLittleMaid.LOGGER.error("Failed to load custom sound {} for model {}: {}", effect, id, e.getMessage());
                    }
                }
                sounds = loaded;
            }
        }
        // 说明：userFunctions 与 eventHandlers 在 Fabric 移植版中“没有数据源”——既无读取实现，现有模型包也不含
        // `user_functions` 或 `<category>_ctrl_<name>` 键，故保持空表，不再按 TODO 处理。
        var asset = new GeckoAsset(sounds, Object2ReferenceMaps.emptyMap(), Object2ReferenceMaps.emptyMap());
        GeckoLibCache.getInstance().getModels().put(id,
                new GeckoContainer(geo, animationData, controllerFactory, Object2ReferenceMaps.emptyMap(), manager, texture, asset, type));
    }

    private static GeoModel registerGeo(InputStream inputStream) {
        RawGeoModel rawModel = Converter.fromInputStream(inputStream);
        if (rawModel.getFormatVersion() == FormatVersion.NEW) {
            RawGeometryTree rawGeometryTree = RawGeometryTree.build(rawModel);
            return GeoBuilder.constructGeoModel(rawGeometryTree);
        }
        return null;
    }

    public static AnimationFile getAnimationFile(InputStream stream) {
        AnimationFile animationFile = new AnimationFile();
        MolangParser parser = GeckoLibCache.getInstance().parser.get();
        try {
            JsonObject jsonObject = GsonHelper.fromJson(CustomPackLoader.GSON, new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : JsonAnimationUtils.getAnimations(jsonObject)) {
                String animationName = entry.getKey();
                Animation animation;
                try {
                    animation = JsonAnimationUtils.deserializeJsonToAnimation(JsonAnimationUtils.getAnimation(jsonObject, animationName), parser);
                    animationFile.animations().put(animationName, animation);
                } catch (ChainedJsonException e) {
                    TouhouLittleMaid.LOGGER.error("Failed to load animation {}: {}", animationName, e.getMessage());
                }
            }
        } finally {
            parser.reset();
        }
        return animationFile;
    }

    private static void clearAllCache() {
        GeckoLibCache.getInstance().getModels().clear();
    }

    private static void loadDefaultAnimation() {
        DEFAULT_MAID_ANIMATIONS.clear();
        DEFAULT_CHAIR_ANIMATIONS.clear();

        var animationFiles = new EnumMap<DefaultGeckoAnimationEvent.AnimationType, AnimationFile>(DefaultGeckoAnimationEvent.AnimationType.class);
        for (var entry : DEFAULT_ANIMATION_FILES.entrySet()) {
            try (InputStream stream = Minecraft.getInstance().getResourceManager().open(entry.getValue())) {
                animationFiles.put(entry.getKey(), getAnimationFile(stream));
            } catch (IOException e) {
                animationFiles.put(entry.getKey(), new AnimationFile());
                TouhouLittleMaid.LOGGER.error("Failed to load default maid animation file:", e);
            }
        }

        DefaultGeckoAnimationEvent.CALLBACK.invoker().onDefaultGeckoAnimation(new DefaultGeckoAnimationEvent(animationFiles));

        for (var type : DEFAULT_ANIMATION_FILES.keySet()) {
            if (type == CHAIR) {
                DEFAULT_CHAIR_ANIMATIONS.putAll(animationFiles.get(CHAIR).animations());
            } else {
                DEFAULT_MAID_ANIMATIONS.putAll(animationFiles.get(type).animations());
            }
        }
    }

    @FunctionalInterface
    public interface InputStreamGetter<T> {
        @Nullable
        InputStream get(T id) throws IOException;
    }

    @FunctionalInterface
    public interface InputStreamSupplier {
        InputStream get() throws IOException;
    }
}
