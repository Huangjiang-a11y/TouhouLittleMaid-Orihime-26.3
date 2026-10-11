package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.network.client.PortableAudioToClientPackageProxy;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * S2C：通知客户端在指定玩家身上播放/停止手提音响。
 */
public record PortableAudioToClientPackage(int playerId, String url, boolean play) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PortableAudioToClientPackage> TYPE =
            new CustomPacketPayload.Type<>(modLoc("portable_audio_to_client"));
    public static final StreamCodec<ByteBuf, PortableAudioToClientPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PortableAudioToClientPackage::playerId,
            ByteBufCodecs.STRING_UTF8, PortableAudioToClientPackage::url,
            ByteBufCodecs.BOOL, PortableAudioToClientPackage::play,
            PortableAudioToClientPackage::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PortableAudioToClientPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> PortableAudioToClientPackageProxy.handle(message));
    }
}
