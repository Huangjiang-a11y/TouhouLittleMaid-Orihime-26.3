package com.github.tartaricacid.touhoulittlemaid.network.client;

import com.github.tartaricacid.touhoulittlemaid.client.sound.PortableAudioClientManager;
import com.github.tartaricacid.touhoulittlemaid.network.message.PortableAudioToClientPackage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class PortableAudioToClientPackageProxy {
    private PortableAudioToClientPackageProxy() {
    }

    public static void handle(PortableAudioToClientPackage message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(message.playerId());
        if (entity == null) {
            return;
        }
        if (message.play()) {
            PortableAudioClientManager.play(entity, message.url());
        } else {
            PortableAudioClientManager.stop(entity.getId());
        }
    }
}
