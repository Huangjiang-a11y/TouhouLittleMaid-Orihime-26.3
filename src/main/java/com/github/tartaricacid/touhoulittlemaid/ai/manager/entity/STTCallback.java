package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ResponseCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.SendUserChatPackage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpRequest;

public class STTCallback implements ResponseCallback<String> {
    private final Player player;
    private final EntityMaid maid;

    public STTCallback(Player player, EntityMaid maid) {
        this.player = player;
        this.maid = maid;
    }

    @Override
    public void onFailure(HttpRequest request, Throwable throwable, int errorCode) {
        String cause = throwable.getLocalizedMessage();
        MutableComponent errorMessage = ErrorCode.getErrorMessage(ServiceType.STT, errorCode, cause);
        runOnClientThread(() -> player.sendSystemMessage(errorMessage.withStyle(ChatFormatting.RED)));
        TouhouLittleMaid.LOGGER.error("STT request failed: {}, error is {}", request, throwable.getMessage());
    }

    @Override
    public void onSuccess(String chatText) {
        runOnClientThread(() -> {
            if (StringUtils.isNotBlank(chatText)) {
                ChatClientInfo clientInfo = ChatClientInfo.fromMaid(this.maid);
                ClientPlayNetworking.send(new SendUserChatPackage(maid.getId(), chatText, clientInfo));
                String name = player.getScoreboardName();
                String format = String.format("<%s> %s", name, chatText);
                player.sendSystemMessage(Component.literal(format).withStyle(ChatFormatting.GRAY));
            } else {
                MutableComponent component = Component.translatable("ai.touhou_little_maid.chat.stt.content_is_empty");
                player.sendSystemMessage(component.withStyle(ChatFormatting.GRAY));
            }
        });
    }

    /**
     * HTTP/WebSocket 的回调线程不是客户端主线程。在回调线程上发聊天消息会触发字体烘焙
     * （GuiMessage.splitLines → FontSet.getGlyph → 字形纹理上传 → 提交 GPU 命令），
     * 非渲染线程碰 GL 会直接把驱动搞崩，所以统一回到客户端主线程执行。
     */
    private static void runOnClientThread(Runnable runnable) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            runnable.run();
        } else {
            minecraft.execute(runnable);
        }
    }
}
