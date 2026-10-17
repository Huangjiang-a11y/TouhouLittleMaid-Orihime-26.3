package com.github.tartaricacid.touhoulittlemaid.debug.target;

import cn.sh1rocu.touhoulittlemaid.api.event.PlayerTickEvent;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.MaidDebugDataPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@VisibleForDebug
public class SendMaidDebugDataEvent {
    /** 一条路径最多同步这么多个节点，防止极端情况下包过大 */
    private static final int MAX_PATH_NODES = 64;
    private static final java.util.Map<Integer, Boolean> LAST_PATH_NULL = new java.util.concurrent.ConcurrentHashMap<>();

    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!TouhouLittleMaid.DEBUG) {
            return;
        }
        if (event.getEntity().level.isClientSide() || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        // 每 4 tick 发送一次数据
        if (serverPlayer.tickCount % 4 == 0) {
            List<EntityMaid> debuggingMaid = DebugMaidManager.getDebuggingMaid(serverPlayer);
            for (EntityMaid maid : debuggingMaid) {
                sendForMaid(maid, serverPlayer);
            }
        }
    }

    private static void sendForMaid(@Nullable EntityMaid maid, ServerPlayer player) {
        if (maid == null) {
            return;
        }
        List<BlockPos> nodes = new ArrayList<>();
        int currentNode = -1;
        Path path = maid.getNavigation().getPath();
        if (path != null) {
            currentNode = path.getNextNodeIndex();
            int count = Math.min(path.getNodeCount(), MAX_PATH_NODES);
            for (int i = 0; i < count; i++) {
                nodes.add(path.getNodePos(i));
            }
        }
        List<DebugTarget> targets = DebugMaidManager.getDebugTargets(maid);
        // 只在"路径从无到有 / 从有到无"时打一条，方便定位路径为空的时机
        boolean pathNull = nodes.isEmpty();
        Boolean last = LAST_PATH_NULL.put(maid.getId(), pathNull);
        if (last == null || last != pathNull) {
            TouhouLittleMaid.LOGGER.info("[MaidDebug] 状态变化: maid={} pathNull={} pathNodes={} currentNode={} targets={}",
                    maid.getId(), pathNull, nodes.size(), currentNode, targets.size());
        }
        ServerPlayNetworking.send(player, new MaidDebugDataPacket(maid.getId(), nodes, currentNode, targets));
    }
}
