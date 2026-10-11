package com.github.tartaricacid.touhoulittlemaid.network.message;

import cn.sh1rocu.touhoulittlemaid.util.PacketDistributor;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * C2S：玩家在手提音响 GUI 里点了播放/停止。
 * 服务端把链接记到手持物品上，并广播给追踪该玩家的所有客户端（含自己）。
 */
public record PortableAudioControlPackage(String url, boolean play) implements CustomPacketPayload {
    public static final int MAX_URL_LENGTH = 512;

    public static final CustomPacketPayload.Type<PortableAudioControlPackage> TYPE =
            new CustomPacketPayload.Type<>(modLoc("portable_audio_control"));
    public static final StreamCodec<ByteBuf, PortableAudioControlPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PortableAudioControlPackage::url,
            ByteBufCodecs.BOOL, PortableAudioControlPackage::play,
            PortableAudioControlPackage::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PortableAudioControlPackage message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ServerPlayer sender = context.player();
            String url = message.url();
            if (url.length() > MAX_URL_LENGTH) {
                url = url.substring(0, MAX_URL_LENGTH);
            }
            ItemStack stack = sender.getMainHandItem();
            if (stack.is(InitItems.PORTABLE_AUDIO)) {
                stack.set(InitDataComponent.PORTABLE_AUDIO_URL, url);
            }
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(sender,
                    new PortableAudioToClientPackage(sender.getId(), url, message.play()));
        });
    }
}
