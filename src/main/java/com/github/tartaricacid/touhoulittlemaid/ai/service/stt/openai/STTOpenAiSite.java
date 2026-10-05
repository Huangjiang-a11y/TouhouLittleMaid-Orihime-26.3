package com.github.tartaricacid.touhoulittlemaid.ai.service.stt.openai;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializableSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTApiType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.STTOpenAiFormLayout;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.STTSiteFormLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * OpenAI 语音识别站点（/v1/audio/transcriptions）。
 * <p>
 * 请求形状与硅基流动一致（multipart：model + file），所以任何 OpenAI 兼容的
 * 语音识别端点都能这么填（本地 faster-whisper、Groq 等）。
 */
public class STTOpenAiSite implements STTSite {
    public static final String API_TYPE = STTApiType.OPENAI.getName();

    private final String id;
    private final Identifier icon;

    private boolean enabled;
    private String url;
    private String secretKey;
    private String model;

    public STTOpenAiSite(String id, Identifier icon, boolean enabled, String url, String secretKey, String model) {
        this.id = id;
        this.icon = icon;
        this.enabled = enabled;
        this.url = url;
        this.secretKey = secretKey;
        this.model = model;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public boolean enabled() {
        return this.enabled;
    }

    @Override
    public Identifier icon() {
        return this.icon;
    }

    @Override
    public String url() {
        return this.url;
    }

    @Override
    public Map<String, String> headers() {
        return Map.of();
    }

    @Override
    public String getApiType() {
        return API_TYPE;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getModel() {
        return model;
    }

    @Override
    public STTOpenAiClient client() {
        return new STTOpenAiClient(STT_HTTP_CLIENT, this);
    }

    @Override
    public STTSiteFormLayout formLayout() {
        return new STTOpenAiFormLayout(this);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public static class Serializer implements SerializableSite<STTOpenAiSite> {
        public static final Codec<STTOpenAiSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf(ID).forGetter(STTOpenAiSite::id),
                Identifier.CODEC.fieldOf(ICON).forGetter(STTOpenAiSite::icon),
                Codec.BOOL.fieldOf(ENABLED).forGetter(STTOpenAiSite::enabled),
                Codec.STRING.fieldOf(URL).forGetter(STTOpenAiSite::url),
                Codec.STRING.fieldOf(SECRET_KEY).forGetter(STTOpenAiSite::getSecretKey),
                Codec.STRING.fieldOf("model").forGetter(STTOpenAiSite::getModel)
        ).apply(instance, STTOpenAiSite::new));

        @Override
        public Codec<STTOpenAiSite> codec() {
            return CODEC;
        }

        @Override
        public STTOpenAiSite defaultSite() {
            return new STTOpenAiSite(
                    API_TYPE,
                    SerializableSite.defaultIcon(API_TYPE),
                    false,
                    "https://api.openai.com/v1/audio/transcriptions",
                    StringUtils.EMPTY,
                    "whisper-1"
            );
        }
    }
}
