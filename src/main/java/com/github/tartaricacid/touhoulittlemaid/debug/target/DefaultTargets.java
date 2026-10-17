package com.github.tartaricacid.touhoulittlemaid.debug.target;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBrains;
import net.minecraft.core.BlockPos;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.List;
import java.util.function.Function;

@VisibleForDebug
public class DefaultTargets {
    private static final int COLOR = 0x9FFFFF00;
    private static final String TEXT = "Target Pos";
    private static final int WALK_COLOR = 0x9F00CFFF;
    private static final int ATTACK_COLOR = 0x9FFF3030;
    private static final int LIFE_TIME = 2 * 1000;

    public static List<Function<EntityMaid, List<DebugTarget>>> getDefaultTargets() {
        return List.of(DefaultTargets::getDefaultTargets);
    }

    public static List<DebugTarget> getDefaultTargets(EntityMaid maid) {
        List<DebugTarget> result = new java.util.ArrayList<>(3);
        // 工作目标（任务自己记的）
        maid.getBrain().getMemory(InitBrains.TARGET_POS)
                .map(tracker -> new DebugTarget(tracker.currentBlockPosition(), COLOR, TEXT, LIFE_TIME))
                .ifPresent(result::add);
        // 行走目标（远程任务在射程内会清掉寻路、只留这个）
        maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .map(walk -> new DebugTarget(walk.getTarget().currentBlockPosition(), WALK_COLOR, "Walk Target", LIFE_TIME))
                .ifPresent(result::add);
        // 攻击目标
        maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                .map(entity -> new DebugTarget(BlockPos.containing(entity.position()), ATTACK_COLOR, "Attack Target", LIFE_TIME))
                .ifPresent(result::add);
        return result;
    }
}
