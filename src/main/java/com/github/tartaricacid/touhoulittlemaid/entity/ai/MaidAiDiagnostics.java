package com.github.tartaricacid.touhoulittlemaid.entity.ai;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import org.slf4j.Logger;

import java.util.stream.Collectors;

/**
 * 临时体检工具：定位"女仆一动不动"到底卡在哪一环。定位完应该整类删掉。
 * <p>
 * dump 每 2 秒打印一次她的真实内部状态；hardReset 在每次加载时把 AI 状态强清一遍。
 */
public final class MaidAiDiagnostics {
    /** 聊天框输出开关：排查时临时打开即可，默认关闭（日志里仍会打印） */
    public static final boolean CHAT_OUTPUT = false;

    private static final Logger LOGGER = LogUtils.getLogger();
    /** 是否开启体检日志（定位期间 true） */
    private static final boolean ENABLED = true;

    private MaidAiDiagnostics() {
    }

    /** 每次随着存档加载时调用一次：把可能残留（且写进存档）的 AI 目标记忆强清掉 */
    public static void hardReset(EntityMaid maid) {
        Brain<EntityMaid> brain = maid.getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(InitBrains.TARGET_POS);
        maid.getNavigation().stop();
        maid.setNoAi(false);
        maid.guiOpening = false;
        if (ENABLED) {
            LOGGER.info("[MaidDiag] hardReset 已执行，AI 目标记忆全部清空: {}", maid.getUUID());
        }
    }

    public static void dump(EntityMaid maid) {
        if (!ENABLED) {
            return;
        }
        Brain<EntityMaid> brain = maid.getBrain();
        String activities = brain.getActiveActivities().stream()
                .map(Activity::toString).collect(Collectors.joining(","));
        String running = brain.getRunningBehaviors().stream()
                .map(b -> b.getClass().getSimpleName()).collect(Collectors.joining(","));
        String atk = brain.getMemory(MemoryModuleType.ATTACK_TARGET)
                .map(e -> e.getType() + "@" + fmt(maid.distanceTo(e))).orElse("-");
        String walk = brain.getMemory(MemoryModuleType.WALK_TARGET)
                .map(w -> w.getTarget().currentBlockPosition() + " close=" + w.getCloseEnoughDist()
                        + " d=" + fmt(Math.sqrt(w.getTarget().currentBlockPosition().distSqr(maid.blockPosition()))))
                .orElse("-");
        String tpos = brain.getMemory(InitBrains.TARGET_POS)
                .map(t -> t.currentBlockPosition().toString()).orElse("-");
        var nav = maid.getNavigation();
        String path = nav.getPath() == null ? "null" : ("len=" + nav.getPath().getNodeCount());
        String home = maid.hasHome()
                ? maid.getHomePosition() + " d=" + fmt(Math.sqrt(maid.getHomePosition().distSqr(maid.blockPosition())))
                : "无";
        String owner = maid.getOwner() == null ? "-" : fmt(maid.distanceTo(maid.getOwner()));
        String line = String.format(
                "task=%s act=[%s] run=[%s] ATK=%s WALK=%s TPOS=%s nav(inProg=%s done=%s %s) pos=%s home=%s owner=%s guiOpening=%s noAi=%s",
                maid.getTask().getUid(), activities, running, atk, walk, tpos,
                nav.isInProgress(), nav.isDone(), path,
                maid.blockPosition(), home, owner, maid.guiOpening, maid.isNoAi());
        LOGGER.info("[MaidDiag] {}", line);
        // 直接发到主人聊天栏，省得去翻日志文件（16 格内才发，免得刷屏）
        if (maid.getOwner() instanceof ServerPlayer player && player.distanceTo(maid) <= 16.0F) {
            if (CHAT_OUTPUT) {
                player.sendSystemMessage(Component.literal("[MaidDiag] " + line));
            }
        }
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
