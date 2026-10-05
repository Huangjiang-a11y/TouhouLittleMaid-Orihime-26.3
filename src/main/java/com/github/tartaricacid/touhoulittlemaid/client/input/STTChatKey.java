package com.github.tartaricacid.touhoulittlemaid.client.input;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.STTCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static com.github.tartaricacid.touhoulittlemaid.client.init.KeyMappingRegister.MAID_CATEGORY;

public class STTChatKey {
    public static final KeyMapping STT_CHAT_KEY = new KeyMapping("key.touhou_little_maid.stt_chat.desc",
//            KeyConflictContext.IN_GAME,
//            KeyModifier.NONE,
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_X,
            MAID_CATEGORY
    );

    public static void onSttChatPress(int action, KeyEvent event) {
        if (keyIsMatch(event)) {
            TouhouLittleMaid.LOGGER.info("[STT] 按键命中（按键 {}", STT_CHAT_KEY.getTranslatedKeyMessage().getString());
            if (!AIConfig.LLM_ENABLED.get()) {
                TouhouLittleMaid.LOGGER.info("[STT] 跳过：LLMEnabled=false");
                return;
            }
            if (!AIConfig.STT_ENABLED.get()) {
                TouhouLittleMaid.LOGGER.info("[STT] 跳过：STTEnabled=false");
                return;
            }
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                TouhouLittleMaid.LOGGER.info("[STT] 跳过：玩家为空或旁观者");
                return;
            }
            if (!isInGame()) {
                TouhouLittleMaid.LOGGER.info("[STT] 跳过：不在游戏内（GUI/加载界面/鼠标未捕获/窗口非激活）");
                return;
            }
            STT_CHAT_KEY.consumeClick();
            if (action == InputConstants.PRESS) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(InitSounds.RECORDING_START, 1f));
                getNearestMaid(player, STTChatKey::sttStart, true);
                return;
            }
            if (action == InputConstants.RELEASE) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(InitSounds.RECORDING_END, 1f));
                getNearestMaid(player, STTChatKey::sttStop, false);
            }
        }
    }

    @SuppressWarnings("removal")
    private static boolean keyIsMatch(KeyEvent event) {
        return STT_CHAT_KEY.matches(event);
    }

    private static void getNearestMaid(LocalPlayer player, Consumer<EntityMaid> consumer, boolean isStart) {
        Level level = player.level;
        int range = AIConfig.MAID_CAN_CHAT_DISTANCE.get();
        AABB aabb = player.getBoundingBox().inflate(range);
        List<EntityMaid> maids = level.getEntitiesOfClass(EntityMaid.class, aabb, maid -> maid.isOwnedBy(player) && maid.isAlive());
        maids.sort(Comparator.comparingDouble(maid -> maid.distanceToSqr(player)));
        if (!maids.isEmpty()) {
            TouhouLittleMaid.LOGGER.info("[STT] 12 格内找到 {} 只自己的女仆，取最近的一只", maids.size());
            consumer.accept(maids.get(0));
            return;
        }
        TouhouLittleMaid.LOGGER.info("[STT] 跳过：{} 格内没有自己的女仆", range);
        if (isStart) {
            player.sendSystemMessage(Component.translatable("ai.touhou_little_maid.chat.stt.no_maid_found", range));
        }
    }

    private static boolean isInGame() {
        Minecraft mc = Minecraft.getInstance();
        // 不能是加载界面
        if (ScreenUtil.hasOverlay()) {
            return false;
        }
        // 不能打开任何 GUI
        if (ScreenUtil.getScreen() != null) {
            return false;
        }
        // 当前窗口捕获鼠标操作
        if (!mc.mouseHandler.isMouseGrabbed()) {
            return false;
        }
        // 选择了当前窗口
        return mc.isWindowActive();
    }

    private static void sttStart(EntityMaid maid) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        String typeName = AIConfig.STT_TYPE.get().getName();
        STTSite sttSite = AvailableSites.getSTTSite(typeName);
        TouhouLittleMaid.LOGGER.info("[STT] 配置类型={}，解析到站点={}，enabled={}", typeName, sttSite,
                sttSite != null && sttSite.enabled());
        if (sttSite == null) {
            TouhouLittleMaid.LOGGER.error("[STT] 跳过：类型 {} 没有对应的站点（站点 id 与类型名不匹配？）", typeName);
            player.sendSystemMessage(Component.translatable("ai.touhou_little_maid.chat.stt.empty"));
            return;
        }
        if (!sttSite.enabled()) {
            TouhouLittleMaid.LOGGER.info("[STT] 跳过：站点未被启用（站点列表里点亮它）");
            player.sendSystemMessage(Component.translatable("ai.touhou_little_maid.chat.stt.empty"));
            return;
        }
        tryToStart(maid, player, sttSite);
    }

    private static void tryToStart(EntityMaid maid, LocalPlayer player, STTSite sttSite) {
        STTConfig config = new STTConfig();
        STTCallback callback = new STTCallback(player, maid);
        sttSite.client().startRecord(config, callback);
    }

    private static void sttStop(EntityMaid maid) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        STTSite sttSite = AvailableSites.getSTTSite(AIConfig.STT_TYPE.get().getName());
        TouhouLittleMaid.LOGGER.info("[STT] 松键停止，站点={}", sttSite);
        if (sttSite != null && sttSite.enabled()) {
            STTConfig config = new STTConfig();
            STTCallback callback = new STTCallback(player, maid);
            sttSite.client().stopRecord(config, callback);
        }
    }
}
