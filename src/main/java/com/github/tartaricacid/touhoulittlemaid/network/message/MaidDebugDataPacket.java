package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.debug.target.DebugTarget;
import com.github.tartaricacid.touhoulittlemaid.debug.target.MaidDebugClientData;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 女仆调试数据：每 4 tick 从服务端推给正在调试这只女仆的玩家。
 * 对应 1.21.1 里 PathfindingDebugPayload + GameTestAddMarkerDebugPayload 两个原版包的功能，
 * 26.3 把这两个包删了，改用新的调试订阅系统；这里用自家包重新实现。
 */
public record MaidDebugDataPacket(int maidId, List<BlockPos> pathNodes, int currentNode,
                                  List<DebugTarget> targets) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MaidDebugDataPacket> TYPE = new CustomPacketPayload.Type<>(modLoc("maid_debug_data"));

    public static final StreamCodec<ByteBuf, DebugTarget> TARGET_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, DebugTarget::pos,
            ByteBufCodecs.VAR_INT, DebugTarget::color,
            ByteBufCodecs.STRING_UTF8, DebugTarget::text,
            ByteBufCodecs.VAR_INT, DebugTarget::lifeTime,
            DebugTarget::new);

    public static final StreamCodec<ByteBuf, MaidDebugDataPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MaidDebugDataPacket::maidId,
            ByteBufCodecs.<ByteBuf, BlockPos>list().apply(BlockPos.STREAM_CODEC), MaidDebugDataPacket::pathNodes,
            ByteBufCodecs.VAR_INT, MaidDebugDataPacket::currentNode,
            ByteBufCodecs.<ByteBuf, DebugTarget>list().apply(TARGET_CODEC), MaidDebugDataPacket::targets,
            MaidDebugDataPacket::new);

    public static void handle(MaidDebugDataPacket message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> MaidDebugClientData.accept(message));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
