package com.github.tartaricacid.touhoulittlemaid.ai.service.stt.openai;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ResponseCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTConfig;
import com.github.tartaricacid.touhoulittlemaid.client.sound.record.MicrophoneManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OpenAI Realtime 语音识别客户端（WebSocket）。
 * <p>
 * 流程（照 RikkaHub 的实战实现 + 官方 realtime-transcription 文档）：
 * 1. 连 wss://.../v1/realtime?intent=transcription（URL 里没有 intent=transcription 会自动补）；
 * 2. 连上后发 session.update，session.type=transcription，手工模式 turn_detection=null；
 * 3. 把 PCM16 音频分块用 input_audio_buffer.append 发上去（base64）；
 * 4. 发 input_audio_buffer.commit；
 * 5. 收 conversation.item.input_audio_transcription.completed 里的 transcript。
 * <p>
 * 注意：麦克风录制用的就是 16kHz，这里直接把 rate 声明成 16000 发裸 PCM
 * （官方 WebSockets 指南里 audio/pcm 明确支持 16000 一档），本地不重采样、
 * 服务端也不用再把 24k 降到 16k。resample 保留，万一以后录制采样率变了还能用。
 * <p>
 * 收尾策略是自适应的：服务端回 session.updated（承认手工模式，等于官方行为，
 * 一次 commit 只出一条 completed）时收到第一条就收工；没有回执的兼容服务
 * （自己按音频静默断句，会切出多条 completed）则等 SEGMENT_SILENCE_SECONDS
 * 静默，并把多段拼接起来。
 */
public class STTOpenAiRealtimeClient implements STTClient {
    private static final AudioFormat RECORD_FORMAT = new AudioFormat(16000, 16, 1, true, false);
    private static final int RECORD_RATE = 16000;
    /**
     * 发给服务端的采样率，必须和 append 进去的字节一致（不一致 = 服务端按错误速率解码
     * = 语速快/慢 1.5 倍 = 识别糊掉）。麦克风录制就是 16kHz，官方 audio/pcm 也支持
     * 16000 一档，所以直发 16k：本地零重采样，服务端也不用再降采样。
     */
    private static final int API_RATE = 16000;
    private static final int CHUNK_BYTES = 32 * 1024;
    private static final long TIMEOUT_SECONDS = 45;
    /**
     * 服务端自己断句的服务（比如本地 SenseVoice 那种），一段录音会被切成多条
     * completed 事件。收到最后一段后静默这么久没有新段，就认为识别结束。
     */
    private static final long SEGMENT_SILENCE_SECONDS = 2;
    /**
     * 发给服务端的音频尾部补的静音量（毫秒），见 appendTailSilence
     */
    private static final int TAIL_SILENCE_MILLIS = 1000;
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "tlm-realtime-stt-timer");
        thread.setDaemon(true);
        return thread;
    });
    /**
     * 本地地址专用：不走代理。否则用户给 STT 配了代理（为了连 OpenAI 官方端点）
     * 之后，ws://127.0.0.1 之类的本地服务会被代理掉，连不上。
     */
    private static final HttpClient LOCAL_HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final STTOpenAiRealtimeSite site;
    private volatile WebSocket socket;

    public STTOpenAiRealtimeClient(STTOpenAiRealtimeSite site) {
        this.site = site;
    }

    @Override
    public void startRecord(STTConfig config, ResponseCallback<String> callback) {
        Mixer.Info info = MicrophoneManager.getMicrophoneInfo(RECORD_FORMAT);
        if (info == null) {
            TouhouLittleMaid.LOGGER.error("[STT] 找不到可用麦克风设备（Android/FCL 上 javax.sound 可能枚举不到任何设备），站点 URL={}",
                    this.site.url());
            callback.onFailure(null, new Throwable("No suitable microphone found"), ErrorCode.MICROPHONE_NOT_FOUND);
            return;
        }
        TouhouLittleMaid.LOGGER.info("[STT] 开始录音，麦克风设备={}，站点 URL={}", info.getName(), this.site.url());
        MicrophoneManager.startRecord(info.getName(), RECORD_FORMAT, data -> send(data, callback));
    }

    @Override
    public void stopRecord(STTConfig config, ResponseCallback<String> callback) {
        MicrophoneManager.stopRecord();
    }

    private void send(byte[] recorded, ResponseCallback<String> callback) {
        byte[] pcm = appendTailSilence(
                resample(stripWavHeader(recorded), RECORD_RATE, API_RATE), API_RATE, TAIL_SILENCE_MILLIS);
        TouhouLittleMaid.LOGGER.info("[STT] 上传开始：PCM {} 字节，站点 {}，分块 {}", pcm.length, this.site.url(),
                (pcm.length + CHUNK_BYTES - 1) / CHUNK_BYTES);
        CompletableFuture<String> finished = new CompletableFuture<>();
        AtomicBoolean done = new AtomicBoolean(false);
        List<String> segments = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<ScheduledFuture<?>> pendingFinish = new AtomicReference<>();
        // 服务端是否承认了我们的手工模式会话（回了 session.updated）。
        // 承认 = 一次 commit 恰好一条 completed = 官方行为，收到第一条就能收工；
        // 不承认（兼容服务自己断句）= 一段录音会有多条 completed，必须等静默。
        AtomicBoolean manualModeAcked = new AtomicBoolean(false);

        WebSocket.Listener listener = new WebSocket.Listener() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void onOpen(WebSocket webSocket) {
                socket = webSocket;
                TouhouLittleMaid.LOGGER.info("[STT] WebSocket 已连接，发送 session.update");
                webSocket.sendText(sessionUpdateEvent(), true);
                for (int offset = 0; offset < pcm.length; offset += CHUNK_BYTES) {
                    int length = Math.min(CHUNK_BYTES, pcm.length - offset);
                    String audio = Base64.getEncoder().encodeToString(
                            java.util.Arrays.copyOfRange(pcm, offset, offset + length));
                    webSocket.sendText(appendEvent(audio), true);
                }
                webSocket.sendText("{\"type\":\"input_audio_buffer.commit\"}", true);
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                TouhouLittleMaid.LOGGER.info("[STT] 收到事件: {}", data.length() > 300 ? data.subSequence(0, 300) + "..." : data);
                buffer.append(data);
                if (!last) {
                    webSocket.request(1);
                    return null;
                }
                String text = buffer.toString();
                buffer.setLength(0);
                handleEvent(text, finished, segments, pendingFinish, manualModeAcked);
                webSocket.request(1);
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                TouhouLittleMaid.LOGGER.error("[STT] WebSocket 出错: {}", error.toString());
                finished.completeExceptionally(error);
            }
        };

        URI uri = URI.create(this.endpoint());
        var builder = clientFor(uri).newWebSocketBuilder().connectTimeout(Duration.ofSeconds(10));
        if (!this.site.getSecretKey().isBlank()) {
            builder.header("Authorization", "Bearer " + this.site.getSecretKey());
        }
        builder.buildAsync(uri, listener).whenComplete((webSocket, throwable) -> {
            if (throwable != null) {
                finished.completeExceptionally(throwable);
            }
        });

        finished.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).whenComplete((transcript, throwable) -> {
            if (!done.compareAndSet(false, true)) {
                return;
            }
            WebSocket webSocket = this.socket;
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
            if (throwable != null) {
                TouhouLittleMaid.LOGGER.error("[STT] OpenAI realtime STT failed", throwable);
                callback.onFailure(null, throwable, ErrorCode.REQUEST_RECEIVED_ERROR);
            } else {
                callback.onSuccess(transcript);
            }
        });
    }

    private void handleEvent(String text, CompletableFuture<String> finished, List<String> segments,
                             AtomicReference<ScheduledFuture<?>> pendingFinish, AtomicBoolean manualModeAcked) {
        try {
            JsonElement element = JsonParser.parseString(text);
            if (!element.isJsonObject()) {
                return;
            }
            JsonObject event = element.getAsJsonObject();
            JsonElement typeElement = event.get("type");
            if (typeElement == null || !typeElement.isJsonPrimitive()) {
                return;
            }
            switch (typeElement.getAsString()) {
                case "session.updated" -> {
                    if (manualModeAcked.compareAndSet(false, true)) {
                        TouhouLittleMaid.LOGGER.info(
                                "[STT] OpenAI Realtime：服务端回了 session.updated，手工模式有效（收到首段即收工）");
                    }
                }
                case "conversation.item.input_audio_transcription.completed" -> {
                    String transcript = getString(event, "transcript", "").trim();
                    if (transcript.isEmpty()) {
                        return;
                    }
                    if (finished.isDone()) {
                        // 只有"承认了手工模式却还在自己断句"的服务会走到这里：回调已经发过，
                        // 这一段接不上了。打出来，方便定位该不该退回静默窗口模式。
                        TouhouLittleMaid.LOGGER.warn(
                                "OpenAI realtime STT: 收到晚到的分段，已丢弃：{}", transcript);
                        return;
                    }
                    if (manualModeAcked.get() && segments.isEmpty()) {
                        // 官方行为：一次 commit 只产生一条 completed，立刻收工，不等静默
                        segments.add(transcript);
                        finished.complete(joinSegments(segments));
                        return;
                    }
                    segments.add(transcript);
                    if (segments.size() == 1) {
                        TouhouLittleMaid.LOGGER.info(
                                "[STT] OpenAI Realtime：未收到 session.updated，按服务端自行断句处理（静默 {} 秒收尾）",
                                SEGMENT_SILENCE_SECONDS);
                    }
                    // 服务端自己断句时，后面可能还有别的段，静默一段时间再收工
                    ScheduledFuture<?> previous = pendingFinish.getAndSet(TIMER.schedule(
                            () -> finished.complete(joinSegments(segments)),
                            SEGMENT_SILENCE_SECONDS, TimeUnit.SECONDS));
                    if (previous != null) {
                        previous.cancel(false);
                    }
                }
                case "conversation.item.input_audio_transcription.failed" ->
                        finished.completeExceptionally(new Throwable(errorMessage(event, "Transcription failed")));
                case "error" ->
                        finished.completeExceptionally(new Throwable(errorMessage(event, "Realtime error")));
                default -> {
                }
            }
        } catch (Exception e) {
            TouhouLittleMaid.LOGGER.warn("Invalid realtime event: {}", text);
        }
    }

    /**
     * 去掉 WAV 容器头。MicrophoneManager 会往原始 PCM 前面套一段 WAV 头（RIFF/fmt/data），
     * 但 Realtime 协议要求 base64 裸 PCM、"without a WAV or other container header"。
     * 不剥的话这几十个字节会被当成音频样本解码（约 1.4ms 噪音），还可能让靠音频静默
     * 断句的服务误判出一个垃圾分段。按 chunk 找到 data 块取其后内容，非 WAV 原样返回。
     */
    private static byte[] stripWavHeader(byte[] data) {
        boolean riff = data.length >= 44 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F'
                && data[3] == 'F' && data[8] == 'W' && data[9] == 'A' && data[10] == 'V' && data[11] == 'E';
        if (!riff) {
            return data;
        }
        int offset = 12;
        while (offset + 8 <= data.length) {
            int size = (data[offset + 4] & 0xFF) | ((data[offset + 5] & 0xFF) << 8)
                    | ((data[offset + 6] & 0xFF) << 16) | ((data[offset + 7] & 0xFF) << 24);
            if (data[offset] == 'd' && data[offset + 1] == 'a' && data[offset + 2] == 't'
                    && data[offset + 3] == 'a') {
                int start = offset + 8;
                return java.util.Arrays.copyOfRange(data, start, Math.min(data.length, start + Math.max(size, 0)));
            }
            if (size < 0 || offset + 8 + size > data.length) {
                break;
            }
            offset += 8 + size + (size & 1);
        }
        // 兜底：标准 WAV 头就是 44 字节
        return java.util.Arrays.copyOfRange(data, Math.min(44, data.length), data.length);
    }

    /**
     * 尾部补静音。服务端自己断句的兼容服务（靠音频里的静默判定一句结束，比如
     * SenseVoice 那种 min_silence=0.4s）如果录到最后一个字就停，最后一句永远
     * 等不到静默，不会有 completed 事件，结果就是整段被丢掉。补 1 秒静音即可。
     */
    private static byte[] appendTailSilence(byte[] pcm, int rate, int millis) {
        int bytes = rate * 2 * millis / 1000;
        byte[] out = new byte[pcm.length + bytes];
        System.arraycopy(pcm, 0, out, 0, pcm.length);
        return out;
    }

    private static String joinSegments(List<String> segments) {
        synchronized (segments) {
            return String.join("", segments);
        }
    }

    /**
     * 本地（回环）地址不走代理，其余地址用带代理配置的共享客户端
     */
    private static HttpClient clientFor(URI uri) {
        String host = uri.getHost();
        if (host != null) {
            String lower = host.toLowerCase();
            if ("127.0.0.1".equals(lower) || "localhost".equals(lower)
                    || "::1".equals(lower) || "[::1]".equals(lower)) {
                return LOCAL_HTTP_CLIENT;
            }
        }
        return STTSite.STT_HTTP_CLIENT;
    }

    private static String errorMessage(JsonObject event, String fallback) {
        JsonElement error = event.get("error");
        if (error != null && error.isJsonObject()) {
            return getString(error.getAsJsonObject(), "message", fallback);
        }
        return fallback;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        if (element instanceof JsonPrimitive primitive && primitive.isString()) {
            return primitive.getAsString();
        }
        return fallback;
    }

    /**
     * URL 里没写 intent=transcription 就补上（Realtime 的转录模式靠这个 query 参数区分）
     */
    private String endpoint() {
        String url = this.site.url().trim();
        if (url.contains("intent=transcription")) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url.replaceAll("/+$", "") + separator + "intent=transcription";
    }

    /**
     * 会话配置：转录模式（type=transcription）+ 手工回合（turn_detection=null，
     * 由客户端 commit）
     */
    private String sessionUpdateEvent() {
        JsonObject format = new JsonObject();
        format.addProperty("type", "audio/pcm");
        format.addProperty("rate", API_RATE);

        JsonObject transcription = new JsonObject();
        transcription.addProperty("model", this.site.getModel());

        JsonObject input = new JsonObject();
        input.add("format", format);
        input.add("transcription", transcription);
        input.add("turn_detection", com.google.gson.JsonNull.INSTANCE);

        JsonObject audio = new JsonObject();
        audio.add("input", input);

        JsonObject session = new JsonObject();
        session.addProperty("type", "transcription");
        session.add("audio", audio);

        JsonObject event = new JsonObject();
        event.addProperty("type", "session.update");
        event.add("session", session);
        return event.toString();
    }

    private static String appendEvent(String base64Audio) {
        JsonObject event = new JsonObject();
        event.addProperty("type", "input_audio_buffer.append");
        event.addProperty("audio", base64Audio);
        return event.toString();
    }

    /**
     * PCM16 单声道线性插值重采样
     */
    private static byte[] resample(byte[] pcm, int fromRate, int toRate) {
        if (fromRate == toRate) {
            return pcm;
        }
        int inSamples = pcm.length / 2;
        if (inSamples == 0) {
            return pcm;
        }
        int outSamples = (int) ((long) inSamples * toRate / fromRate);
        byte[] out = new byte[outSamples * 2];
        for (int i = 0; i < outSamples; i++) {
            double position = (double) i * fromRate / toRate;
            int index = (int) position;
            int next = Math.min(index + 1, inSamples - 1);
            double fraction = position - index;
            short first = readShort(pcm, index * 2);
            short second = readShort(pcm, next * 2);
            short value = (short) Math.round(first + (second - first) * fraction);
            out[i * 2] = (byte) (value & 0xFF);
            out[i * 2 + 1] = (byte) ((value >> 8) & 0xFF);
        }
        return out;
    }

    private static short readShort(byte[] data, int index) {
        return (short) ((data[index] & 0xFF) | (data[index + 1] << 8));
    }
}
