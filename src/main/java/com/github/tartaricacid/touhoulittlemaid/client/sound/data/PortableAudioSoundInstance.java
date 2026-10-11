package com.github.tartaricacid.touhoulittlemaid.client.sound.data;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.sound.OggReader;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 手提音响的播放实例：绑定在实体（玩家）身上，位置随实体移动。
 * <p>
 * 播放源是一条链接（{@code http(s)://} 或 {@code file://}），在后台线程拉取并流式解码
 * （MP3 / Opus / Vorbis），解码链路与 TTS 的 {@link MaidAISoundInstance} 一致。
 */
public class PortableAudioSoundInstance extends EntityBoundSoundInstance implements FabricSoundInstance {
    private final String source;

    public PortableAudioSoundInstance(Entity entity, String source) {
        super(InitSounds.PORTABLE_AUDIO, SoundSource.RECORDS, 4.0F, 1.0F, entity, entity.getId());
        this.source = source;
    }

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library, Identifier sound, boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return decode(readAll(this.source));
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error("[PortableAudio] 加载失败: {}", this.source, e);
                return null;
            }
        }, Util.backgroundExecutor());
    }

    private static AudioStream decode(byte[] data) throws Exception {
        try {
            return new Mp3AudioStream(data);
        } catch (UnsupportedAudioFileException e) {
            OggReader.Type type = OggReader.getOggType(data);
            if (type == OggReader.Type.OPUS) {
                return new OpusAudioStream(data);
            }
            if (type == OggReader.Type.VORBIS) {
                return new JOrbisAudioStream(new ByteArrayInputStream(data));
            }
            throw e;
        }
    }

    /**
     * 读取链接内容。支持 http(s):// 与 file://；http 链接里的中文/空格会自动做百分号编码。
     */
    private static byte[] readAll(String url) throws IOException {
        String trimmed = url.trim();
        if (trimmed.regionMatches(true, 0, "file:", 0, 5)) {
            String path = trimmed.substring(5);
            while (path.startsWith("//")) {
                path = path.substring(1);
            }
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            try (InputStream in = Files.newInputStream(Path.of(path))) {
                return in.readAllBytes();
            }
        }
        try (InputStream in = URI.create(encode(trimmed)).toURL().openStream()) {
            return in.readAllBytes();
        }
    }

    private static String encode(String url) {
        StringBuilder sb = new StringBuilder(url.length());
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == ' ' || c > 0x7E) {
                for (byte b : String.valueOf(c).getBytes(StandardCharsets.UTF_8)) {
                    sb.append('%');
                    sb.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16)));
                    sb.append(Character.toUpperCase(Character.forDigit(b & 0xF, 16)));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
