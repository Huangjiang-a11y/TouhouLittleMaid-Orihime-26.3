package com.github.tartaricacid.touhoulittlemaid.network.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncYsmMaidDataPackage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class SyncYsmMaidDataPackageProxy {
    public static void handle(SyncYsmMaidDataPackage message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getEntity(message.maidId()) instanceof EntityMaid maid) {
            maid.rouletteAnim = message.rouletteAnim();
            maid.rouletteAnimPlaying = message.playing();
        }
    }
}
