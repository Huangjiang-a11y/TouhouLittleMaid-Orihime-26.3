package com.github.tartaricacid.touhoulittlemaid.client.sound;

import com.github.tartaricacid.touhoulittlemaid.client.sound.data.PortableAudioSoundInstance;
import com.google.common.collect.Maps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.Entity;

import java.util.Map;

/**
 * 客户端侧的手提音响播放管理：按实体 id 记录正在播放的实例，支持停止/替换。
 */
public final class PortableAudioClientManager {
    private static final Map<Integer, SoundInstance> PLAYING = Maps.newHashMap();

    private PortableAudioClientManager() {
    }

    public static void play(Entity entity, String url) {
        stop(entity.getId());
        PortableAudioSoundInstance instance = new PortableAudioSoundInstance(entity, url);
        PLAYING.put(entity.getId(), instance);
        Minecraft.getInstance().getSoundManager().play(instance);
    }

    public static void stop(int entityId) {
        SoundInstance instance = PLAYING.remove(entityId);
        if (instance != null) {
            Minecraft.getInstance().getSoundManager().stop(instance);
        }
    }
}
