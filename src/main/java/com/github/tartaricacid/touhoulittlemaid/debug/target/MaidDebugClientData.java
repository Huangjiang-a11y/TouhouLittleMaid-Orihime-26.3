package com.github.tartaricacid.touhoulittlemaid.debug.target;

import com.github.tartaricacid.touhoulittlemaid.network.message.MaidDebugDataPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.util.VisibleForDebug;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端侧的女仆调试数据缓存。服务端每 4 tick 推一次，超过 1 秒没更新就丢弃。
 */
@VisibleForDebug
public final class MaidDebugClientData {
    private static final long EXPIRE_MS = 1000L;
    private static final Map<Integer, Entry> DATA = new ConcurrentHashMap<>();

    private MaidDebugClientData() {
    }

    public static void accept(MaidDebugDataPacket packet) {
        DATA.put(packet.maidId(), new Entry(packet.pathNodes(), packet.currentNode(), packet.targets(), System.currentTimeMillis()));
    }

    public static Map<Integer, Entry> data() {
        long now = System.currentTimeMillis();
        DATA.entrySet().removeIf(e -> now - e.getValue().time() > EXPIRE_MS);
        return DATA;
    }

    public record Entry(List<BlockPos> path, int currentNode, List<DebugTarget> targets, long time) {
    }
}
