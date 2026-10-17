package com.github.tartaricacid.touhoulittlemaid.client.animation.gecko;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 临时探针：看一眼客户端每帧到底给女仆选了哪个动画，以及客户端是否知道她在坐。
 * 定位完删。
 */
public final class MaidAnimDiag {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final boolean ENABLED = true;
    private static long lastLog = 0L;

    private MaidAnimDiag() {
    }

    public static void log(EntityMaid maid, String chosen) {
        if (!ENABLED) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLog < 1000L) {
            return;
        }
        lastLog = now;
        LOGGER.info("[MaidAnimDiag] chosen={} clientSitting={} clientPassenger={} pose={} vehicle={} orderedToSit={}",
                chosen, maid.isMaidInSittingPose(), maid.isPassenger(), maid.getPose(),
                maid.getVehicle() == null ? "null" : maid.getVehicle().getType().toString(),
                maid.isOrderedToSit());
    }

    public static void logVehicle(String what) {
        if (!ENABLED) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastLog < 1000L) {
            return;
        }
        lastLog = now;
        LOGGER.info("[MaidAnimDiag] 载具动画通道命中: {}", what);
    }
}
