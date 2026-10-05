package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.network.client.SyncYsmMaidDataPackageProxy;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 服务端 -> 客户端：同步女仆的 YSM 轮盘动画状态（对应 1.21.1 的同名包，
 * 这里只带轮盘字段；1.21.1 还带了 roamingVars，移植版暂不需要）
 */
public record SyncYsmMaidDataPackage(int maidId, String rouletteAnim, boolean playing) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncYsmMaidDataPackage> TYPE =
            new CustomPacketPayload.Type<>(modLoc("sync_ysm_maid_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncYsmMaidDataPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncYsmMaidDataPackage::maidId,
            ByteBufCodecs.STRING_UTF8, SyncYsmMaidDataPackage::rouletteAnim,
            ByteBufCodecs.BOOL, SyncYsmMaidDataPackage::playing,
            SyncYsmMaidDataPackage::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncYsmMaidDataPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> SyncYsmMaidDataPackageProxy.handle(message));
    }
}
