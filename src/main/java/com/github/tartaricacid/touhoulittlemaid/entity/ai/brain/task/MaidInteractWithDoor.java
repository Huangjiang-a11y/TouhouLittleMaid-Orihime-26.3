package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.mixin.accessor.FenceGateBlockAccessor;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.OptionalBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import javax.annotation.Nullable;
import java.util.*;

public class MaidInteractWithDoor {
    private static final int COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE = 3;
    private static final double SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN = 8;
    private static final double MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS = 2;

    /**
     * 卡住多少 tick 就认定"这扇开着的门在挡路"。走一格大约 10 tick，给 1 秒足够排除正常的
     * 减速、转身、被别的东西挡住后重新起步。
     */
    private static final int STUCK_TICKS_BEFORE_CLOSING_DOOR = 20;
    /**
     * 一 tick 的位移小于这个平方距离就算"没动"（约 0.05 格）。
     */
    private static final double STUCK_MOVEMENT_SQR = 0.0025;
    /**
     * 关掉之后这段时间内不许再开 —— 否则会和下面那段"开门"逻辑来回拉锯：
     * 侧放的门是"关门能过、开门挡路"，一开就又把她堵住。
     */
    private static final int KEEP_CLOSED_TICKS = 60;
    /**
     * 每只女仆的"卡住计数 + 暂时别开的门"。键是弱引用，女仆没了自动丢；
     * 服务器 tick 单线程，不需要并发保护。
     */
    private static final Map<EntityMaid, BlockedDoorState> BLOCKED_DOOR_STATES = new WeakHashMap<>();

    private static final class BlockedDoorState {
        @Nullable
        private Vec3 lastPos;
        private int stuckTicks;
        @Nullable
        private BlockPos keepClosedPos;
        private long keepClosedUntil;
    }

    public static BehaviorControl<LivingEntity> create() {
        MutableObject<Node> mutableObject = new MutableObject<>();
        MutableInt mutableInt = new MutableInt();
        return BehaviorBuilder.create((instance) -> instance.group(instance.present(MemoryModuleType.PATH), instance.registered(MemoryModuleType.DOORS_TO_CLOSE),
                instance.registered(MemoryModuleType.NEAREST_LIVING_ENTITIES)).apply(instance, (pathMemory, doorToCloseMemory, livingEntityMemory) -> (serverLevel, entity, time) -> {
            Path path = instance.get(pathMemory);
            Optional<Set<GlobalPos>> doorToClosePos = instance.tryGet(doorToCloseMemory);

            if (!path.notStarted() && !path.isDone()) {
                // 卡住就关门。单独放在节流之前：它是"补救"逻辑，不该被下面那段开/关门的
                // 节流吞掉（否则每次都是差几 tick 判定不到）。
                closeBlockingDoorWhenStuck(serverLevel, entity, path, time);

                if (Objects.equals(mutableObject.getValue(), path.getNextNode())) {
                    mutableInt.setValue(COOLDOWN_BEFORE_RERUNNING_IN_SAME_NODE);
                } else if (mutableInt.decrementAndGet() > 0) {
                    return false;
                }

                boolean canOpenDoor = entity instanceof EntityMaid maid && maid.getConfigManager().isOpenDoor();
                boolean canOpenFenceGate = entity instanceof EntityMaid maid && maid.getConfigManager().isOpenFenceGate();

                mutableObject.setValue(path.getNextNode());
                Node previousNode = path.getPreviousNode();
                Node nextNode = path.getNextNode();
                BlockPos previousNodeBlockPos = previousNode.asBlockPos();
                BlockState previousNodeBlockState = serverLevel.getBlockState(previousNodeBlockPos);

                BlockPos keepClosed = keepClosedDoor(entity, time);

                if (canOpenDoor && !previousNodeBlockPos.equals(keepClosed) && previousNodeBlockState.is(BlockTags.WOODEN_DOORS, (stateBase) -> stateBase.getBlock() instanceof DoorBlock)) {
                    DoorBlock doorBlock = (DoorBlock) previousNodeBlockState.getBlock();
                    if (!doorBlock.isOpen(previousNodeBlockState)) {
                        doorBlock.setOpen(entity, serverLevel, previousNodeBlockState, previousNodeBlockPos, true);
                    }
                    doorToClosePos = rememberDoorToClose(doorToCloseMemory, doorToClosePos, serverLevel, previousNodeBlockPos);
                } else if (canOpenFenceGate && previousNodeBlockState.is(BlockTags.FENCE_GATES, (stateBase) -> stateBase.getBlock() instanceof FenceGateBlock)) {
                    if (!previousNodeBlockState.getValue(FenceGateBlock.OPEN)) {
                        setFenceGate(entity, serverLevel, previousNodeBlockState, previousNodeBlockPos, true);
                    }
                    doorToClosePos = rememberDoorToClose(doorToCloseMemory, doorToClosePos, serverLevel, previousNodeBlockPos);
                }

                BlockPos nextNodeBlockPos = nextNode.asBlockPos();
                BlockState nextNodeBlockState = serverLevel.getBlockState(nextNodeBlockPos);
                if (canOpenDoor && !nextNodeBlockPos.equals(keepClosed) && nextNodeBlockState.is(BlockTags.WOODEN_DOORS, (stateBase) -> stateBase.getBlock() instanceof DoorBlock)) {
                    DoorBlock doorBlock = (DoorBlock) nextNodeBlockState.getBlock();
                    if (!doorBlock.isOpen(nextNodeBlockState)) {
                        doorBlock.setOpen(entity, serverLevel, nextNodeBlockState, nextNodeBlockPos, true);
                        doorToClosePos = rememberDoorToClose(doorToCloseMemory, doorToClosePos, serverLevel, nextNodeBlockPos);
                    }
                } else if (canOpenFenceGate && nextNodeBlockState.is(BlockTags.FENCE_GATES, (stateBase) -> stateBase.getBlock() instanceof FenceGateBlock)) {
                    if (!nextNodeBlockState.getValue(FenceGateBlock.OPEN)) {
                        setFenceGate(entity, serverLevel, nextNodeBlockState, nextNodeBlockPos, true);
                        doorToClosePos = rememberDoorToClose(doorToCloseMemory, doorToClosePos, serverLevel, nextNodeBlockPos);
                    }
                }

                doorToClosePos.ifPresent((doorPos) -> closeDoorsThatIHaveOpenedOrPassedThrough(serverLevel, entity, previousNode, nextNode, doorPos, instance.tryGet(livingEntityMemory)));
                return true;
            } else {
                return false;
            }
        }));
    }

    /**
     * 女仆卡在一扇开着的木门前时，把它关上。
     * <p>
     * 为什么需要：门的形状是按 {@code FACING} 取的旋转份，而 {@code OPEN} 时改按
     * {@code FACING.getClockWise()} / {@code getCounterClockWise()}（取决于 HINGE）取 ——
     * 等于在 {@code FACING} 上又转了 90°（见 DoorBlock#getShape）。于是"走廊轴 ≠ 门 FACING 轴"
     * （玩家侧放门）时，<b>关门能过、开门反而挡路</b>：门板是 3/16 厚的满高薄板、横跨整条走廊，
     * 0.6 宽的女仆挤不过去（正常朝向还剩 0.8125 的缝，能过）。而 path type 只看
     * {@code DoorBlock.OPEN} 不看朝向，寻路照样判"可走"，她就一头撞在门板上原地不动。
     * <p>
     * 这里刻意不预判方向：只是"先试试，走不动就换个开合状态再试"，任何朝向都能兜住。
     */
    private static void closeBlockingDoorWhenStuck(ServerLevel serverLevel, LivingEntity entity, Path path, long time) {
        if (!(entity instanceof EntityMaid maid) || !maid.getConfigManager().isOpenDoor()) {
            return;
        }
        BlockedDoorState state = BLOCKED_DOOR_STATES.computeIfAbsent(maid, k -> new BlockedDoorState());
        Vec3 pos = maid.position();
        if (state.lastPos == null || state.lastPos.distanceToSqr(pos) >= STUCK_MOVEMENT_SQR) {
            state.stuckTicks = 0;
        } else {
            state.stuckTicks++;
        }
        state.lastPos = pos;
        if (state.stuckTicks < STUCK_TICKS_BEFORE_CLOSING_DOOR) {
            return;
        }
        state.stuckTicks = 0;

        for (BlockPos doorPos : blockingDoorCandidates(path, maid)) {
            BlockState blockState = serverLevel.getBlockState(doorPos);
            if (blockState.is(BlockTags.WOODEN_DOORS, (stateBase) -> stateBase.getBlock() instanceof DoorBlock)) {
                DoorBlock doorBlock = (DoorBlock) blockState.getBlock();
                if (doorBlock.isOpen(blockState)) {
                    doorBlock.setOpen(maid, serverLevel, blockState, doorPos, false);
                    state.keepClosedPos = doorPos.immutable();
                    state.keepClosedUntil = time + KEEP_CLOSED_TICKS;
                    return;
                }
            }
        }
    }

    /**
     * 可能是"挡路那扇门"的位置：下一节点（她正要走进去的）、她自己所在格、上一节点。
     */
    private static List<BlockPos> blockingDoorCandidates(Path path, EntityMaid maid) {
        List<BlockPos> candidates = new ArrayList<>(3);
        Node next = path.getNextNode();
        if (next != null) {
            candidates.add(next.asBlockPos());
        }
        BlockPos selfPos = maid.blockPosition();
        if (!candidates.contains(selfPos)) {
            candidates.add(selfPos);
        }
        Node previous = path.getPreviousNode();
        if (previous != null && !candidates.contains(previous.asBlockPos())) {
            candidates.add(previous.asBlockPos());
        }
        return candidates;
    }

    /**
     * 目前处于"保持关闭"窗口内的那扇门（见 {@link #closeBlockingDoorWhenStuck}），没有则返回 null。
     */
    @Nullable
    private static BlockPos keepClosedDoor(LivingEntity entity, long time) {
        if (!(entity instanceof EntityMaid maid)) {
            return null;
        }
        BlockedDoorState state = BLOCKED_DOOR_STATES.get(maid);
        if (state == null || state.keepClosedPos == null || time >= state.keepClosedUntil) {
            return null;
        }
        return state.keepClosedPos;
    }

    public static void closeDoorsThatIHaveOpenedOrPassedThrough(ServerLevel serverLevel, LivingEntity entity, @Nullable Node previous, @Nullable Node next, Set<GlobalPos> doorPositions, Optional<List<LivingEntity>> nearestLivingEntities) {
        Iterator<GlobalPos> doorPosIterator = doorPositions.iterator();
        while (doorPosIterator.hasNext()) {
            GlobalPos globalPos = doorPosIterator.next();
            BlockPos blockPos = globalPos.pos();
            if ((previous == null || !previous.asBlockPos().equals(blockPos) || (next != null && entity.blockPosition().equals(next.asBlockPos())))
                    && (next == null || !next.asBlockPos().equals(blockPos))) {
                if (isDoorTooFarAway(serverLevel, entity, globalPos)) {
                    doorPosIterator.remove();
                } else {
                    BlockState blockstate = serverLevel.getBlockState(blockPos);

                    if (blockstate.is(BlockTags.WOODEN_DOORS, (stateBase) -> stateBase.getBlock() instanceof DoorBlock)) {
                        DoorBlock doorblock = (DoorBlock) blockstate.getBlock();
                        if (!doorblock.isOpen(blockstate)) {
                            doorPosIterator.remove();
                        } else if (areOtherMobsComingThroughDoor(entity, blockPos, nearestLivingEntities)) {
                            doorPosIterator.remove();
                        } else {
                            doorblock.setOpen(entity, serverLevel, blockstate, blockPos, false);
                            doorPosIterator.remove();
                        }
                    } else if ((blockstate.is(BlockTags.FENCE_GATES, (stateBase) -> stateBase.getBlock() instanceof FenceGateBlock))) {
                        if (!blockstate.getValue(FenceGateBlock.OPEN)) {
                            doorPosIterator.remove();
                        } else if (areOtherMobsComingThroughDoor(entity, blockPos, nearestLivingEntities)) {
                            doorPosIterator.remove();
                        } else {
                            setFenceGate(entity, serverLevel, blockstate, blockPos, false);
                        }
                    } else {
                        doorPosIterator.remove();
                    }
                }
            }
        }

    }

    private static boolean areOtherMobsComingThroughDoor(LivingEntity entity, BlockPos pos, Optional<List<LivingEntity>> nearestLivingEntities) {
        return nearestLivingEntities.map(entities -> entities.stream()
                .filter((livingEntity) -> livingEntity.getType() == entity.getType())
                .filter((livingEntity) -> pos.closerToCenterThan(livingEntity.position(), MAX_DISTANCE_TO_HOLD_DOOR_OPEN_FOR_OTHER_MOBS))
                .anyMatch((livingEntity) -> isMobComingThroughDoor(livingEntity.getBrain(), pos))).orElse(false);
    }

    private static boolean isMobComingThroughDoor(Brain<?> brain, BlockPos pos) {
        if (!brain.hasMemoryValue(MemoryModuleType.PATH)) {
            return false;
        } else {
            Path path = brain.getMemory(MemoryModuleType.PATH).get();
            if (path.isDone()) {
                return false;
            } else {
                Node previousNode = path.getPreviousNode();
                if (previousNode == null) {
                    return false;
                } else {
                    Node nextNode = path.getNextNode();
                    return pos.equals(previousNode.asBlockPos()) || pos.equals(nextNode.asBlockPos());
                }
            }
        }
    }

    private static boolean isDoorTooFarAway(ServerLevel level, LivingEntity entity, GlobalPos pos) {
        return pos.dimension() != level.dimension() || !pos.pos().closerToCenterThan(entity.position(), SKIP_CLOSING_DOOR_IF_FURTHER_AWAY_THAN);
    }

    private static Optional<Set<GlobalPos>> rememberDoorToClose(MemoryAccessor<OptionalBox.Mu, Set<GlobalPos>> doorsToClose, Optional<Set<GlobalPos>> doorPositions, ServerLevel level, BlockPos blockPos) {
        GlobalPos globalPos = GlobalPos.of(level.dimension(), blockPos);
        return Optional.of(doorPositions.map((pos) -> {
            pos.add(globalPos);
            return pos;
        }).orElseGet(() -> {
            Set<GlobalPos> posSet = Sets.newHashSet(globalPos);
            doorsToClose.set(posSet);
            return posSet;
        }));
    }

    private static void setFenceGate(@Nullable Entity entity, Level serverLevel, BlockState blockstate, BlockPos blockPos, boolean isOpen) {
        serverLevel.setBlock(blockPos, blockstate.setValue(FenceGateBlock.OPEN, isOpen), Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
        if (blockstate.getBlock() instanceof FenceGateBlockAccessor fenceGateBlock) {
            SoundEvent openSound = fenceGateBlock.tlm$getType().fenceGateOpen();
            SoundEvent closeSound = fenceGateBlock.tlm$getType().fenceGateClose();
            serverLevel.playSound(entity, blockPos, isOpen ? openSound : closeSound, SoundSource.BLOCKS, 1.0F, serverLevel.getRandom().nextFloat() * 0.1F + 0.9F);
        }
        serverLevel.gameEvent(entity, isOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, blockPos);
    }
}